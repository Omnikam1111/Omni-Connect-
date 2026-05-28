package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.data.entity.Note
import com.example.data.entity.ScheduledMessage
import com.example.data.repository.ContactRepository
import com.example.data.DatabaseProvider
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import com.google.android.gms.auth.GoogleAuthUtil
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class ScheduledMessageWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "ScheduledMessageWorker"
        private const val NOTIFICATION_CHANNEL_ID = "scheduled_messages_channel"
        private val client = OkHttpClient()

        // Helper to schedule a WorkManager request for a ScheduledMessage
        fun scheduleMessageWork(context: Context, message: ScheduledMessage) {
            val delayMillis = message.scheduleTimeMillis - System.currentTimeMillis()
            
            // If the delay is very negative, we still want to run it immediately (best-effort)
            val initialDelay = if (delayMillis > 0) delayMillis else 0L

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val inputData = workDataOf("message_id" to message.id)

            val workRequest = OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag("msg_tag_${message.id}")
                .build()

            // Enqueue uniquely per message ID so we don't duplicate schedules
            WorkManager.getInstance(context).enqueueUniqueWork(
                "msg_work_${message.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            Log.d(TAG, "Scheduled message ID ${message.id} in WorkManager with initial delay: $initialDelay ms")
        }

        fun cancelScheduledWork(context: Context, messageId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("msg_work_$messageId")
            Log.d(TAG, "Cancelled WorkManager schedule for message ID $messageId")
        }
    }

    override suspend fun doWork(): Result {
        val repository = DatabaseProvider.getRepository(applicationContext)
        val messageId = inputData.getLong("message_id", -1L)
        if (messageId == -1L) {
            Log.e(TAG, "No message ID provided to ScheduledMessageWorker")
            return Result.success()
        }

        val message = repository.getScheduledMessageById(messageId) ?: run {
            Log.e(TAG, "ScheduledMessage with ID $messageId not found in Room")
            return Result.success()
        }

        // Check if already cancelled or sent to prevent double-firing
        if (message.status != "PENDING") {
            Log.d(TAG, "ScheduledMessage $messageId is not PENDING (current status: ${message.status})")
            return Result.success()
        }

        Log.d(TAG, "Executing scheduled delivery of ID $messageId to ${message.recipientName}")

        var success = false
        var errorMsg: String? = null

        try {
            if (message.type.run { equals("SMS", ignoreCase = true) }) {
                // Real programmatic SMS logic using SmsManager
                val hasSmsPermission = ContextCompat.checkSelfPermission(
                    applicationContext, android.Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasSmsPermission) {
                    val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        applicationContext.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }
                    
                    // Send actual SMS
                    smsManager.sendTextMessage(
                        message.recipientValue,
                        null,
                        message.messageContent,
                        null,
                        null
                    )
                    success = true
                    Log.d(TAG, "Successfully invoked SmsManager to send to: ${message.recipientValue}")
                } else {
                    errorMsg = "SEND_SMS permission not granted."
                    Log.e(TAG, "Cannot send SMS: SEND_SMS permission is missing")
                }
            } else {
                val preferences = com.example.data.SettingsPreferences(applicationContext)
                val googleEmail = preferences.googleAccountEmail
                val hasRefreshToken = preferences.googleRefreshToken.isNotBlank()

                if (googleEmail.isNotBlank() && hasRefreshToken) {
                    Log.d(TAG, "Sending email via Gmail API for connected Google Account: $googleEmail")
                    val accessToken = try {
                        com.example.util.GoogleOAuthManager.getOrRefreshAccessToken(applicationContext)
                    } catch (e: Exception) {
                        Log.e(TAG, "Google Access Token acquisition failed", e)
                        null
                    }

                    if (accessToken.isNullOrBlank()) {
                        throw Exception("Google account needs re-authentication: Accquired Google Access Token was empty. Please reconnect your account in Settings.")
                    }

                    // Build MIME email message
                    val props = Properties()
                    val session = Session.getInstance(props)
                    val mimeMessage = MimeMessage(session).apply {
                        setFrom(InternetAddress(googleEmail))
                        setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.recipientValue))
                        setSubject("Message from Nexus CRM for ${message.recipientName}")
                        setText(message.messageContent)
                    }

                    val outputStream = ByteArrayOutputStream()
                    mimeMessage.writeTo(outputStream)
                    val rawBytes = outputStream.toByteArray()
                    val base64UrlSafe = android.util.Base64.encodeToString(rawBytes, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)

                    // Post via OkHttp to Gmail API messages/send
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val jsonBody = JSONObject().put("raw", base64UrlSafe).toString()
                    val body = jsonBody.toRequestBody(mediaType)

                    val req = Request.Builder()
                        .url("https://gmail.googleapis.com/gmail/v1/users/me/messages/send")
                        .addHeader("Authorization", "Bearer $accessToken")
                        .post(body)
                        .build()

                    client.newCall(req).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errBody = response.body?.string() ?: ""
                            throw Exception("Gmail API returned error [HTTP ${response.code}]: $errBody")
                        }
                    }
                    success = true
                    Log.d(TAG, "Successfully sent email via Gmail API to: ${message.recipientValue}")
                } else {
                    // Fallback to SMTP Outgoing Mail Gateway
                    val host = preferences.smtpHost
                    val port = preferences.smtpPort
                    val username = preferences.smtpUsername
                    val password = preferences.smtpPassword
                    val sender = preferences.smtpSender.ifBlank { username }
                    val useSsl = preferences.smtpUseSsl
                    val useTls = preferences.smtpUseTls

                    if (username.isBlank() || password.isBlank()) {
                        errorMsg = "Email SMTP not configured. Please open Settings -> Email SMTP Settings and add your SMTP credentials (username/password) or connect your Google Mail (Gmail) account to send live scheduled emails."
                        Log.e(TAG, "Cannot send Email: SMTP configuration is missing")
                    } else {
                        Log.d(TAG, "Connecting to SMTP server fallback at $host:$port...")
                        val props = Properties().apply {
                            put("mail.smtp.host", host)
                            put("mail.smtp.port", port)
                            put("mail.smtp.auth", "true")
                            
                            if (useSsl) {
                                put("mail.smtp.socketFactory.port", port)
                                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                                put("mail.smtp.socketFactory.fallback", "false")
                            }
                            if (useTls) {
                                put("mail.smtp.starttls.enable", "true")
                            }
                        }

                        val session = Session.getInstance(props, object : Authenticator() {
                            override fun getPasswordAuthentication(): PasswordAuthentication {
                                return PasswordAuthentication(username, password)
                            }
                        })

                        val mimeMessage = MimeMessage(session).apply {
                            setFrom(InternetAddress(sender))
                            setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.recipientValue))
                            setSubject("Message from Nexus CRM for ${message.recipientName}")
                            setText(message.messageContent)
                        }

                        Transport.send(mimeMessage)
                        success = true
                        Log.d(TAG, "Successfully sent email to: ${message.recipientValue} via SMTP!")
                    }
                }
            }
        } catch (e: Exception) {
            errorMsg = e.message ?: "Unknown execution error"
            Log.e(TAG, "Execution failed for message ID $messageId", e)
        }

        if (success) {
            val isRecurring = when (message.recurrence.uppercase()) {
                "ONE_HOUR", "DAILY", "WEEKLY", "MONTHLY", "BIRTHDAY", "FOLLOW_UP" -> true
                else -> false
            }

            if (isRecurring) {
                // Update status to SENT
                val updatedMsg = message.copy(
                    status = "SENT",
                    lastAttempt = System.currentTimeMillis(),
                    errorMessage = null
                )
                repository.updateScheduledMessage(updatedMsg)
            } else {
                // Delete it as requested, so it's removed from the Scheduled list since it's successfully sent
                repository.deleteScheduledMessageById(message.id)
            }

            // Dynamic interaction tracking: insert note for this contact as an action log
            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
            val typeName = if (message.type.run { equals("SMS", ignoreCase = true) }) "Scheduled SMS" else "Scheduled Email"
            val logContent = "Auto-sent $typeName to ${message.recipientName} (${message.recipientValue}): \"${message.messageContent}\" at $dateStr"
            
            repository.insertNote(
                Note(
                    contactId = message.contactId,
                    content = logContent,
                    createdAtMillis = System.currentTimeMillis(),
                    isInteraction = true
                )
            )

            // Update contact last interaction
            val contactDetail = repository.getContactByIdSuspend(message.contactId)
            contactDetail?.let {
                repository.updateContact(it.contact.copy(lastInteractionInMillis = System.currentTimeMillis()))
            }

            // Create system status notification
            showNotification(
                "Message Sent",
                "$typeName sent to ${message.recipientName}: ${message.messageContent}"
            )

            // Compute recurrences if required!
            if (message.recurrence != "ONCE") {
                scheduleNextRecurrence(repository, message)
            }
        } else {
            // Update status to FAILED
            val updatedMsg = message.copy(
                status = "FAILED",
                lastAttempt = System.currentTimeMillis(),
                errorMessage = errorMsg
            )
            repository.updateScheduledMessage(updatedMsg)

            showNotification(
                "Message Delivery Failed",
                "Scheduled ${message.type} to ${message.recipientName} failed: ${errorMsg ?: "Unknown error"}"
            )
        }

        return Result.success()
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Scheduled Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Status notifications for scheduled message deliveries"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((1000..9999).random(), notification)
    }

    private suspend fun scheduleNextRecurrence(repository: ContactRepository, prevMessage: ScheduledMessage) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = prevMessage.scheduleTimeMillis
        }

        when (prevMessage.recurrence.uppercase()) {
            "ONE_HOUR" -> calendar.add(Calendar.HOUR_OF_DAY, 1)
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "BIRTHDAY" -> calendar.add(Calendar.YEAR, 1)
            "FOLLOW_UP" -> calendar.add(Calendar.DAY_OF_YEAR, 3)
            else -> return
        }

        val nextTime = calendar.timeInMillis
        // Only schedule if the next occurrence is in the future
        if (nextTime > System.currentTimeMillis()) {
            val nextMsg = ScheduledMessage(
                contactId = prevMessage.contactId,
                recipientName = prevMessage.recipientName,
                type = prevMessage.type,
                recipientValue = prevMessage.recipientValue,
                messageContent = prevMessage.messageContent,
                scheduleTimeMillis = nextTime,
                recurrence = prevMessage.recurrence,
                status = "PENDING"
            )
            val newId = repository.insertScheduledMessage(nextMsg)
            scheduleMessageWork(applicationContext, nextMsg.copy(id = newId))
        }
    }
}
