package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    private const val TAG = "ImageUtils"

    // High performance in-memory caching mapping content Uri string -> local copied file path
    // This dramatically eliminates duplicate disk writes/copies during scrolling or list recompositions
    private val uriCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun saveUriToLocalFile(context: Context, uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        
        // Return from cache instantly if resolved previously
        uriCache[uriString]?.let { cachedPath ->
            if (File(cachedPath).exists()) {
                return cachedPath
            }
        }

        // If it starts with "/" and is in our filesDir or app directory path, it's already a safe local file.
        val packageName = context.packageName
        if (uriString.startsWith("/") && uriString.contains(packageName)) {
            val file = File(uriString)
            if (file.exists()) {
                uriCache[uriString] = uriString
                return uriString
            }
        }

        try {
            val uri = Uri.parse(uriString)
            val scheme = uri.scheme
            
            // Handle standard file scheme
            if (scheme == "file") {
                val path = uri.path
                if (path != null && File(path).exists()) {
                    uriCache[uriString] = path
                    return path
                }
            }

            // Otherwise, we open an input stream from the ContentResolver
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val photosDir = File(context.filesDir, "contact_photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            
            // Create a unique filename based on time and a random number to avoid collisions
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("gif") -> "gif"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val fileName = "profile_${System.currentTimeMillis()}_${(1000..9999).random()}.$extension"
            val file = File(photosDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                inputStream.use { stream ->
                    stream.copyTo(outputStream)
                }
            }
            
            Log.d(TAG, "Successfully copied $uriString to local file: ${file.absolutePath}")
            val localPath = file.absolutePath
            uriCache[uriString] = localPath
            return localPath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Uri $uriString to local file", e)
            return null
        }
    }

    @Composable
    fun rememberProfilePhotoState(uriString: String?): Any? {
        val context = LocalContext.current
        return produceState<Any?>(initialValue = null, uriString) {
            if (uriString.isNullOrBlank()) {
                value = null
                return@produceState
            }
            
            val isContentUri = uriString.startsWith("content://") || uriString.startsWith("android.resource://")
            if (!isContentUri) {
                // Already a local file path, or other scheme (like http/res id string)
                value = uriString
                return@produceState
            }
            
            // It is a content:// URI. Let's copy it locally in a background thread safely
            withContext(Dispatchers.IO) {
                try {
                    val localPath = saveUriToLocalFile(context, uriString)
                    value = localPath ?: uriString // fallback to original if copy fails
                } catch (e: Exception) {
                    value = uriString // fallback to original
                }
            }
        }.value
    }
}
