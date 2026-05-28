package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import com.example.data.entity.Contact

object IntentExecutor {
    private const val TAG = "IntentExecutor"

    fun executeAction(context: Context, actionType: String, targetData: String) {
        val intent = try {
            when (actionType.uppercase()) {
                "PHONE_CALL" -> {
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(targetData)}"))
                }
                "SMS" -> {
                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(targetData)}"))
                }
                "WHATSAPP" -> {
                    val cleanNumber = targetData.filter { it.isDigit() }
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber"))
                }
                "TELEGRAM" -> {
                    val handle = targetData.trim().removePrefix("@")
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$handle"))
                }
                "SIGNAL" -> {
                    val cleanNumber = targetData.filter { it.isDigit() || it == '+' }
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://signal.me/#p/$cleanNumber"))
                }
                "URL" -> {
                    val formattedUrl = if (!targetData.startsWith("http://") && !targetData.startsWith("https://")) {
                        "https://$targetData"
                    } else {
                        targetData
                    }
                    Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                }
                "APP_PACKAGE" -> {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(targetData)
                    launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$targetData"))
                }
                "MAP_COORDINATES" -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(targetData)}"))
                }
                "EMAIL" -> {
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(targetData)}"))
                }
                else -> {
                    Log.w(TAG, "Unknown actionType: $actionType")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building intent", e)
            Toast.makeText(context, "Invalid action target data format", Toast.LENGTH_SHORT).show()
            null
        }

        if (intent != null) {
            try {
                // Ensure task flags inside dynamic execution context
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start intent activity", e)
                Toast.makeText(context, "No app available to handle this action type", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openDirections(context: Context, formattedAddress: String) {
        val gmapsUri = Uri.parse("google.navigation:q=${Uri.encode(formattedAddress)}")
        val intent = Intent(Intent.ACTION_VIEW, gmapsUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web maps or browser
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(formattedAddress)}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Unable to launch map utility", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addCalendarBirthdayReminder(context: Context, contact: Contact) {
        val birthday = contact.birthdayInMillis ?: return
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Birthday: ${contact.firstName} ${contact.lastName}")
            putExtra(CalendarContract.Events.DESCRIPTION, "Remind ${contact.nickname ?: contact.firstName}'s birthday - logged in Nexus CRM.")
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, birthday)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, birthday)
            putExtra(CalendarContract.Events.RRULE, "FREQ=YEARLY")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            Toast.makeText(context, "Opening calendar picker...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Calendar application not found", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareProfileLink(context: Context, contact: Contact, phone: String, email: String) {
        val dataString = buildString {
            append("https://nexus-crm.com/share?")
            append("first=${Uri.encode(contact.firstName)}")
            append("&last=${Uri.encode(contact.lastName)}")
            if (contact.nickname != null) {
                append("&nick=${Uri.encode(contact.nickname)}")
            }
            if (phone.isNotEmpty()) {
                append("&tel=${Uri.encode(phone)}")
            }
            if (email.isNotEmpty()) {
                append("&email=${Uri.encode(email)}")
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Nexus CRM Shared Profile")
            putExtra(Intent.EXTRA_TEXT, "Here is my contact profile:\n$dataString")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val chooser = Intent.createChooser(intent, "Share Profile Link").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No sharing application available", Toast.LENGTH_SHORT).show()
        }
    }
}
