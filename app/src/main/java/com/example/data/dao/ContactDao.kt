package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Transaction
    @Query("SELECT * FROM contacts WHERE isUserProfile = 0 ORDER BY rating DESC, firstName ASC")
    fun getAllContacts(): Flow<List<ContactWithDetails>>

    @Transaction
    @Query("SELECT * FROM contacts WHERE isUserProfile = 0 ORDER BY rating DESC, firstName ASC")
    fun getAllContactsForList(): Flow<List<ContactWithListDetails>>

    @Transaction
    @Query("SELECT * FROM contacts WHERE isUserProfile = 0 ORDER BY rating DESC, firstName ASC")
    suspend fun getAllContactsList(): List<ContactWithDetails>

    @Transaction
    @Query("SELECT * FROM contacts WHERE isUserProfile = 1 LIMIT 1")
    fun getUserProfile(): Flow<ContactWithDetails?>

    @Transaction
    @Query("SELECT * FROM contacts WHERE isUserProfile = 1 LIMIT 1")
    suspend fun getUserProfileSuspend(): ContactWithDetails?

    @Transaction
    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactById(id: Long): Flow<ContactWithDetails?>

    @Transaction
    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactByIdSuspend(id: Long): ContactWithDetails?

    @Transaction
    @Query("""
        SELECT DISTINCT c.* FROM contacts c
        LEFT JOIN notes n ON n.contactId = c.id
        LEFT JOIN custom_actions a ON a.contactId = c.id
        WHERE c.isUserProfile = 0 AND (
            c.firstName LIKE :query OR
            c.lastName LIKE :query OR
            c.nickname LIKE :query OR
            c.notes LIKE :query OR
            c.tag LIKE :query OR
            n.content LIKE :query OR
            a.label LIKE :query
        )
        ORDER BY c.rating DESC, c.firstName ASC
    """)
    fun searchContacts(query: String): Flow<List<ContactWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    // Child table: Phone Numbers
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoneNumber(phoneNumber: PhoneNumber): Long

    @Query("DELETE FROM phone_numbers WHERE id = :id")
    suspend fun deletePhoneNumber(id: Long)

    // Child table: Emails
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: Email): Long

    @Query("DELETE FROM emails WHERE id = :id")
    suspend fun deleteEmail(id: Long)

    // Child table: Custom Actions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomAction(action: CustomAction): Long

    @Update
    suspend fun updateCustomAction(action: CustomAction)

    @Query("DELETE FROM custom_actions WHERE actionId = :id")
    suspend fun deleteCustomAction(id: Long)

    // Child table: Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Query("DELETE FROM notes WHERE noteId = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT * FROM notes WHERE contactId = :contactId ORDER BY createdAtMillis DESC LIMIT :limit OFFSET :offset")
    suspend fun getNotesPaginated(contactId: Long, limit: Int, offset: Int): List<Note>

    // Child table: Addresses
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: Address): Long

    @Query("DELETE FROM addresses WHERE addressId = :id")
    suspend fun deleteAddress(id: Long)

    @Transaction
    @Query("SELECT * FROM addresses")
    fun getAllAddresses(): Flow<List<Address>>

    // Last Scanned interactions tracking helpers
    @Query("SELECT lastScannedId FROM phone_number_match WHERE type = :type LIMIT 1")
    suspend fun getLastScannedId(type: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLastScanned(phoneNumberMatch: PhoneNumberMatch)

    // Scheduled Messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledMessage(message: ScheduledMessage): Long

    @Update
    suspend fun updateScheduledMessage(message: ScheduledMessage)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteScheduledMessageById(id: Long)

    @Query("SELECT * FROM scheduled_messages WHERE contactId = :contactId ORDER BY scheduleTimeMillis ASC")
    fun getScheduledMessagesForContact(contactId: Long): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun getScheduledMessageById(id: Long): ScheduledMessage?

    @Query("SELECT * FROM scheduled_messages WHERE status = 'PENDING' ORDER BY scheduleTimeMillis ASC")
    suspend fun getPendingScheduledMessages(): List<ScheduledMessage>

    @Query("SELECT * FROM scheduled_messages ORDER BY scheduleTimeMillis ASC")
    fun getAllScheduledMessages(): Flow<List<ScheduledMessage>>
}
