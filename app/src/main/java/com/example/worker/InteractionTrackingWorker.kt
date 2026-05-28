package com.example.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.data.DatabaseProvider
import com.example.data.entity.Note
import com.example.data.entity.PhoneNumberMatch
import com.example.data.repository.ContactRepository
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class InteractionTrackingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "InteractionTrackingWork"
        private const val WORK_NAME = "com.example.worker.InteractionTrackingWorker"

        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()

                val request = PeriodicWorkRequestBuilder<InteractionTrackingWorker>(
                    15, TimeUnit.MINUTES
                )
                .setConstraints(constraints)
                .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Log.d(TAG, "Interaction tracking worker scheduled successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule interaction tracking worker", e)
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting interaction tracking worker run.")
            val repository = DatabaseProvider.getRepository(applicationContext)

            // 1. Fetch contacts & phone numbers from DB
            val contactsWithDetails = repository.getAllContactsList()
            if (contactsWithDetails.isEmpty()) {
                Log.d(TAG, "No contacts found to match interactions.")
                return Result.success()
            }

            // Create phone helper map: Normalized Number -> Contact ID
            val numberToContactIdMap = mutableMapOf<String, Long>()
            for (detail in contactsWithDetails) {
                for (phone in detail.phoneNumbers) {
                    val norm = normalize(phone.number)
                    if (norm.isNotEmpty()) {
                        numberToContactIdMap[norm] = detail.contact.id
                    }
                }
            }

            // Get last scanned IDs
            val lastCallId = repository.getLastScannedId("call_log") ?: 0L
            val lastSmsId = repository.getLastScannedId("sms") ?: 0L

            // Scans
            var maxCallId = lastCallId
            var maxSmsId = lastSmsId

            val hasCallLogPermission = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED

            val hasSmsPermission = ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED

            // 2. Scan Call Log
            if (hasCallLogPermission) {
                try {
                    val callUri = CallLog.Calls.CONTENT_URI
                    val projection = arrayOf(
                        CallLog.Calls._ID,
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.DATE,
                        CallLog.Calls.TYPE
                    )
                    val selection = "${CallLog.Calls._ID} > ?"
                    val selectionArgs = arrayOf(lastCallId.toString())
                    val sortOrder = "${CallLog.Calls._ID} ASC"

                    val cursor = applicationContext.contentResolver.query(
                        callUri, projection, selection, selectionArgs, sortOrder
                    )
                    cursor?.use { c ->
                        val idCol = c.getColumnIndexOrThrow(CallLog.Calls._ID)
                        val numCol = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                        val dateCol = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
                        val typeCol = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)

                        while (c.moveToNext()) {
                            val id = c.getLong(idCol)
                            val num = c.getString(numCol) ?: ""
                            val date = c.getLong(dateCol)
                            val type = c.getInt(typeCol)

                            if (id > maxCallId) maxCallId = id

                            val normNum = normalize(num)
                            val matchedContactId = matchNumber(normNum, numberToContactIdMap)
                            if (matchedContactId != null) {
                                val contact = contactsWithDetails.firstOrNull { it.contact.id == matchedContactId }?.contact
                                val typeStr = when (type) {
                                    CallLog.Calls.INCOMING_TYPE -> "Incoming call"
                                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing call"
                                    CallLog.Calls.MISSED_TYPE -> "Missed call"
                                    else -> "Call"
                                }
                                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(date))
                                val noteContent = "$typeStr logged at $dateStr with ${contact?.firstName ?: "Contact"}"

                                // Insert note
                                repository.insertNote(
                                    Note(
                                        contactId = matchedContactId,
                                        content = noteContent,
                                        createdAtMillis = date,
                                        isInteraction = true
                                    )
                                )

                                // Update contact last interaction
                                if (contact != null) {
                                    val currentLast = contact.lastInteractionInMillis ?: 0L
                                    if (date > currentLast) {
                                        repository.updateContact(
                                            contact.copy(lastInteractionInMillis = date)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error matching call logs", e)
                }
            }

            // 3. Scan SMS
            if (hasSmsPermission) {
                try {
                    val smsUri = Uri.parse("content://sms")
                    val projection = arrayOf(
                        "_id",
                        "address",
                        "date",
                        "type"
                    )
                    val selection = "_id > ?"
                    val selectionArgs = arrayOf(lastSmsId.toString())
                    val sortOrder = "_id ASC"

                    val cursor = applicationContext.contentResolver.query(
                        smsUri, projection, selection, selectionArgs, sortOrder
                    )
                    cursor?.use { c ->
                        val idCol = c.getColumnIndexOrThrow("_id")
                        val addrCol = c.getColumnIndexOrThrow("address")
                        val dateCol = c.getColumnIndexOrThrow("date")
                        val typeCol = c.getColumnIndexOrThrow("type")

                        while (c.moveToNext()) {
                            val id = c.getLong(idCol)
                            val address = c.getString(addrCol) ?: ""
                            val date = c.getLong(dateCol)
                            val type = c.getInt(typeCol)

                            if (id > maxSmsId) maxSmsId = id

                            val normNum = normalize(address)
                            val matchedContactId = matchNumber(normNum, numberToContactIdMap)
                            if (matchedContactId != null) {
                                val contact = contactsWithDetails.firstOrNull { it.contact.id == matchedContactId }?.contact
                                val typeStr = when (type) {
                                    1 -> "SMS received"
                                    2 -> "SMS sent"
                                    else -> "SMS interaction"
                                }
                                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(date))
                                val noteContent = "$typeStr with ${contact?.firstName ?: "Contact"} on $dateStr"

                                // Insert note
                                repository.insertNote(
                                    Note(
                                        contactId = matchedContactId,
                                        content = noteContent,
                                        createdAtMillis = date,
                                        isInteraction = true
                                    )
                                )

                                // Update contact last interaction
                                if (contact != null) {
                                    val currentLast = contact.lastInteractionInMillis ?: 0L
                                    if (date > currentLast) {
                                        repository.updateContact(
                                            contact.copy(lastInteractionInMillis = date)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error matching SMS logs", e)
                }
            }

            // 4. Save checked offsets
            if (maxCallId > lastCallId) {
                repository.insertLastScanned(PhoneNumberMatch("call_log", maxCallId))
            }
            if (maxSmsId > lastSmsId) {
                repository.insertLastScanned(PhoneNumberMatch("sms", maxSmsId))
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "InteractionTrackingWorker encountered unhandled database exception", e)
            Result.retry()
        }
    }

    private fun normalize(number: String): String {
        // Retain only digits. If country code is attached, we'll strip or compare suffixes.
        return number.filter { it.isDigit() }
    }

    private fun matchNumber(testNumber: String, map: Map<String, Long>): Long? {
        if (testNumber.isEmpty()) return null
        // Standard suffix matching to accommodate country prefixes (match last 7-10 digits)
        for ((storedNum, contactId) in map) {
            if (testNumber == storedNum) return contactId
            if (testNumber.length >= 7 && storedNum.endsWith(testNumber)) return contactId
            if (storedNum.length >= 7 && testNumber.endsWith(storedNum)) return contactId
        }
        return null
    }
}
