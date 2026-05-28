package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.entity.*
import com.example.data.repository.ContactRepository
import com.example.data.DatabaseProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import android.util.Base64
import java.nio.charset.StandardCharsets

data class SavedShaderBackupModel(val name: String, val code: String)

data class PreferencesBackupModel(
    val isBiometricEnabled: Boolean = false,
    val realPin: String = "1234",
    val decoyPin: String = "5555",
    val hasPinConfigured: Boolean = true,
    val isCustomThemeEnabled: Boolean = false,
    val selectedThemePreset: String = "classic",
    val customPrimaryColor: Int = 0xFFD0BCFF.toInt(),
    val customSecondaryColor: Int = 0xFFCCC2DC.toInt(),
    val customBackgroundColor: Int = 0xFF141218.toInt(),
    val customSurfaceColor: Int = 0xFF1D1B20.toInt(),
    val customDeleteColor: Int = 0xFFFF5252.toInt(),
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: String = "587",
    val smtpUsername: String = "",
    val smtpPassword: String = "",
    val smtpSender: String = "",
    val smtpUseSsl: Boolean = false,
    val smtpUseTls: Boolean = true,
    val googleAccountEmail: String = "",
    val googleAccountName: String = "",
    val googleOAuthClientId: String = "",
    val googleOAuthClientSecret: String = "",
    val googleRefreshToken: String = "",
    val isShaderBackgroundEnabled: Boolean = false,
    val selectedShaderPreset: String = "stars",
    val customShaderCode: String = "",
    val shaderSeed: Float = 1.0f,
    val contactCardOpacity: Float = 1.0f
)

data class BackupFormat(
    val contacts: List<ContactBackupModel> = emptyList(),
    val savedShaders: List<SavedShaderBackupModel> = emptyList(),
    val preferences: PreferencesBackupModel? = null
)

data class ContactBackupModel(
    val firstName: String,
    val lastName: String,
    val nickname: String?,
    val pronouns: String?,
    val rating: Int,
    val profilePhotoUri: String?,
    val profilePhotoBase64: String? = null,
    val birthdayInMillis: Long?,
    val lastInteractionInMillis: Long?,
    val phoneNumbers: List<PhoneBackupModel>,
    val emails: List<EmailBackupModel>,
    val customActions: List<ActionBackupModel>,
    val notes: List<NoteBackupModel>,
    val addresses: List<AddressBackupModel>,
    val profileNotes: String?,
    val tag: String?
)

data class PhoneBackupModel(val number: String, val label: String)
data class EmailBackupModel(val email: String, val label: String)
data class ActionBackupModel(
    val label: String,
    val iconResName: String,
    val actionType: String,
    val targetData: String,
    val iconBase64: String? = null
)
data class NoteBackupModel(val content: String, val createdAtMillis: Long, val isInteraction: Boolean)
data class AddressBackupModel(val label: String, val latitude: Double, val longitude: Double, val formattedAddress: String)

object BackupRestoreHelper {
    private const val TAG = "BackupRestoreHelper"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupFormat::class.java)

    private fun readBytesFromUriOrPath(context: Context, uriString: String?): ByteArray? {
        if (uriString.isNullOrBlank()) return null
        try {
            if (uriString.startsWith("/")) {
                val file = java.io.File(uriString)
                if (file.exists()) {
                    return file.readBytes()
                }
            }
            val uri = android.net.Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                return inputStream.readBytes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read bytes for: $uriString", e)
        }
        return null
    }

    private fun saveBytesToLocalFile(context: Context, bytes: ByteArray, subDir: String, fileNamePrefix: String, extension: String): String? {
        try {
            val dir = java.io.File(context.filesDir, subDir)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = java.io.File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}_${(1000..9999).random()}.$extension")
            java.io.FileOutputStream(file).use { out ->
                out.write(bytes)
            }
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bytes to $subDir", e)
        }
        return null
    }

    private const val PASSPHRASE = "NexusSecureBackupKey_2026_CRM_System"

    private fun encrypt(plainText: String): String {
        val keyBytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(PASSPHRASE.toByteArray(StandardCharsets.UTF_8))
            .copyOf(16) // 128-bit AES key
        val secretKey = SecretKeySpec(keyBytes, "AES")
        
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val random = SecureRandom()
        val ivBytes = ByteArray(16)
        random.nextBytes(ivBytes)
        val ivSpec = IvParameterSpec(ivBytes)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        
        // Concat IV + encrypted bytes
        val combined = ByteArray(ivBytes.size + encryptedBytes.size)
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.size)
        System.arraycopy(encryptedBytes, 0, combined, ivBytes.size, encryptedBytes.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String {
        val sanitized = encryptedBase64.replace("\\s".toRegex(), "")
        val combined = Base64.decode(sanitized, Base64.NO_WRAP)
        if (combined.size < 16) throw IllegalArgumentException("Invalid encrypted backup data length")
        
        val ivBytes = ByteArray(16)
        System.arraycopy(combined, 0, ivBytes, 0, 16)
        
        val encryptedBytes = ByteArray(combined.size - 16)
        System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.size)
        
        val keyBytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(PASSPHRASE.toByteArray(StandardCharsets.UTF_8))
            .copyOf(16)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivSpec = IvParameterSpec(ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    fun exportToStream(context: Context, outputStream: OutputStream): Boolean {
        return try {
            val repository = DatabaseProvider.getRepository(context)
            
            // Runs on IO-safe context
            val contacts = io.getContactListSynchronously(repository)
            
            val backupList = contacts.map { d ->
                val profilePhotoBytes = readBytesFromUriOrPath(context, d.contact.profilePhotoUri)
                val profilePhotoB64 = profilePhotoBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

                ContactBackupModel(
                    firstName = d.contact.firstName,
                    lastName = d.contact.lastName,
                    nickname = d.contact.nickname,
                    pronouns = d.contact.pronouns,
                    rating = d.contact.rating,
                    profilePhotoUri = d.contact.profilePhotoUri,
                    profilePhotoBase64 = profilePhotoB64,
                    birthdayInMillis = d.contact.birthdayInMillis,
                    lastInteractionInMillis = d.contact.lastInteractionInMillis,
                    phoneNumbers = d.phoneNumbers.map { PhoneBackupModel(it.number, it.label) },
                    emails = d.emails.map { EmailBackupModel(it.email, it.label) },
                    customActions = d.customActions.map { act ->
                        val actionIconBytes = if (act.iconResName.contains("/social_icons/")) {
                            readBytesFromUriOrPath(context, act.iconResName)
                        } else null
                        val actionIconB64 = actionIconBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                        ActionBackupModel(
                            label = act.label,
                            iconResName = act.iconResName,
                            actionType = act.actionType,
                            targetData = act.targetData,
                            iconBase64 = actionIconB64
                        )
                    },
                    notes = d.notes.map { NoteBackupModel(it.content, it.createdAtMillis, it.isInteraction) },
                    addresses = d.addresses.map { AddressBackupModel(it.label, it.latitude, it.longitude, it.formattedAddress) },
                    profileNotes = d.contact.notes,
                    tag = d.contact.tag
                )
            }

            val preferences = com.example.data.SettingsPreferences(context)
            val shaderNames = preferences.getSavedShaderNames()
            val shaderBackups = shaderNames.map { name ->
                SavedShaderBackupModel(name = name, code = preferences.getSavedShaderCode(name))
            }

            val prefsBackup = PreferencesBackupModel(
                isBiometricEnabled = preferences.isBiometricEnabled,
                realPin = preferences.realPin,
                decoyPin = preferences.decoyPin,
                hasPinConfigured = preferences.hasPinConfigured,
                isCustomThemeEnabled = preferences.isCustomThemeEnabled,
                selectedThemePreset = preferences.selectedThemePreset,
                customPrimaryColor = preferences.customPrimaryColor,
                customSecondaryColor = preferences.customSecondaryColor,
                customBackgroundColor = preferences.customBackgroundColor,
                customSurfaceColor = preferences.customSurfaceColor,
                customDeleteColor = preferences.customDeleteColor,
                smtpHost = preferences.smtpHost,
                smtpPort = preferences.smtpPort,
                smtpUsername = preferences.smtpUsername,
                smtpPassword = preferences.smtpPassword,
                smtpSender = preferences.smtpSender,
                smtpUseSsl = preferences.smtpUseSsl,
                smtpUseTls = preferences.smtpUseTls,
                googleAccountEmail = preferences.googleAccountEmail,
                googleAccountName = preferences.googleAccountName,
                googleOAuthClientId = preferences.googleOAuthClientId,
                googleOAuthClientSecret = preferences.googleOAuthClientSecret,
                googleRefreshToken = preferences.googleRefreshToken,
                isShaderBackgroundEnabled = preferences.isShaderBackgroundEnabled,
                selectedShaderPreset = preferences.selectedShaderPreset,
                customShaderCode = preferences.customShaderCode,
                shaderSeed = preferences.shaderSeed,
                contactCardOpacity = preferences.contactCardOpacity
            )

            // Convert to JSON String
            val rawJson = adapter.toJson(BackupFormat(backupList, shaderBackups, prefsBackup))
            // Encrypt JSON
            val encryptedBase64 = encrypt(rawJson)

            // Write encrypted payload to outputStream
            outputStream.write(encryptedBase64.toByteArray(StandardCharsets.UTF_8))
            outputStream.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting backup JSON", e)
            false
        }
    }

    fun importFromStream(context: Context, inputStream: InputStream, mergeMode: Boolean): Boolean {
        return try {
            val content = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }.trim()
            val decContent = try {
                decrypt(content)
            } catch (e: Exception) {
                // If decryption fails, check if the string is just standard plaintext JSON (fallback)
                if (content.startsWith("{") && content.endsWith("}")) {
                    content
                } else {
                    throw e
                }
            }

            val backup = adapter.fromJson(decContent) ?: return false

            val repository = DatabaseProvider.getRepository(context)

            // Direct IO logic coroutine block
            io.importSynchronously(context, repository, backup, mergeMode)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error importing backup JSON", e)
            false
        }
    }

    private object io {
        // Runs inside standard thread limits safely
        fun getContactListSynchronously(repository: ContactRepository): List<ContactWithDetails> {
            return kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                repository.getAllContactsList()
            }
        }

        fun importSynchronously(context: Context, repository: ContactRepository, backup: BackupFormat, mergeMode: Boolean) {
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val preferences = com.example.data.SettingsPreferences(context)
                    if (!mergeMode) {
                        // Clear existing contacts
                        val existing = repository.getAllContactsList()
                        for (c in existing) {
                            repository.deleteContact(c.contact)
                        }

                        // Clear existing custom shaders
                        val existingShaders = preferences.getSavedShaderNames().toList()
                        for (name in existingShaders) {
                            preferences.deleteSavedShader(name)
                        }
                    }

                    // Restore preferences of current state if available
                    backup.preferences?.let { p ->
                        preferences.importPreferences(
                            isBiometricEnabled = p.isBiometricEnabled,
                            realPin = p.realPin,
                            decoyPin = p.decoyPin,
                            hasPinConfigured = p.hasPinConfigured,
                            isCustomThemeEnabled = p.isCustomThemeEnabled,
                            selectedThemePreset = p.selectedThemePreset,
                            customPrimaryColor = p.customPrimaryColor,
                            customSecondaryColor = p.customSecondaryColor,
                            customBackgroundColor = p.customBackgroundColor,
                            customSurfaceColor = p.customSurfaceColor,
                            customDeleteColor = p.customDeleteColor,
                            smtpHost = p.smtpHost,
                            smtpPort = p.smtpPort,
                            smtpUsername = p.smtpUsername,
                            smtpPassword = p.smtpPassword,
                            smtpSender = p.smtpSender,
                            smtpUseSsl = p.smtpUseSsl,
                            smtpUseTls = p.smtpUseTls,
                            googleAccountEmail = p.googleAccountEmail,
                            googleAccountName = p.googleAccountName,
                            googleOAuthClientId = p.googleOAuthClientId,
                            googleOAuthClientSecret = p.googleOAuthClientSecret,
                            googleRefreshToken = p.googleRefreshToken,
                            isShaderBackgroundEnabled = p.isShaderBackgroundEnabled,
                            selectedShaderPreset = p.selectedShaderPreset,
                            customShaderCode = p.customShaderCode,
                            shaderSeed = p.shaderSeed,
                            contactCardOpacity = p.contactCardOpacity
                        )
                    }

                    // Import new shaders in batch to avoid SharedPreferences async write race conditions
                    val shaderPairs = backup.savedShaders.map { Pair(it.name, it.code) }
                    preferences.saveCustomShaders(shaderPairs)

                    for (item in backup.contacts) {
                        val restoredPhotoUri = if (!item.profilePhotoBase64.isNullOrEmpty()) {
                            try {
                                val bytes = Base64.decode(item.profilePhotoBase64, Base64.NO_WRAP)
                                saveBytesToLocalFile(context, bytes, "contact_photos", "profile_restore", "jpg")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error decoding profile photo", e)
                                item.profilePhotoUri
                            }
                        } else {
                            item.profilePhotoUri
                        }

                        val contactId = repository.insertContact(
                            Contact(
                                firstName = item.firstName,
                                lastName = item.lastName,
                                nickname = item.nickname,
                                pronouns = item.pronouns,
                                rating = item.rating,
                                profilePhotoUri = restoredPhotoUri,
                                birthdayInMillis = item.birthdayInMillis,
                                lastInteractionInMillis = item.lastInteractionInMillis,
                                notes = item.profileNotes,
                                tag = item.tag
                            )
                        )

                        for (phone in item.phoneNumbers) {
                            repository.insertPhoneNumber(PhoneNumber(contactId = contactId, number = phone.number, label = phone.label))
                        }
                        for (email in item.emails) {
                            repository.insertEmail(Email(contactId = contactId, email = email.email, label = email.label))
                        }
                        for (act in item.customActions) {
                            val restoredIconPath = if (!act.iconBase64.isNullOrEmpty()) {
                                try {
                                    val bytes = Base64.decode(act.iconBase64, Base64.NO_WRAP)
                                    saveBytesToLocalFile(context, bytes, "social_icons", "icon_restore", "png")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error decoding custom icon", e)
                                    act.iconResName
                                }
                            } else {
                                act.iconResName
                            }

                            repository.insertCustomAction(
                                CustomAction(
                                    contactId = contactId,
                                    label = act.label,
                                    iconResName = restoredIconPath ?: act.iconResName,
                                    actionType = act.actionType,
                                    targetData = act.targetData
                                )
                            )
                        }
                        for (note in item.notes) {
                            repository.insertNote(Note(contactId = contactId, content = note.content, createdAtMillis = note.createdAtMillis, isInteraction = note.isInteraction))
                        }
                        for (adr in item.addresses) {
                            repository.insertAddress(Address(contactId = contactId, label = adr.label, latitude = adr.latitude, longitude = adr.longitude, formattedAddress = adr.formattedAddress))
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sync error in import thread", e)
                }
            }
        }
    }
}
