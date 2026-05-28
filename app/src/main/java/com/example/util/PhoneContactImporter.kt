package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.example.data.entity.Contact
import com.example.data.entity.Email
import com.example.data.entity.PhoneNumber
import com.example.data.entity.ContactWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhoneContact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val photoUri: String? = null,
    val isSelected: Boolean = false
)

object PhoneContactImporter {

    // Load a single contact's details from Uri (e.g. from ACTION_PICK)
    suspend fun loadContactFromUri(context: Context, contactUri: Uri): ContactWithDetails? = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        var contactId: String? = null
        var displayName: String? = null
        var photoUri: String? = null

        contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

                if (idIdx != -1) contactId = cursor.getString(idIdx)
                if (nameIdx != -1) displayName = cursor.getString(nameIdx)
                if (photoIdx != -1) photoUri = cursor.getString(photoIdx)
            }
        }

        if (contactId == null || displayName == null) return@withContext null

        val phoneNumbers = getPhoneNumbersForContact(contentResolver, contactId!!)
        val emails = getEmailsForContact(contentResolver, contactId!!)

        val parts = displayName!!.trim().split("\\s+".toRegex())
        val firstName = parts.firstOrNull() ?: ""
        val lastName = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" ") else ""

        val contact = Contact(
            firstName = firstName,
            lastName = lastName,
            profilePhotoUri = photoUri,
            rating = 5
        )

        ContactWithDetails(
            contact = contact,
            phoneNumbers = phoneNumbers,
            emails = emails
        )
    }

    // Load all device contacts
    suspend fun fetchAllPhoneContacts(context: Context): List<PhoneContact> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contactsMap = mutableMapOf<String, PhoneContact>()

        // 1. Fetch display names and photo uris
        val cursor = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI
            ),
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndex(ContactsContract.Contacts._ID)
            val nameCol = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val photoCol = c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

            while (c.moveToNext()) {
                val id = if (idCol != -1) c.getString(idCol) else continue
                val name = if (nameCol != -1) c.getString(nameCol) ?: "Unknown" else "Unknown"
                val photoUri = if (photoCol != -1) c.getString(photoCol) else null
                
                contactsMap[id] = PhoneContact(id = id, name = name, photoUri = photoUri)
            }
        }

        // 2. Fetch all Phone Numbers in bulk
        val phoneCursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        phoneCursor?.use { pc ->
            val idCol = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numCol = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (pc.moveToNext()) {
                val contactId = if (idCol != -1) pc.getString(idCol) else continue
                val number = if (numCol != -1) pc.getString(numCol) ?: continue else continue
                val cleanedNum = number.replace("\\s+".toRegex(), "")
                val pcObj = contactsMap[contactId]
                if (pcObj != null) {
                    val updatedNums = pcObj.phoneNumbers + cleanedNum
                    contactsMap[contactId] = pcObj.copy(phoneNumbers = updatedNums)
                }
            }
        }

        // 3. Fetch all Emails in bulk
        val emailCursor = resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            ),
            null,
            null,
            null
        )

        emailCursor?.use { ec ->
            val idCol = ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addressCol = ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)

            while (ec.moveToNext()) {
                val contactId = if (idCol != -1) ec.getString(idCol) else continue
                val address = if (addressCol != -1) ec.getString(addressCol) ?: continue else continue
                val pcObj = contactsMap[contactId]
                if (pcObj != null) {
                    val updatedEmails = pcObj.emails + address
                    contactsMap[contactId] = pcObj.copy(emails = updatedEmails)
                }
            }
        }

        contactsMap.values.toList()
    }

    private fun getPhoneNumbersForContact(resolver: ContentResolver, contactId: String): List<PhoneNumber> {
        val list = mutableListOf<PhoneNumber>()
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        cursor?.use { c ->
            val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            while (c.moveToNext()) {
                val number = if (numIdx != -1) c.getString(numIdx) ?: continue else continue
                val cleanedNum = number.replace("\\s+".toRegex(), "")
                val type = if (typeIdx != -1) {
                    when (c.getInt(typeIdx)) {
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "mobile"
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "work"
                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "home"
                        else -> "other"
                    }
                } else "mobile"
                list.add(PhoneNumber(contactId = 0L, number = cleanedNum, label = type))
            }
        }
        return list
    }

    private fun getEmailsForContact(resolver: ContentResolver, contactId: String): List<Email> {
        val list = mutableListOf<Email>()
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.TYPE),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )
        cursor?.use { c ->
            val addressIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            val typeIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
            while (c.moveToNext()) {
                val address = if (addressIdx != -1) c.getString(addressIdx) ?: continue else continue
                val type = if (typeIdx != -1) {
                    when (c.getInt(typeIdx)) {
                        ContactsContract.CommonDataKinds.Email.TYPE_HOME -> "personal"
                        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> "work"
                        else -> "other"
                    }
                } else "personal"
                list.add(Email(contactId = 0L, email = address, label = type))
            }
        }
        return list
    }
}
