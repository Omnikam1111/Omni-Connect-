package com.example.util

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveCallState {
    private val _activeCall = MutableStateFlow<Call?>(null)
    val activeCall = _activeCall.asStateFlow()

    private val _callState = MutableStateFlow<Int>(Call.STATE_DISCONNECTED)
    val callState = _callState.asStateFlow()

    private val _callerNumber = MutableStateFlow<String>("")
    val callerNumber = _callerNumber.asStateFlow()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            _callState.value = state
            if (state == Call.STATE_DISCONNECTED) {
                _activeCall.value = null
            }
        }
    }

    fun setCall(call: Call?) {
        _activeCall.value?.unregisterCallback(callCallback)
        _activeCall.value = call
        if (call != null) {
            _callState.value = call.state
            _callerNumber.value = call.details?.handle?.schemeSpecificPart ?: "Unknown"
            call.registerCallback(callCallback)
        } else {
            _callState.value = Call.STATE_DISCONNECTED
            _callerNumber.value = ""
        }
    }

    fun answer() {
        _activeCall.value?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
    }

    fun hangup() {
        _activeCall.value?.disconnect()
        setCall(null)
    }
}
