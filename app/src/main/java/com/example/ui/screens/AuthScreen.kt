package com.example.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.ContactListViewModel

class LeakSafeCallback(
    private var actualCallback: ((Boolean) -> Unit)?
) {
    fun invoke(success: Boolean) {
        actualCallback?.invoke(success)
        clear()
    }

    fun invokeTemporary(success: Boolean) {
        actualCallback?.invoke(success)
    }

    fun clear() {
        actualCallback = null
    }
}

@Composable
fun AuthScreen(
    viewModel: ContactListViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var enteredPin by remember { mutableStateOf("") }
    val errorMsg = (authState as? AuthState.Error)?.message

    val activeCallbackRef = remember { mutableStateOf<LeakSafeCallback?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            activeCallbackRef.value?.clear()
            activeCallbackRef.value = null
        }
    }

    val triggerPrompt = remember {
        {
            activeCallbackRef.value?.clear()
            val safeCallback = LeakSafeCallback { success ->
                if (success) {
                    Toast.makeText(appContext, "Biometric authentication successful", Toast.LENGTH_SHORT).show()
                    viewModel.verifyPin(viewModel.preferences.realPin) // Automatically log in to real DB
                } else {
                    Toast.makeText(appContext, "Biometric authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
            activeCallbackRef.value = safeCallback
            triggerBiometricPrompt(context, safeCallback)
        }
    }

    // Automatically trigger biometrics if enabled (requirement 2.7)
    LaunchedEffect(Unit) {
        if (viewModel.preferences.isBiometricEnabled) {
            triggerPrompt()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1B24), Color(0xFF0F0E13))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Keypad Icon & Title
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Secure Vault Lock",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Enter Real PIN to load secure vault or Decoy PIN to load plausible decoy environment",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Bullet Identifiers
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { idx ->
                    val isFilled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary else Color(0xFFFFFFFF).copy(alpha = 0.2f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isFilled) MaterialTheme.colorScheme.primary else Color(0xFFFFFFFF).copy(alpha = 0.4f),
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error displays
            AnimatedVisibility(
                visible = errorMsg != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                errorMsg?.let {
                    Text(
                        text = it,
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 3x4 Grid Numeric Panel
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val digits = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                for (row in digits) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        for (key in row) {
                            if (key == "BIO") {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .clickable {
                                            triggerPrompt()
                                        }
                                        .testTag("bio_trigger_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Biometric Button",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (key == "DEL") {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                viewModel.forceResetAuthError()
                                            }
                                        }
                                        .testTag("del_pin_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace Button",
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .clickable {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                viewModel.forceResetAuthError()
                                                if (enteredPin.length == 4) {
                                                    val verified = viewModel.verifyPin(enteredPin)
                                                    if (!verified) {
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("pin_digit_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun findFragmentActivity(context: android.content.Context): FragmentActivity? {
    var ctx = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is FragmentActivity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}

private fun triggerBiometricPrompt(context: android.content.Context, safeCallback: LeakSafeCallback) {
    Log.d("AuthScreen", "triggerBiometricPrompt called")
    val activity = findFragmentActivity(context)
    if (activity == null) {
        Log.e("AuthScreen", "Context must be FragmentActivity for BiometricPrompt")
        safeCallback.invoke(false)
        return
    }

    try {
        val biometricManager = androidx.biometric.BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or 
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        Log.d("AuthScreen", "canAuthenticate result: $canAuth")
        if (canAuth != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            Log.w("AuthScreen", "Biometric authentication not available (code $canAuth)")
            safeCallback.invoke(false)
            return
        }
    } catch (t: Throwable) {
        Log.e("AuthScreen", "BiometricManager check exception", t)
        safeCallback.invoke(false)
        return
    }

    try {
        Log.d("AuthScreen", "Attempting to create BiometricPrompt")
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("AuthScreen", "Biometric authentication succeeded")
                    safeCallback.invoke(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d("AuthScreen", "Biometric authentication error: $errorCode, $errString")
                    safeCallback.invoke(false)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d("AuthScreen", "Biometric authentication failed")
                    safeCallback.invokeTemporary(false)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Verification Required")
            .setSubtitle("Confirm biological shield to unlock encrypted CRM database")
            .setNegativeButtonText("Use Passcode Instead")
            .build()

        biometricPrompt.authenticate(promptInfo)
    } catch (e: Throwable) {
        Log.e("AuthScreen", "Biometric authentication exception during initialization/prompt", e)
        safeCallback.invoke(false)
    }
}
