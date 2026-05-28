package com.example.util

import android.telecom.Call
import android.telecom.InCallService
import android.content.Intent

class SimpleInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        ActiveCallState.setCall(call)
        
        // Launch main activity to display ongoing call UI
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        if (intent != null) {
            startActivity(intent)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        ActiveCallState.setCall(null)
    }
}
