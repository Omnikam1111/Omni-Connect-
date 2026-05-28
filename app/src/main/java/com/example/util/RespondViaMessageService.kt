package com.example.util

import android.app.IntentService
import android.content.Intent
import android.util.Log

class RespondViaMessageService : IntentService("RespondViaMessageService") {
    @Deprecated("Deprecated in Java", ReplaceWith("Log.d"))
    override fun onHandleIntent(intent: Intent?) {
        Log.d("RespondViaMessageService", "Handling quick reply SMS intent: ${intent?.action}")
    }
}
