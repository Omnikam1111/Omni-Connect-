package com.example.util

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.data.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsThread(
    val threadId: Long,
    val address: String,
    val snippet: String,
    val date: Long,
    val msgCount: Int
)

data class SmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int // Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX or MESSAGE_TYPE_SENT
)

data class CallLogEntry(
    val id: Long,
    val number: String,
    val cachedName: String?,
    val type: Int, // CallLog.Calls.INCOMING_TYPE, etc.
    val date: Long,
    val duration: Long
)

object SmsAndCallLogger {
    private const val TAG = "SmsAndCallLogger"

    fun fetchCallLogs(context: Context): List<CallLogEntry> {
        val list = mutableListOf<CallLogEntry>()
        val uri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri, projection, null, null, "${CallLog.Calls.DATE} DESC LIMIT 100"
            )
            if (cursor != null && cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

                do {
                    list.add(
                        CallLogEntry(
                            id = cursor.getLong(idIdx),
                            number = cursor.getString(numIdx) ?: "Unknown",
                            cachedName = if (nameIdx != -1) cursor.getString(nameIdx) else null,
                            type = cursor.getInt(typeIdx),
                            date = cursor.getLong(dateIdx),
                            duration = cursor.getLong(durIdx)
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying call logs", e)
        } finally {
            cursor?.close()
        }
        return list
    }

    fun makePhoneCall(context: Context, phoneNumber: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: SecurityException) {
            // Fallback to ACTION_DIAL if CALL_PHONE permission or default state isn't active
            val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
        }
    }

    fun fetchSmsThreads(context: Context): List<SmsThread> {
        val list = mutableListOf<SmsThread>()
        val uri = Uri.parse("content://sms")
        val projection = arrayOf("thread_id", "address", "body", "date")
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri, projection, null, null, "date DESC LIMIT 1000"
            )
            if (cursor != null && cursor.moveToFirst()) {
                val threadIdIdx = cursor.getColumnIndex("thread_id")
                val addressIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val dateIdx = cursor.getColumnIndex("date")

                val threadsMap = mutableMapOf<Long, SmsThread>()
                val countsMap = mutableMapOf<Long, Int>()

                do {
                    val threadId = cursor.getLong(threadIdIdx)
                    val address = cursor.getString(addressIdx) ?: "Unknown"
                    val snippet = cursor.getString(bodyIdx) ?: ""
                    val date = cursor.getLong(dateIdx)

                    countsMap[threadId] = (countsMap[threadId] ?: 0) + 1

                    if (!threadsMap.containsKey(threadId)) {
                        threadsMap[threadId] = SmsThread(
                            threadId = threadId,
                            address = address,
                            snippet = snippet,
                            date = date,
                            msgCount = 0
                        )
                    }
                } while (cursor.moveToNext())

                list.addAll(threadsMap.values.map { thread ->
                    thread.copy(msgCount = countsMap[thread.threadId] ?: 1)
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SMS threads", e)
        } finally {
            cursor?.close()
        }
        return list
    }

    suspend fun loadContactNamesMap(context: Context): Map<String, String> = withContext(Dispatchers.IO) {
        val phoneToName = mutableMapOf<String, String>()
        
        // 1. Fetch from local database (highest priority)
        try {
            val repo = DatabaseProvider.getRepository(context)
            val localContacts = repo.getAllContactsList()
            for (c in localContacts) {
                val name = "${c.contact.firstName} ${c.contact.lastName}".trim()
                if (name.isNotEmpty()) {
                    for (phone in c.phoneNumbers) {
                        val cleanPhone = phone.number.filter { it.isDigit() }
                        if (cleanPhone.length >= 7) {
                            phoneToName[cleanPhone.takeLast(10)] = name
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading local contact list", e)
        }

        // 2. Fetch from system contacts as fallback
        try {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                val systemContacts = PhoneContactImporter.fetchAllPhoneContacts(context)
                for (sc in systemContacts) {
                    if (sc.name.isNotEmpty()) {
                        for (phone in sc.phoneNumbers) {
                            val cleanPhone = phone.filter { it.isDigit() }
                            if (cleanPhone.length >= 7) {
                                val key = cleanPhone.takeLast(10)
                                if (!phoneToName.containsKey(key)) {
                                    phoneToName[key] = sc.name
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading system contact list", e)
        }
        
        phoneToName
    }

    fun getContactName(number: String, contactsMap: Map<String, String>): String {
        val clean = number.filter { it.isDigit() }
        if (clean.length >= 7) {
            val key = clean.takeLast(10)
            contactsMap[key]?.let { return it }
            
            val last7 = clean.takeLast(7)
            for ((k, v) in contactsMap) {
                if (k.endsWith(last7) || last7.endsWith(k.takeLast(7))) {
                    return v
                }
            }
        }
        return number
    }

    private fun fetchAddressForThread(context: Context, threadId: Long): String {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri, projection, "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()), "date DESC LIMIT 1"
            )
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0) ?: "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting address for thread $threadId", e)
        } finally {
            cursor?.close()
        }
        return "Unknown"
    }

    private fun fetchLastDateForThread(context: Context, threadId: Long): Long {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.DATE)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri, projection, "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()), "date DESC LIMIT 1"
            )
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting date for thread $threadId", e)
        } finally {
            cursor?.close()
        }
        return System.currentTimeMillis()
    }

    fun fetchMessagesForThread(context: Context, threadId: Long): List<SmsMessage> {
        val list = mutableListOf<SmsMessage>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri, projection, "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()), "date ASC"
            )
            if (cursor != null && cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(Telephony.Sms._ID)
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                val typeIdx = cursor.getColumnIndex(Telephony.Sms.TYPE)

                do {
                    list.add(
                        SmsMessage(
                            id = cursor.getLong(idIdx),
                            address = cursor.getString(addressIdx) ?: "Unknown",
                            body = cursor.getString(bodyIdx) ?: "",
                            date = cursor.getLong(dateIdx),
                            type = cursor.getInt(typeIdx)
                        )
                    )
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SMS messages", e)
        } finally {
            cursor?.close()
        }
        return list
    }

    fun sendSms(context: Context, recipient: String, messageText: String, onComplete: (Boolean) -> Unit) {
        try {
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(recipient, null, messageText, null, null)

            // Save sent SMS to system outbox so it also appears in our app threads
            val values = ContentValues().apply {
                put(Telephony.Sms.Outbox.ADDRESS, recipient)
                put(Telephony.Sms.Outbox.BODY, messageText)
                put(Telephony.Sms.Outbox.DATE, System.currentTimeMillis())
                put(Telephony.Sms.Outbox.READ, 1)
            }
            context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)

            onComplete(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
            onComplete(false)
        }
    }
}
