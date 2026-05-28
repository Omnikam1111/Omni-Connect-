package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.data.SettingsPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object GoogleOAuthManager {
    private const val TAG = "GoogleOAuthManager"
    private val client = OkHttpClient()

    fun initiateOAuthFlow(context: Context): Boolean {
        val prefs = SettingsPreferences(context)
        val clientId = prefs.googleOAuthClientId.trim()

        if (clientId.isBlank()) {
            return false
        }

        val authUrl = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", "com.aistudio.oauth://callback")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile")
            .appendQueryParameter("prompt", "consent")
            .appendQueryParameter("access_type", "offline")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, authUrl).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    fun exchangeCodeForTokens(
        context: Context,
        code: String,
        callback: (Boolean, String) -> Unit
    ) {
        val prefs = SettingsPreferences(context)
        val clientId = prefs.googleOAuthClientId.trim()
        val clientSecret = prefs.googleOAuthClientSecret.trim()

        if (clientId.isBlank() || clientSecret.isBlank()) {
            callback(false, "Client ID or Secret is missing. Please configure them in Settings before signing in.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val formBody = FormBody.Builder()
                    .add("code", code)
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .add("redirect_uri", "com.aistudio.oauth://callback")
                    .add("grant_type", "authorization_code")
                    .build()

                val request = Request.Builder()
                    .url("https://oauth2.googleapis.com/token")
                    .post(formBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errMsg = response.body?.string() ?: ""
                        Log.e(TAG, "Token exchange failed: $errMsg")
                        val errorDesc = try {
                            JSONObject(errMsg).optString("error_description", "Unknown error")
                        } catch (e: Exception) {
                            "Status ${response.code}"
                        }
                        withContext(Dispatchers.Main) {
                            callback(false, "Failed to exchange authorization code: $errorDesc")
                        }
                        return@launch
                    }

                    val json = JSONObject(response.body?.string() ?: "{}")
                    val accessToken = json.getString("access_token")
                    val refreshToken = json.optString("refresh_token", "") // standard Web App offline flow
                    val expiresIn = json.getLong("expires_in")
                    
                    // Save tokens
                    prefs.googleAccessToken = accessToken
                    if (refreshToken.isNotBlank()) {
                        prefs.googleRefreshToken = refreshToken
                    }
                    prefs.googleTokenExpiry = System.currentTimeMillis() + (expiresIn * 1000)

                    // Fetch user email & name
                    val userInfoRequest = Request.Builder()
                        .url("https://www.googleapis.com/oauth2/v3/userinfo")
                        .addHeader("Authorization", "Bearer $accessToken")
                        .build()

                    client.newCall(userInfoRequest).execute().use { userResponse ->
                        if (userResponse.isSuccessful) {
                            val userJson = JSONObject(userResponse.body?.string() ?: "{}")
                            val email = userJson.optString("email", "")
                            val name = userJson.optString("name", "Connected User")

                            prefs.googleAccountEmail = email
                            prefs.googleAccountName = name

                            withContext(Dispatchers.Main) {
                                callback(true, "Successfully connected Gmail for $email!")
                            }
                        } else {
                            prefs.googleAccountEmail = "gmail_api_user"
                            prefs.googleAccountName = "Connected Gmail Account"
                            withContext(Dispatchers.Main) {
                                callback(true, "Connected successfully, but folder info is missing.")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in code exchange", e)
                withContext(Dispatchers.Main) {
                    callback(false, "Connection failed: ${e.localizedMessage ?: "Unknown network error."}")
                }
            }
        }
    }

    suspend fun getOrRefreshAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = SettingsPreferences(context)
        val accessToken = prefs.googleAccessToken
        val expiry = prefs.googleTokenExpiry
        val refreshToken = prefs.googleRefreshToken
        val clientId = prefs.googleOAuthClientId.trim()
        val clientSecret = prefs.googleOAuthClientSecret.trim()

        // 1. If we have a cached valid access token, use it
        // Check if expiry is in more than 5 minutes (safety threshold)
        if (accessToken.isNotBlank() && expiry > (System.currentTimeMillis() + 300000)) {
            Log.d(TAG, "Cached access token is still valid. Returning cached token.")
            return@withContext accessToken
        }

        // 2. If it is expired, refresh it using refresh_token
        if (refreshToken.isBlank()) {
            Log.e(TAG, "Cannot refresh token: Refresh token is missing")
            return@withContext null
        }
        if (clientId.isBlank() || clientSecret.isBlank()) {
            Log.e(TAG, "Cannot refresh token: Client ID or Secret is missing")
            return@withContext null
        }

        try {
            Log.d(TAG, "Refreshing access token from Google...")
            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errMsg = response.body?.string() ?: ""
                    Log.e(TAG, "Failed to refresh token: $errMsg")
                    return@withContext null
                }

                val json = JSONObject(response.body?.string() ?: "{}")
                val newAccessToken = json.getString("access_token")
                val expiresIn = json.getLong("expires_in")

                prefs.googleAccessToken = newAccessToken
                prefs.googleTokenExpiry = System.currentTimeMillis() + (expiresIn * 1000)
                Log.d(TAG, "Successfully refreshed access token from Google.")
                return@withContext newAccessToken
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error while refreshing access token", e)
            return@withContext null
        }
    }
}
