package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DatabaseProvider
import com.example.data.entity.*
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout


class ContactDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ContactRepository by lazy { DatabaseProvider.getRepository(application) }
    private val TAG = "ContactDetailVM"

    private val _contactWithDetails = MutableStateFlow<ContactWithDetails?>(null)
    val contactWithDetails: StateFlow<ContactWithDetails?> = _contactWithDetails.asStateFlow()

    private val _scheduledMessages = MutableStateFlow<List<ScheduledMessage>>(emptyList())
    val scheduledMessages: StateFlow<List<ScheduledMessage>> = _scheduledMessages.asStateFlow()

    private val _paginatedNotes = MutableStateFlow<List<Note>>(emptyList())
    val paginatedNotes: StateFlow<List<Note>> = _paginatedNotes.asStateFlow()

    private val _isLoadingNotes = MutableStateFlow(false)
    val isLoadingNotes: StateFlow<Boolean> = _isLoadingNotes.asStateFlow()

    private var notesOffset = 0
    private val pageSize = 20
    private var endOfNotesReached = false
    private val notesLock = Any()

    private var autoSaveJob: Job? = null
    private var loadJob: Job? = null
    private var scheduledMessagesJob: Job? = null
    private var contactId: Long = 0L
    private val contactIdMutex = Mutex()
    private var lastSavedContact: Contact? = null

    fun loadNextNotesPage() {
        val currentContactId = contactId
        if (currentContactId <= 0L || endOfNotesReached || _isLoadingNotes.value) return

        viewModelScope.launch {
            synchronized(notesLock) {
                if (_isLoadingNotes.value || endOfNotesReached) return@launch
                _isLoadingNotes.value = true
            }
            try {
                val newNotes = repository.getNotesPaginated(currentContactId, limit = pageSize, offset = notesOffset)
                if (newNotes.size < pageSize) {
                    endOfNotesReached = true
                }
                _paginatedNotes.value = _paginatedNotes.value + newNotes
                notesOffset += newNotes.size
                Log.d(TAG, "Loaded paginated notes page. New list size = ${_paginatedNotes.value.size}, reachedEnd = $endOfNotesReached")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading notes paginated", e)
            } finally {
                _isLoadingNotes.value = false
            }
        }
    }

    fun resetNotesPagination() {
        viewModelScope.launch {
            synchronized(notesLock) {
                notesOffset = 0
                endOfNotesReached = false
                _paginatedNotes.value = emptyList()
            }
            loadNextNotesPage()
        }
    }

    private fun observeScheduledMessages(id: Long) {
        scheduledMessagesJob?.cancel()
        scheduledMessagesJob = viewModelScope.launch {
            repository.getScheduledMessagesForContact(id)
                .catch { e -> Log.e(TAG, "Error collecting scheduled messages", e) }
                .collect { list ->
                    _scheduledMessages.value = list
                }
        }
    }

    fun loadContact(id: Long) {
        Log.d(TAG, "loadContact id=$id, current=$contactId")
        
        // Cancel existing jobs
        loadJob?.cancel()
        scheduledMessagesJob?.cancel()
        loadJob = null
        
        // Reset state
        _contactWithDetails.value = null
        _scheduledMessages.value = emptyList()
        contactId = id
        
        if (id <= 0L) {
            // Pre-create a contact draft immediately to prevent lazy creation race conditions
            loadJob = viewModelScope.launch {
                val draft = Contact()
                try {
                    val newId = repository.insertContact(draft)
                    contactId = newId
                    val draftWithId = draft.copy(id = newId)
                    _contactWithDetails.value = ContactWithDetails(draftWithId)
                    lastSavedContact = draftWithId
                    observeScheduledMessages(newId)
                    resetNotesPagination()
                    Log.d(TAG, "Created pre-edit contact draft id: $newId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error inserting contact draft in loadContact", e)
                    // Fallback to in-memory only if database insertion fails
                    _contactWithDetails.value = ContactWithDetails(Contact())
                }
            }
            return
        }
        
        // Observe messages after contact ID is set
        observeScheduledMessages(id)
        resetNotesPagination()
        
        // Collect Flow from database to automatically hear updates of related entities too (e.g. custom actions, phone numbers, addresses, emails)
        loadJob = viewModelScope.launch {
            repository.getContactById(id)
                .catch { e -> 
                    Log.e(TAG, "Error loading contact id=$id", e)
                    _contactWithDetails.value = null
                }
                .collect { details ->
                    if (details != null) {
                        _contactWithDetails.value = details
                        lastSavedContact = details.contact
                    }
                }
        }
    }

    fun loadUserProfile() {
        loadJob?.cancel()
        _contactWithDetails.value = null
        _scheduledMessages.value = emptyList()
        loadJob = viewModelScope.launch {
            // Find existing user profile unit
            repository.getUserProfile()
                .catch { e -> Log.e(TAG, "Error collecting user profile (likely DB closed)", e) }
                .collect { details ->
                    if (details != null) {
                        _contactWithDetails.value = details
                        contactId = details.contact.id
                        lastSavedContact = details.contact
                        observeScheduledMessages(details.contact.id)
                        resetNotesPagination()
                    } else {
                        // Create if not exists
                        val profileId = repository.insertContact(
                            Contact(
                                firstName = "My Name",
                                lastName = "My Surnames",
                                nickname = "Me",
                                isUserProfile = true
                            )
                        )
                        // Insert sample details if blank
                        repository.insertPhoneNumber(PhoneNumber(contactId = profileId, number = "+1 (555) 012-3456", label = "mobile"))
                        repository.insertEmail(Email(contactId = profileId, email = "me@example.com", label = "personal"))
                        contactId = profileId
                        observeScheduledMessages(profileId)
                        resetNotesPagination()
                    }
                }
        }
    }

    // Live update fields with debounce
    fun updateField(block: (Contact) -> Contact) {
        val currentDetails = _contactWithDetails.value ?: return
        val updatedContact = block(currentDetails.contact)
        _contactWithDetails.value = currentDetails.copy(contact = updatedContact)

        triggerAutoSave(updatedContact)
    }

    fun setProfilePhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            val localPath = withContext(Dispatchers.IO) {
                com.example.util.ImageUtils.saveUriToLocalFile(context, uri.toString())
            }
            if (localPath != null) {
                updateField { it.copy(profilePhotoUri = localPath) }
            }
        }
    }

    private fun triggerAutoSave(contact: Contact) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500) // 500ms inactive debounce
            saveContactImmediately(contact)
        }
    }

    private var forceSaveJob: Job? = null

    fun forceSaveOnPause() {
        forceSaveJob?.cancel() // Cancel previous if running
        forceSaveJob = viewModelScope.launch(Dispatchers.IO) {
            _contactWithDetails.value?.contact?.let { contact ->
                saveContactSync(contact)
            }
        }
    }

    private suspend fun saveContactSync(contact: Contact) {
        if (contact == lastSavedContact) {
            Log.d(TAG, "Skipping save: contact content is unchanged")
            return
        }
        if (contact.id <= 0L) {
            val newId = repository.insertContact(contact)
            contactIdMutex.withLock {
                if (contactId <= 0L) {
                   contactId = newId
                }
            }
            val saved = contact.copy(id = newId)
            lastSavedContact = saved
            _contactWithDetails.value = _contactWithDetails.value?.copy(
                contact = saved
            )
            Log.d(TAG, "Inserted new contact draft id: $newId")
        } else {
            repository.updateContact(contact)
            lastSavedContact = contact
            Log.d(TAG, "Auto-saved contact id: ${contact.id}")
        }
    }

    private fun saveContactImmediately(contact: Contact) {
        viewModelScope.launch {
            saveContactSync(contact)
        }
    }

    // Children table additions (Instant confirming saves)
    fun addPhoneNumber(number: String, label: String) {
        viewModelScope.launch {
            try {
                ensureContactId()
                repository.insertPhoneNumber(PhoneNumber(contactId = contactId, number = number, label = label))
            } catch (e: Exception) {
                Log.e(TAG, "Error adding phone number", e)
            }
        }
    }

    fun deletePhoneNumber(id: Long) {
        viewModelScope.launch {
            try {
                repository.deletePhoneNumber(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting phone number", e)
            }
        }
    }

    fun addEmail(email: String, label: String) {
        viewModelScope.launch {
            try {
                ensureContactId()
                repository.insertEmail(Email(contactId = contactId, email = email, label = label))
            } catch (e: Exception) {
                Log.e(TAG, "Error adding email", e)
            }
        }
    }

    fun deleteEmail(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteEmail(id)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting email", e)
            }
        }
    }

    fun addCustomAction(label: String, iconResName: String, actionType: String, targetData: String) {
        viewModelScope.launch {
            ensureContactId()
            repository.insertCustomAction(
                CustomAction(
                    contactId = contactId,
                    label = label,
                    iconResName = iconResName,
                    actionType = actionType,
                    targetData = targetData
                )
            )
        }
    }

    fun updateCustomAction(action: CustomAction) {
        viewModelScope.launch {
            repository.updateCustomAction(action)
        }
    }

    fun deleteCustomAction(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomAction(id)
        }
    }

    fun addNote(content: String, isInteraction: Boolean = false) {
        viewModelScope.launch {
            ensureContactId()
            val note = Note(
                contactId = contactId,
                content = content,
                createdAtMillis = System.currentTimeMillis(),
                isInteraction = isInteraction
            )
            val insertedId = repository.insertNote(note)
            val noteWithId = note.copy(noteId = insertedId)
            
            // Prepend new note at the top of memory logs
            _paginatedNotes.value = listOf(noteWithId) + _paginatedNotes.value
            notesOffset++
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            // Filter deleted note and reduce offset accordingly
            val beforeSize = _paginatedNotes.value.size
            _paginatedNotes.value = _paginatedNotes.value.filter { it.noteId != id }
            val removedCount = beforeSize - _paginatedNotes.value.size
            notesOffset = (notesOffset - removedCount).coerceAtLeast(0)
        }
    }

    fun addAddress(label: String, lat: Double, lng: Double, formattedAddress: String) {
        viewModelScope.launch {
            ensureContactId()
            repository.insertAddress(
                Address(
                    contactId = contactId,
                    label = label,
                    latitude = lat,
                    longitude = lng,
                    formattedAddress = formattedAddress
                )
            )
        }
    }

    fun deleteAddress(id: Long) {
        viewModelScope.launch {
            repository.deleteAddress(id)
        }
    }

    private suspend fun ensureContactId() {
        if (contactId > 0L) return
        
        Log.d(TAG, "ensureContactId called")
        val startTime = System.currentTimeMillis()
        
        try {
            withTimeout(5000L) {
                contactIdMutex.withLock {
                    if (contactId > 0L) return@withLock
                    
                    try {
                        val contact = _contactWithDetails.value?.contact ?: Contact()
                        if (contact.id <= 0L) {
                            val newId = repository.insertContact(contact)
                            contactId = newId
                            _contactWithDetails.value = _contactWithDetails.value?.copy(
                                contact = contact.copy(id = newId)
                            )
                        } else {
                            contactId = contact.id
                        }
                    } catch (e: Exception) {
                        // If database is closed, try to re-initialize
                        if (e.message?.contains("closed") == true) {
                            Log.w(TAG, "Database was closed, attempting to reinitialize")
                            withContext(Dispatchers.IO) {
                                // Re-initialize with current decoy mode
                                val decoyMode = DatabaseProvider.getCurrentDecoyMode()
                                DatabaseProvider.initDatabase(getApplication(), useDecoy = decoyMode)
                            }
                            // Retry the operation
                            val contact = _contactWithDetails.value?.contact ?: Contact()
                            if (contact.id <= 0L) {
                                val newId = repository.insertContact(contact)
                                contactId = newId
                                _contactWithDetails.value = _contactWithDetails.value?.copy(
                                    contact = contact.copy(id = newId)
                                )
                            } else {
                                contactId = contact.id
                            }
                        } else {
                            throw e
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "ensureContactId timed out after ${System.currentTimeMillis() - startTime}ms", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure contact ID", e)
            throw e
        }
    }

    fun scheduleMessage(
        context: Context,
        recipientName: String,
        type: String,         // "SMS" or "EMAIL"
        recipientValue: String,
        messageContent: String,
        scheduleTimeMillis: Long,
        recurrence: String = "ONCE" // "ONCE", "DAILY", "WEEKLY", "MONTHLY", "BIRTHDAY", "ONE_HOUR", "FOLLOW_UP"
    ) {
        viewModelScope.launch {
            ensureContactId()
            val message = ScheduledMessage(
                contactId = contactId,
                recipientName = recipientName,
                type = type,
                recipientValue = recipientValue,
                messageContent = messageContent,
                scheduleTimeMillis = scheduleTimeMillis,
                recurrence = recurrence,
                status = "PENDING"
            )
            val savedId = repository.insertScheduledMessage(message)
            val insertedMessage = message.copy(id = savedId)

            // Queue with WorkManager
            com.example.worker.ScheduledMessageWorker.scheduleMessageWork(context, insertedMessage)
            Log.d(TAG, "Successfully saved and scheduled message of ID: $savedId")
        }
    }

    fun cancelScheduledMessage(context: Context, id: Long) {
        viewModelScope.launch {
            com.example.worker.ScheduledMessageWorker.cancelScheduledWork(context, id)
            repository.deleteScheduledMessageById(id)
            Log.d(TAG, "Successfully cancelled message of ID: $id")
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        loadJob?.cancel()
        scheduledMessagesJob?.cancel()
        forceSaveJob?.cancel()
    }
}
