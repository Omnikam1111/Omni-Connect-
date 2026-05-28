package com.example.data.repository

import android.content.Context
import com.example.data.DatabaseProvider
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class ContactRepository(private val context: Context) {

    private val contactDao get() = DatabaseProvider.getDatabase(context).contactDao()

    fun getAllContacts(): Flow<List<ContactWithDetails>> = contactDao.getAllContacts().distinctUntilChanged()

    fun getAllContactsForList(): Flow<List<ContactWithListDetails>> = contactDao.getAllContactsForList().distinctUntilChanged()

    suspend fun getAllContactsList(): List<ContactWithDetails> = contactDao.getAllContactsList()

    fun getUserProfile(): Flow<ContactWithDetails?> = contactDao.getUserProfile().distinctUntilChanged()

    suspend fun getUserProfileSuspend(): ContactWithDetails? = contactDao.getUserProfileSuspend()

    fun getContactById(id: Long): Flow<ContactWithDetails?> = contactDao.getContactById(id).distinctUntilChanged()

    suspend fun getContactByIdSuspend(id: Long): ContactWithDetails? = contactDao.getContactByIdSuspend(id)

    fun searchContacts(query: String): Flow<List<ContactWithDetails>> {
        val formattedQuery = "%$query%"
        return contactDao.searchContacts(formattedQuery).distinctUntilChanged()
    }

    suspend fun insertContact(contact: Contact): Long = contactDao.insertContact(contact)

    suspend fun updateContact(contact: Contact) = contactDao.updateContact(contact)

    suspend fun deleteContact(contact: Contact) = contactDao.deleteContact(contact)

    suspend fun deleteContactById(id: Long) = contactDao.deleteContactById(id)

    // Child table operations
    suspend fun insertPhoneNumber(phoneNumber: PhoneNumber): Long = contactDao.insertPhoneNumber(phoneNumber)

    suspend fun deletePhoneNumber(id: Long) = contactDao.deletePhoneNumber(id)

    suspend fun insertEmail(email: Email): Long = contactDao.insertEmail(email)

    suspend fun deleteEmail(id: Long) = contactDao.deleteEmail(id)

    suspend fun insertCustomAction(action: CustomAction): Long = contactDao.insertCustomAction(action)

    suspend fun updateCustomAction(action: CustomAction) = contactDao.updateCustomAction(action)

    suspend fun deleteCustomAction(id: Long) = contactDao.deleteCustomAction(id)

    suspend fun insertNote(note: Note): Long = contactDao.insertNote(note)

    suspend fun deleteNote(id: Long) = contactDao.deleteNote(id)

    suspend fun getNotesPaginated(contactId: Long, limit: Int, offset: Int): List<Note> =
        contactDao.getNotesPaginated(contactId, limit, offset)

    suspend fun insertAddress(address: Address): Long = contactDao.insertAddress(address)

    suspend fun deleteAddress(id: Long) = contactDao.deleteAddress(id)

    fun getAllAddresses(): Flow<List<Address>> = contactDao.getAllAddresses().distinctUntilChanged()

    // Tracking helpers
    suspend fun getLastScannedId(type: String): Long? = contactDao.getLastScannedId(type)

    suspend fun insertLastScanned(phoneNumberMatch: PhoneNumberMatch) = contactDao.insertLastScanned(phoneNumberMatch)

    // Scheduled Messages
    suspend fun insertScheduledMessage(message: ScheduledMessage): Long = contactDao.insertScheduledMessage(message)

    suspend fun updateScheduledMessage(message: ScheduledMessage) = contactDao.updateScheduledMessage(message)

    suspend fun deleteScheduledMessageById(id: Long) = contactDao.deleteScheduledMessageById(id)

    fun getScheduledMessagesForContact(contactId: Long): Flow<List<ScheduledMessage>> = contactDao.getScheduledMessagesForContact(contactId).distinctUntilChanged()

    suspend fun getScheduledMessageById(id: Long): ScheduledMessage? = contactDao.getScheduledMessageById(id)

    suspend fun getPendingScheduledMessages(): List<ScheduledMessage> = contactDao.getPendingScheduledMessages()

    fun getAllScheduledMessages(): Flow<List<ScheduledMessage>> = contactDao.getAllScheduledMessages().distinctUntilChanged()
}
