package com.example.util

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import com.example.MainActivity

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (message in messages) {
                    val body = message.messageBody
                    val sender = message.displayOriginatingAddress ?: "Unknown"
                    val timestamp = message.timestampMillis

                    Log.d("SmsReceiver", "Received SMS from $sender: $body")

                    // Save incoming SMS to the system provider so it appears in threads
                    val values = ContentValues().apply {
                        put(Telephony.Sms.Inbox.ADDRESS, sender)
                        put(Telephony.Sms.Inbox.BODY, body)
                        put(Telephony.Sms.Inbox.DATE, timestamp)
                        put(Telephony.Sms.Inbox.READ, 0)
                    }
                    context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)

                    showNotification(context, sender, body)
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Failed to parse incoming SMS", e)
            }
        }
    }

    private fun showNotification(context: Context, sender: String, body: String) {
        val channelId = "sms_inbound_channel"
        val notificationId = 1001

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Inbound SMS Notifications", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("New message from $sender")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        manager.notify(notificationId, builder.build())
    }
}
