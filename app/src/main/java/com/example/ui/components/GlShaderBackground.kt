package com.example.ui.components

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.ShaderPresets
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun GlShaderBackground(
    shaderCode: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val renderer = remember {
        ShaderBackgroundRenderer()
    }

    // Reactively update the compiled shader on the GL Thread
    LaunchedEffect(shaderCode) {
        val normalizedCode = normalizeShaderSource(shaderCode)
        renderer.updateShader(normalizedCode)
    }

    val glSurfaceView = remember {
        GLSurfaceView(context).apply {
            // Request OpenGL ES 2.0 context
            setEGLContextClientVersion(2)
            // MUST set renderer during creation, before attaching/onResume to avoid crashes!
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
    }

    DisposableEffect(glSurfaceView) {
        glSurfaceView.onResume()
        onDispose {
            glSurfaceView.onPause()
        }
    }

    AndroidView(
        factory = {
            glSurfaceView
        },
        modifier = modifier
            .fillMaxSize()
            .pointerInput(renderer) {
                detectDragGestures(
                    onDragStart = { offset ->
                        renderer.updateMouse(offset.x, size.height.toFloat() - offset.y)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        renderer.updateMouse(pos.x, size.height.toFloat() - pos.y)
                    }
                )
            }
            .pointerInput(renderer) {
                detectTapGestures(
                    onPress = { offset ->
                        renderer.updateMouse(offset.x, size.height.toFloat() - offset.y)
                    }
                )
            }
    )
}

private class ShaderBackgroundRenderer : GLSurfaceView.Renderer {
    private val TAG = "ShaderRenderer"

    private val vertexCoords = floatArrayOf(
        -1.0f, -1.0f,
         1.0f, -1.0f,
        -1.0f,  1.0f,
        -1.0f,  1.0f,
         1.0f, -1.0f,
         1.0f,  1.0f
    )

    private var vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertexCoords.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer().apply {
            put(vertexCoords)
            position(0)
        }

    private var programId = 0
    private var positionHandle = 0

    private var width = 1f
    private var height = 1f
    private val startTime = System.currentTimeMillis()

    @Volatile
    private var mouseX = -1f
    @Volatile
    private var mouseY = -1f

    @Volatile
    private var pendingShaderCode: String? = null

    fun updateShader(code: String) {
        pendingShaderCode = code
    }

    fun updateMouse(x: Float, y: Float) {
        mouseX = x
        mouseY = y
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.01f, 0.04f, 1.0f)
        
        // Build initial fallback / starfield shader
        val initCode = normalizeShaderSource(ShaderPresets.PRESET_STARS)
        compileAndLinkProgram(initCode)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    override fun onDrawFrame(gl: GL10?) {
        // Safe thread synchronization for dynamic shader updates
        val codeToCompile = pendingShaderCode
        if (codeToCompile != null) {
            pendingShaderCode = null
            compileAndLinkProgram(codeToCompile)
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (programId == 0) return

        GLES20.glUseProgram(programId)

        // Bind vertices
        positionHandle = GLES20.glGetAttribLocation(programId, "position")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        // Resolve default fallback center for coordinate system if not touched yet
        val mX = if (mouseX < 0f) width * 0.5f else mouseX
        val mY = if (mouseY < 0f) height * 0.5f else mouseY

        // Bind Time Uniforms: elapsedSeconds since startup (seconds)
        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f

        val timeHandle = GLES20.glGetUniformLocation(programId, "time")
        if (timeHandle != -1) {
            GLES20.glUniform1f(timeHandle, elapsedSeconds)
        }

        val iTimeHandle = GLES20.glGetUniformLocation(programId, "iTime")
        if (iTimeHandle != -1) {
            GLES20.glUniform1f(iTimeHandle, elapsedSeconds)
        }

        val iGlobalTimeHandle = GLES20.glGetUniformLocation(programId, "iGlobalTime")
        if (iGlobalTimeHandle != -1) {
            GLES20.glUniform1f(iGlobalTimeHandle, elapsedSeconds)
        }

        // Bind Resolution Uniforms
        val resolutionHandle = GLES20.glGetUniformLocation(programId, "resolution")
        if (resolutionHandle != -1) {
            GLES20.glUniform2f(resolutionHandle, width, height)
        }

        val iResolutionHandle = GLES20.glGetUniformLocation(programId, "iResolution")
        if (iResolutionHandle != -1) {
            GLES20.glUniform3f(iResolutionHandle, width, height, width / height)
        }

        // Bind Mouse Uniforms
        val mouseHandle = GLES20.glGetUniformLocation(programId, "mouse")
        if (mouseHandle != -1) {
            GLES20.glUniform2f(mouseHandle, mX / width, mY / height) // Normalised to [0.0, 1.0] as standard for Sandbox/mouse coordinates
        }

        val iMouseHandle = GLES20.glGetUniformLocation(programId, "iMouse")
        if (iMouseHandle != -1) {
            GLES20.glUniform4f(iMouseHandle, mX, mY, 0f, 0f)
        }

        // Draw quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(positionHandle)
    }

    private fun compileAndLinkProgram(fragCode: String) {
        try {
            val vertexShaderCode = """
                attribute vec4 position;
                void main() {
                    gl_Position = position;
                }
            """.trimIndent()

            val vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            if (vs == 0) return

            var fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragCode)
            if (fs == 0) {
                // Compile of custom code failed, fall back safely to stars or modern plasma preview
                Log.e(TAG, "Dynamic GLSL compilation failed. Resolving to fail-safe preset.")
                GLES20.glDeleteShader(vs)
                
                val recoveryFs = loadShader(GLES20.GL_FRAGMENT_SHADER, normalizeShaderSource(ShaderPresets.PRESET_CUSTOM_DEFAULT))
                if (recoveryFs != 0) {
                    val vsRetry = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
                    link(vsRetry, recoveryFs)
                }
                return
            }

            link(vs, fs)
        } catch (e: Exception) {
            Log.e(TAG, "Uncaught error during program compilation", e)
        }
    }

    private fun link(vs: Int, fs: Int) {
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)

        val linked = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            val errorMsg = GLES20.glGetProgramInfoLog(prog)
            Log.e(TAG, "Shader Linking Error: $errorMsg")
            GLES20.glDeleteProgram(prog)
        } else {
            // Delete old program if active
            if (programId != 0) {
                GLES20.glDeleteProgram(programId)
            }
            programId = prog
        }

        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "GL Shader compilation error [Type $type]: $error")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }
}

/**
 * Ensures standard variables are in place to make custom pasted Shadertoy/GLSL scripts fully compatible
 */
private fun normalizeShaderSource(rawCode: String): String {
    var code = rawCode.trim()

    // Prefix with standard precision if not declared
    if (!code.contains("precision")) {
        code = "precision highp float;\n" + code
    }

    // Auto prepend uniform headers if missing but references are found
    val declarations = StringBuilder()
    if (code.contains("iResolution") && !code.contains("uniform vec3 iResolution") && !code.contains("uniform vec2 iResolution")) {
        declarations.append("uniform vec3 iResolution;\n")
    }
    if (code.contains("iTime") && !code.contains("uniform float iTime")) {
        declarations.append("uniform float iTime;\n")
    }

    if (declarations.isNotEmpty()) {
        code = declarations.toString() + code
    }

    // Auto-map shadertoy mainImage style functions to direct GLES 2.0 viewport execution
    if (code.contains("mainImage") && !code.contains("void main(")) {
        code += "\nvoid main() {\n    mainImage(gl_FragColor, gl_FragCoord.xy);\n}\n"
    }

    return code
}

