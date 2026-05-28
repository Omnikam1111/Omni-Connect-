package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DatabaseProvider
import com.example.data.SettingsPreferences
import com.example.data.entity.*
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed interface AuthState {
    object Authenticating : AuthState
    object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}

enum class ContactSortOrder {
    RATING_DESC,
    TAG_ASC,
    FIRST_NAME_ASC,
    FIRST_NAME_DESC
}

private data class FilterParams(
    val query: String,
    val tag: String?,
    val sort: ContactSortOrder,
    val pendingDeletionId: Long?
)

class ContactListViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ContactListViewModel"
    }

    private val repository: ContactRepository by lazy { DatabaseProvider.getRepository(getApplication()) }
    val preferences = SettingsPreferences(application)

    // Auth screen states
    private val _authState = MutableStateFlow<AuthState>(AuthState.Authenticating)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isDecoyActive = MutableStateFlow(false)
    val isDecoyActive: StateFlow<Boolean> = _isDecoyActive.asStateFlow()

    // Filters and search states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _sortOrder = MutableStateFlow<ContactSortOrder>(
        run {
            try {
                ContactSortOrder.valueOf(preferences.savedSortOrder)
            } catch (e: Exception) {
                ContactSortOrder.FIRST_NAME_ASC
            }
        }
    )
    val sortOrder: StateFlow<ContactSortOrder> = _sortOrder.asStateFlow()

    // Theme options flows
    private val _isCustomThemeEnabled = MutableStateFlow(preferences.isCustomThemeEnabled)
    val isCustomThemeEnabled: StateFlow<Boolean> = _isCustomThemeEnabled.asStateFlow()

    private val _isCustomThemeUnlocked = MutableStateFlow(preferences.isCustomThemeUnlocked)
    val isCustomThemeUnlocked: StateFlow<Boolean> = _isCustomThemeUnlocked.asStateFlow()

    private val _selectedThemePreset = MutableStateFlow(preferences.selectedThemePreset)
    val selectedThemePreset: StateFlow<String> = _selectedThemePreset.asStateFlow()

    private val _customPrimaryColor = MutableStateFlow(preferences.customPrimaryColor)
    val customPrimaryColor: StateFlow<Int> = _customPrimaryColor.asStateFlow()

    private val _customSecondaryColor = MutableStateFlow(preferences.customSecondaryColor)
    val customSecondaryColor: StateFlow<Int> = _customSecondaryColor.asStateFlow()

    private val _customBackgroundColor = MutableStateFlow(preferences.customBackgroundColor)
    val customBackgroundColor: StateFlow<Int> = _customBackgroundColor.asStateFlow()

    private val _customSurfaceColor = MutableStateFlow(preferences.customSurfaceColor)
    val customSurfaceColor: StateFlow<Int> = _customSurfaceColor.asStateFlow()

    private val _customDeleteColor = MutableStateFlow(preferences.customDeleteColor)
    val customDeleteColor: StateFlow<Int> = _customDeleteColor.asStateFlow()

    // Shader background flow options
    private val _isShaderBackgroundEnabled = MutableStateFlow(preferences.isShaderBackgroundEnabled)
    val isShaderBackgroundEnabled: StateFlow<Boolean> = _isShaderBackgroundEnabled.asStateFlow()

    private val _selectedShaderPreset = MutableStateFlow(preferences.selectedShaderPreset)
    val selectedShaderPreset: StateFlow<String> = _selectedShaderPreset.asStateFlow()

    private val _customShaderCode = MutableStateFlow(preferences.customShaderCode)
    val customShaderCode: StateFlow<String> = _customShaderCode.asStateFlow()

    private val _shaderSeed = MutableStateFlow(preferences.shaderSeed)
    val shaderSeed: StateFlow<Float> = _shaderSeed.asStateFlow()

    private val _contactCardOpacity = MutableStateFlow(preferences.contactCardOpacity)
    val contactCardOpacity: StateFlow<Float> = _contactCardOpacity.asStateFlow()

    private val _savedShaderNames = MutableStateFlow(preferences.getSavedShaderNames())
    val savedShaderNames: StateFlow<Set<String>> = _savedShaderNames.asStateFlow()

    private val _appLanguage = MutableStateFlow(preferences.appLanguage)
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _preferencesRefreshTrigger = MutableStateFlow(0)
    val preferencesRefreshTrigger: StateFlow<Int> = _preferencesRefreshTrigger.asStateFlow()

    // Combined/Filtered contacts
    private val _contacts = MutableStateFlow<List<ContactWithListDetails>>(emptyList())
    val contacts: StateFlow<List<ContactWithListDetails>> = _contacts.asStateFlow()

    // Undo delete manager (uses a memory-buffered deferred delete approach to ensure perfect cascading preservation)
    @Volatile
    private var recentlyDeletedContact: ContactWithDetails? = null
    private val _pendingDeletionId = MutableStateFlow<Long?>(null)
    private var deletionJob: Job? = null
    private var contactsJob: Job? = null
    private var currentDecoyMode = false
    private var isDatabaseReady = false  // ✅ Track database state

    init {
        Log.d(TAG, "ViewModel init started")
        // ✅ Don't do heavy operations in init
        // Just set up initial state
        _authState.value = AuthState.Authenticating
    }

    // ✅ Call this from a safe context
    fun initialize() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing ViewModel")
                // Check authentication requirements
                if (!preferences.isBiometricEnabled && !preferences.hasPinConfigured) {
                    withContext(Dispatchers.IO) {
                        bypassAuthentication()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in initialization", e)
                _authState.value = AuthState.Error("Failed to initialize: ${e.message}")
            }
        }
    }

    private suspend fun bypassAuthentication() {
        try {
            Log.d(TAG, "Bypassing authentication")
            // ✅ Initialize database on IO thread
            withContext(Dispatchers.IO) {
                if (!isDatabaseReady || currentDecoyMode != false) {
                    DatabaseProvider.initDatabase(getApplication(), useDecoy = false)
                    isDatabaseReady = true
                    currentDecoyMode = false
                }
            }
            _isDecoyActive.value = false
            _authState.value = AuthState.Authenticated
            observeContacts()
        } catch (e: Exception) {
            Log.e(TAG, "Error in bypassAuthentication", e)
            _authState.value = AuthState.Error("Database initialization failed: ${e.message}")
        }
    }

    fun verifyPin(pin: String): Boolean {
        viewModelScope.launch {
            try {
                contactsJob?.cancel()
                contactsJob = null
                
                val targetDecoyMode = when (pin) {
                    preferences.realPin -> false
                    preferences.decoyPin -> true
                    else -> null
                }
                
                if (targetDecoyMode != null) {
                    withContext(Dispatchers.IO) {
                        // ONLY initialize if mode changed OR not ready
                        if (!isDatabaseReady || currentDecoyMode != targetDecoyMode) {
                            Log.d(TAG, "Initializing database with decoy=$targetDecoyMode")
                            DatabaseProvider.initDatabase(getApplication(), useDecoy = targetDecoyMode)
                            isDatabaseReady = true
                            currentDecoyMode = targetDecoyMode
                        } else {
                            Log.d(TAG, "Database already initialized, reusing connection")
                        }
                    }
                    _isDecoyActive.value = targetDecoyMode
                    _authState.value = AuthState.Authenticated
                    observeContacts()
                } else {
                    _authState.value = AuthState.Error("Incorrect PIN. Standard entries only.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in verifyPin", e)
                _authState.value = AuthState.Error("Authentication failed: ${e.message}")
            }
        }
        return false // Return proper value based on result
    }

    fun forceResetAuthError() {
        _authState.value = AuthState.Authenticating
    }

    fun logout() {
        contactsJob?.cancel()
        contactsJob = null
        
        // Immediately execute pending delete on logout
        val idToDelete = _pendingDeletionId.value
        val contactToDelete = recentlyDeletedContact
        if (idToDelete != null && contactToDelete != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repository.deleteContact(contactToDelete.contact)
                } catch (e: Exception) {
                    Log.e(TAG, "Error performing deferred delete in logout", e)
                }
            }
        }
        _pendingDeletionId.value = null
        recentlyDeletedContact = null
        
        // DON'T close database - it may be in use by DetailViewModel
        
        _authState.value = AuthState.Authenticating
        _contacts.value = emptyList()
        isDatabaseReady = false
    }

    private fun observeContacts() {
        if (!isDatabaseReady) {
            Log.w(TAG, "Database not ready, skipping observeContacts")
            return
        }
        
        contactsJob?.cancel()
        
        try {
            // Highly optimized combining logic: cache database queries on list elements, 
            // avoiding SQLite restarts on keypress fuzzy searches
            val dbFlow = repository.getAllContactsForList()
                .catch { e ->
                    Log.e(TAG, "Error in repository flow", e)
                    emit(emptyList())
                }

            val filterFlow = combine(
                _searchQuery,
                _selectedTag,
                _sortOrder,
                _pendingDeletionId
            ) { query, tag, sort, pendingId ->
                FilterParams(query, tag, sort, pendingId)
            }

            contactsJob = dbFlow.combine(filterFlow) { list, params ->
                try {
                    val activeList = if (params.pendingDeletionId != null) {
                        list.filter { it.contact.id != params.pendingDeletionId }
                    } else {
                        list
                    }

                    val rawQuery = params.query.trim()
                    val queryWords = rawQuery.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }

                    var filtered = if (queryWords.isEmpty()) {
                        activeList
                    } else {
                        activeList.filter { item ->
                            queryWords.all { word ->
                                val inFirstName = item.contact.firstName.lowercase().contains(word)
                                val inLastName = item.contact.lastName.lowercase().contains(word)
                                val inNickname = item.contact.nickname?.lowercase()?.contains(word) == true
                                val inPronouns = item.contact.pronouns?.lowercase()?.contains(word) == true
                                val inNotes = item.contact.notes?.lowercase()?.contains(word) == true
                                val inTag = item.contact.tag?.lowercase()?.contains(word) == true
                                
                                val inPhoneNumbers = item.phoneNumbers.any { it.number.lowercase().contains(word) || it.label.lowercase().contains(word) }
                                val inEmails = item.emails.any { it.email.lowercase().contains(word) || it.label.lowercase().contains(word) }
                                val inCustomActions = item.customActions.any { it.label.lowercase().contains(word) }
                                val inAddresses = item.addresses.any { it.formattedAddress.lowercase().contains(word) || it.label.lowercase().contains(word) }

                                inFirstName || inLastName || inNickname || inPronouns || inNotes || inTag ||
                                        inPhoneNumbers || inEmails || inCustomActions || inAddresses
                            }
                        }
                    }

                    if (params.tag != null) {
                        filtered = filtered.filter { it.contact.tag == params.tag }
                    }

                    filtered = when (params.sort) {
                        ContactSortOrder.RATING_DESC -> filtered.sortedByDescending { it.contact.rating }
                        ContactSortOrder.TAG_ASC -> filtered.sortedWith(compareBy({ it.contact.tag ?: "zzz" }, { it.contact.firstName }))
                        ContactSortOrder.FIRST_NAME_ASC -> filtered.sortedBy { it.contact.firstName }
                        ContactSortOrder.FIRST_NAME_DESC -> filtered.sortedByDescending { it.contact.firstName }
                    }

                    filtered
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing contacts in-memory", e)
                    emptyList()
                }
            }
            .catch { e ->
                Log.e(TAG, "Error in combined contacts flow", e)
            }
            .onEach { processedList ->
                _contacts.value = processedList
            }
            .launchIn(viewModelScope)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up observeContacts", e)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun filterByTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun setSortOrder(order: ContactSortOrder) {
        _sortOrder.value = order
        preferences.savedSortOrder = order.name
    }

    private suspend fun reinsertContactWithDetails(details: ContactWithDetails) {
        // Explicitly reinsert the main contact first using its original ID
        repository.insertContact(details.contact)
        
        // Reinsert all cascading related entries safely using their original IDs
        for (phone in details.phoneNumbers) {
            repository.insertPhoneNumber(phone)
        }
        for (email in details.emails) {
            repository.insertEmail(email)
        }
        for (action in details.customActions) {
            repository.insertCustomAction(action)
        }
        for (note in details.notes) {
            repository.insertNote(note)
        }
        for (address in details.addresses) {
            repository.insertAddress(address)
        }
    }

    fun deleteContact(contactWithListDetails: ContactWithListDetails) {
        val contactId = contactWithListDetails.contact.id
        // Fast thread-safe check to prevent duplicate delete calls for the same contact
        if (_pendingDeletionId.value == contactId) {
            Log.d(TAG, "deleteContact: already deleting ID: $contactId, ignoring duplicate.")
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "deleteContact started: Immediately deleting ID: $contactId from database.")
                
                // Fetch complete details first on IO thread to ensure we have a perfect backup for Undo
                val fullContact = withContext(Dispatchers.IO) {
                    repository.getContactByIdSuspend(contactId)
                }
                
                if (fullContact == null) {
                    Log.e(TAG, "deleteContact: fullContact was NULL, cannot back up for Undo.")
                    return@launch
                }

                // Immediately back up to memory so it's ready for Undo action if requested
                recentlyDeletedContact = fullContact
                _pendingDeletionId.value = contactId

                // Immediately delete from SQLite database on IO thread
                withContext(Dispatchers.IO) {
                    repository.deleteContact(fullContact.contact)
                }
                Log.d(TAG, "deleteContact: physically deleted contact from SQLite database for ID: $contactId.")

                // Start a timer on Main thread to auto-expire/clear the undo option after 15 seconds
                deletionJob?.cancel()
                deletionJob = viewModelScope.launch {
                    delay(15000)
                    Log.d(TAG, "deleteContact undo timer expired. Clearing in-memory backup.")
                    _pendingDeletionId.value = null
                    recentlyDeletedContact = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in deleteContact", e)
            }
        }
    }

    fun undoDelete() {
        val pendingId = _pendingDeletionId.value
        val details = recentlyDeletedContact
        if (pendingId == null || details == null) {
            Log.e(TAG, "undoDelete: no pending deletion or backup found to restore.")
            return
        }
        Log.d(TAG, "undoDelete: restoring contact with ID: $pendingId")
        deletionJob?.cancel()

        // Clear the pending deletion filter immediately on the Main thread
        _pendingDeletionId.value = null

        viewModelScope.launch {
            try {
                // Physically restore/re-insert everything back into database with original IDs
                withContext(Dispatchers.IO) {
                    reinsertContactWithDetails(details)
                }
                Log.d(TAG, "undoDelete: successfully reinserted contact and all cascaded child records.")
                
                recentlyDeletedContact = null
                
                // Re-subscribe to the database flow to absolutely guarantee SQLite changes are emitted on physical tablets
                observeContacts()
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring contact in database during undoDelete", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        deletionJob?.cancel()
    }

    fun importContactWithDetails(context: Context, details: ContactWithDetails, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localPhotoPath = com.example.util.ImageUtils.saveUriToLocalFile(context, details.contact.profilePhotoUri)
                val contactToInsert = details.contact.copy(
                    id = 0,
                    profilePhotoUri = localPhotoPath ?: details.contact.profilePhotoUri
                )
                val contactId = repository.insertContact(contactToInsert)
                for (phone in details.phoneNumbers) {
                    repository.insertPhoneNumber(phone.copy(id = 0, contactId = contactId))
                }
                for (email in details.emails) {
                    repository.insertEmail(email.copy(id = 0, contactId = contactId))
                }
                withContext(Dispatchers.Main) {
                    observeContacts() // Refresh flow after single import
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing contact", e)
            }
        }
    }

    fun updateCustomAction(action: com.example.data.entity.CustomAction) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateCustomAction(action)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating custom action", e)
            }
        }
    }

    fun importBulkPhoneContacts(context: Context, contactsToImport: List<com.example.util.PhoneContact>, onSuccess: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingContacts = repository.getAllContactsList()
                val existingPhoneNumbers = existingContacts.flatMap { ec ->
                    ec.phoneNumbers.map { pn -> pn.number.replace("\\D".toRegex(), "") }
                }.filter { it.isNotEmpty() }.toSet()

                val existingEmails = existingContacts.flatMap { ec ->
                    ec.emails.map { em -> em.email.trim().lowercase() }
                }.filter { it.isNotEmpty() }.toSet()

                val existingNames = existingContacts.map { ec ->
                    "${ec.contact.firstName.trim().lowercase()} ${ec.contact.lastName.trim().lowercase()}"
                }.toSet()

                var count = 0
                for (phoneContact in contactsToImport) {
                    val parts = phoneContact.name.trim().split("\\s+".toRegex())
                    val firstName = parts.firstOrNull() ?: ""
                    val lastName = if (parts.size > 1) parts.subList(1, parts.size).joinToString(" ") else ""
                    val fullName = "$firstName $lastName".trim().lowercase()

                    val cleanedPhoneNumbers = phoneContact.phoneNumbers.map { it.replace("\\D".toRegex(), "") }.filter { it.isNotEmpty() }
                    val cleanedEmails = phoneContact.emails.map { it.trim().lowercase() }.filter { it.isNotEmpty() }

                    val isDuplicatePhone = cleanedPhoneNumbers.any { it in existingPhoneNumbers }
                    val isDuplicateEmail = cleanedEmails.any { it in existingEmails }
                    val isDuplicateName = fullName.isNotEmpty() && fullName in existingNames

                    if (isDuplicatePhone || isDuplicateEmail || isDuplicateName) {
                        Log.d(TAG, "Skipping duplicate contact: ${phoneContact.name}")
                        continue
                    }

                    val localPhotoPath = com.example.util.ImageUtils.saveUriToLocalFile(context, phoneContact.photoUri)

                    val contact = Contact(
                        firstName = firstName,
                        lastName = lastName,
                        profilePhotoUri = localPhotoPath ?: phoneContact.photoUri,
                        rating = 5
                    )
                    val contactId = repository.insertContact(contact)
                    for (num in phoneContact.phoneNumbers) {
                        val cleanedNum = num.replace("\\s+".toRegex(), "")
                        repository.insertPhoneNumber(com.example.data.entity.PhoneNumber(contactId = contactId, number = cleanedNum, label = "mobile"))
                    }
                    for (emailAddress in phoneContact.emails) {
                        repository.insertEmail(com.example.data.entity.Email(contactId = contactId, email = emailAddress, label = "personal"))
                    }
                    count++
                }
                withContext(Dispatchers.Main) {
                    observeContacts() // Refresh flow after bulk import
                    onSuccess(count)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in bulk import", e)
            }
        }
    }

    fun updateThemeEnabled(enabled: Boolean) {
        preferences.isCustomThemeEnabled = enabled
        _isCustomThemeEnabled.value = enabled
    }

    fun updateThemeUnlocked(unlocked: Boolean) {
        preferences.isCustomThemeUnlocked = unlocked
        _isCustomThemeUnlocked.value = unlocked
        if (unlocked) {
            updateThemeEnabled(true)
        }
    }

    fun selectThemePreset(presetId: String) {
        preferences.selectedThemePreset = presetId
        _selectedThemePreset.value = presetId
        when (presetId) {
            "classic" -> {
                updatePrimaryColor(0xFFD0BCFF.toInt())
                updateSecondaryColor(0xFFCCC2DC.toInt())
                updateBackgroundColor(0xFF141218.toInt())
                updateSurfaceColor(0xFF1D1B20.toInt())
                updateDeleteColor(0xFFFF5252.toInt())
            }
            "midnight_ocean" -> {
                updatePrimaryColor(0xFF00ADB5.toInt())
                updateSecondaryColor(0xFF393E46.toInt())
                updateBackgroundColor(0xFF1A1F29.toInt())
                updateSurfaceColor(0xFF242B35.toInt())
                updateDeleteColor(0xFFFF2E63.toInt())
            }
            "forest_sage" -> {
                updatePrimaryColor(0xFF81B214.toInt())
                updateSecondaryColor(0xFF206A5D.toInt())
                updateBackgroundColor(0xFF101715.toInt())
                updateSurfaceColor(0xFF16211D.toInt())
                updateDeleteColor(0xFFD24E4E.toInt())
            }
            "sunset_glow" -> {
                updatePrimaryColor(0xFFF07B3F.toInt())
                updateSecondaryColor(0xFFEA5455.toInt())
                updateBackgroundColor(0xFF251B17.toInt())
                updateSurfaceColor(0xFF312520.toInt())
                updateDeleteColor(0xFFD80032.toInt())
            }
            "cyberpunk_neon" -> {
                updatePrimaryColor(0xFF00F0FF.toInt())
                updateSecondaryColor(0xFFFF007F.toInt())
                updateBackgroundColor(0xFF0D0B18.toInt())
                updateSurfaceColor(0xFF18142C.toInt())
                updateDeleteColor(0xFFFF0055.toInt())
            }
        }
        updateThemeEnabled(true)
        updateThemeUnlocked(false)
    }

    fun resetToDefaultThemeColors() {
        selectThemePreset("classic")
    }

    fun updatePrimaryColor(color: Int) {
        preferences.customPrimaryColor = color
        _customPrimaryColor.value = color
    }

    fun updateSecondaryColor(color: Int) {
        preferences.customSecondaryColor = color
        _customSecondaryColor.value = color
    }

    fun updateBackgroundColor(color: Int) {
        preferences.customBackgroundColor = color
        _customBackgroundColor.value = color
    }

    fun updateSurfaceColor(color: Int) {
        preferences.customSurfaceColor = color
        _customSurfaceColor.value = color
    }

    fun updateDeleteColor(color: Int) {
        preferences.customDeleteColor = color
        _customDeleteColor.value = color
    }

    fun updateShaderBackgroundEnabled(enabled: Boolean) {
        preferences.isShaderBackgroundEnabled = enabled
        _isShaderBackgroundEnabled.value = enabled
    }

    fun updateSelectedShaderPreset(preset: String) {
        preferences.selectedShaderPreset = preset
        _selectedShaderPreset.value = preset
        randomizeShaderSeed()
    }

    fun randomizeShaderSeed() {
        val nextSeed = kotlin.random.Random.nextFloat() * 1000f + 1f
        preferences.shaderSeed = nextSeed
        _shaderSeed.value = nextSeed
    }

    fun updateCustomShaderCode(code: String) {
        preferences.customShaderCode = code
        _customShaderCode.value = code
    }

    fun updateContactCardOpacity(opacity: Float) {
        preferences.contactCardOpacity = opacity
        _contactCardOpacity.value = opacity
    }

    fun saveCustomShader(name: String, code: String) {
        preferences.saveCustomShader(name, code)
        _savedShaderNames.value = preferences.getSavedShaderNames()
    }

    fun deleteSavedShader(name: String) {
        preferences.deleteSavedShader(name)
        _savedShaderNames.value = preferences.getSavedShaderNames()
    }

    fun getSavedShaderCode(name: String): String {
        return preferences.getSavedShaderCode(name)
    }

    fun setAppLanguage(language: String) {
        preferences.appLanguage = language
        _appLanguage.value = language
        _preferencesRefreshTrigger.value = _preferencesRefreshTrigger.value + 1
    }

    fun refreshAllPreferences() {
        _appLanguage.value = preferences.appLanguage
        _isCustomThemeEnabled.value = preferences.isCustomThemeEnabled
        _isCustomThemeUnlocked.value = preferences.isCustomThemeUnlocked
        _selectedThemePreset.value = preferences.selectedThemePreset
        _customPrimaryColor.value = preferences.customPrimaryColor
        _customSecondaryColor.value = preferences.customSecondaryColor
        _customBackgroundColor.value = preferences.customBackgroundColor
        _customSurfaceColor.value = preferences.customSurfaceColor
        _customDeleteColor.value = preferences.customDeleteColor
        _isShaderBackgroundEnabled.value = preferences.isShaderBackgroundEnabled
        _selectedShaderPreset.value = preferences.selectedShaderPreset
        _customShaderCode.value = preferences.customShaderCode
        _shaderSeed.value = preferences.shaderSeed
        _contactCardOpacity.value = preferences.contactCardOpacity
        _savedShaderNames.value = preferences.getSavedShaderNames()
        _preferencesRefreshTrigger.value = _preferencesRefreshTrigger.value + 1
    }
}
