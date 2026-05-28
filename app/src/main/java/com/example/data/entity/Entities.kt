package com.example.data.entity

import androidx.room.*

@Entity(
    tableName = "contacts",
    indices = [
        Index("rating"),
        Index("tag"),
        Index("firstName", "lastName"),
        Index("lastInteractionInMillis")
    ]
)
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String = "",
    val lastName: String = "",
    val nickname: String? = null,
    val pronouns: String? = null,
    val rating: Int = 0, // 1-10 rating
    val profilePhotoUri: String? = null,
    val birthdayInMillis: Long? = null,
    val lastInteractionInMillis: Long? = null,
    val isUserProfile: Boolean = false, // True for My Profile, false for other contacts
    val notes: String? = null,
    val tag: String? = null
)

@Entity(
    tableName = "phone_numbers",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contactId")]
)
data class PhoneNumber(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val number: String,
    val label: String // "mobile", "work", etc.
)

@Entity(
    tableName = "emails",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contactId")]
)
data class Email(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val email: String,
    val label: String // "personal", "work", etc.
)

@Entity(
    tableName = "custom_actions",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contactId")]
)
data class CustomAction(
    @PrimaryKey(autoGenerate = true) val actionId: Long = 0,
    val contactId: Long,
    val label: String, // e.g. "Signal", "Discord"
    val iconResName: String, // Material icon identifier or similar
    val actionType: String, // "PHONE_CALL", "SMS", "WHATSAPP", "TELEGRAM", "SIGNAL", "URL", "APP_PACKAGE", "MAP_COORDINATES", "EMAIL"
    val targetData: String // Phone number, URL, Package Name, Coordinates
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contactId"), Index("isInteraction")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val noteId: Long = 0,
    val contactId: Long,
    val content: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isInteraction: Boolean = false
)

@Entity(
    tableName = "addresses",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contactId")]
)
data class Address(
    @PrimaryKey(autoGenerate = true) val addressId: Long = 0,
    val contactId: Long,
    val label: String, // e.g. "Home", "Office"
    val latitude: Double,
    val longitude: Double,
    val formattedAddress: String
)

@Entity(tableName = "phone_number_match")
data class PhoneNumberMatch(
    @PrimaryKey val type: String, // "call_log" or "sms"
    val lastScannedId: Long
)

@Entity(
    tableName = "scheduled_messages",
    foreignKeys = [
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("contactId"), Index("status"), Index("scheduleTimeMillis")]
)
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val recipientName: String,
    val type: String, // "SMS" or "EMAIL"
    val recipientValue: String, // e.g. mobile number or email address
    val messageContent: String,
    val scheduleTimeMillis: Long,
    val recurrence: String = "ONCE", // "ONCE", "DAILY", "WEEKLY", "MONTHLY", "BIRTHDAY", "ONE_HOUR", "FOLLOW_UP"
    val status: String = "PENDING", // "PENDING", "SENT", "FAILED", "CANCELLED"
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttempt: Long? = null,
    val errorMessage: String? = null
)

