package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entity.*
import com.example.ui.viewmodel.ContactDetailViewModel
import com.example.util.IntentExecutor
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactDetailScreen(
    contactId: Long, // Pass -1 for My Profile
    viewModel: ContactDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isShaderEnabled: Boolean = false,
    onNavigateToDialer: (String) -> Unit = {},
    onNavigateToSms: (String) -> Unit = {}
) {
    com.example.ui.components.SafeContent {
        ContactDetailScreenContent(
            contactId = contactId,
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            modifier = modifier,
            isShaderEnabled = isShaderEnabled,
            onNavigateToDialer = onNavigateToDialer,
            onNavigateToSms = onNavigateToSms
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactDetailScreenContent(
    contactId: Long, // Pass -1 for My Profile
    viewModel: ContactDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    isShaderEnabled: Boolean = false,
    onNavigateToDialer: (String) -> Unit = {},
    onNavigateToSms: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val preferences = remember { com.example.data.SettingsPreferences(context) }
    var contactCardOpacity by remember { mutableStateOf(preferences.contactCardOpacity) }
    var appLanguage by remember { mutableStateOf(preferences.appLanguage) }

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences("crm_settings", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "contact_card_opacity") {
                contactCardOpacity = prefs.getFloat("contact_card_opacity", 1.0f)
            }
            if (key == "app_language") {
                appLanguage = prefs.getString("app_language", "en") ?: "en"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val contactWithDetails by viewModel.contactWithDetails.collectAsStateWithLifecycle()
    val scheduledMessages by viewModel.scheduledMessages.collectAsStateWithLifecycle()
    val paginatedNotes by viewModel.paginatedNotes.collectAsStateWithLifecycle()
    val isLoadingNotes by viewModel.isLoadingNotes.collectAsStateWithLifecycle()

    // Initialize/Load
    LaunchedEffect(contactId) {
        if (contactId == -1L) {
            viewModel.loadUserProfile()
        } else {
            viewModel.loadContact(contactId)
        }
    }

    // Force Immediate Save on ON_PAUSE (requirement 2.5)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                try {
                    // Move to background thread
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.forceSaveOnPause()
                    }
                } catch (e: Exception) {
                    // Silent fail - don't crash the app
                    Log.e("ContactDetail", "Save failed", e)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Dialog sheets states
    var showAddPhoneDialog by remember { mutableStateOf(false) }
    var showAddEmailDialog by remember { mutableStateOf(false) }
    var showAddActionDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleType by remember { mutableStateOf("SMS") } // "SMS" or "EMAIL"

    // Uri Image Launcher (requirement 2.1 photo URI picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.setProfilePhoto(context, uri)
            Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    // Social Media Hub image picker and state dialog triggers
    var showLinkEditingDialog by remember { mutableStateOf(false) }
    var showSocialHelpDialog by remember { mutableStateOf(false) }
    var showSocialHowToFindDialog by remember { mutableStateOf(false) }
    var showSocialIconSelectionDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var activeLinkEditingAction by remember { mutableStateOf<CustomAction?>(null) }
    var linkInputText by remember { mutableStateOf("") }

    val socialImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val resizedPath = saveUriAsResizedIcon(context, uri)
            if (resizedPath != null) {
                viewModel.addCustomAction("Social Icon", resizedPath, "SOCIAL_MEDIA", "")
                Toast.makeText(context, "Icon processed successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val smsGranted = results[Manifest.permission.SEND_SMS] ?: (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)
        if (smsGranted) {
            scheduleType = "SMS"
            showScheduleDialog = true
        } else {
            Toast.makeText(context, "Permission SEND_SMS is required to send scheduled SMS messages.", Toast.LENGTH_LONG).show()
        }
    }

    val contactDetails = contactWithDetails
    if (contactDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val contact = contactDetails.contact

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isShaderEnabled) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (contact.isUserProfile) {
                            if (appLanguage == "de") "Mein Profil" else "My Profile Card"
                        } else {
                            if (appLanguage == "de") "Kontaktdetails" else "Contact Detail"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.forceSaveOnPause()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        },
        bottomBar = {
            // Requirement 2.2 quick bottom utilities action row
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val primaryPhone = contactDetails.phoneNumbers.firstOrNull()?.number ?: ""
                    val primaryEmail = contactDetails.emails.firstOrNull()?.email ?: ""
                    val primaryAddress = contactDetails.addresses.firstOrNull()?.formattedAddress ?: ""

                    Button(
                        onClick = {
                            if (primaryPhone.isNotEmpty()) {
                                onNavigateToDialer(primaryPhone)
                            } else {
                                Toast.makeText(context, if (appLanguage == "de") "Keine Telefonnummer verfügbar" else "No phone number available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        enabled = primaryPhone.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (appLanguage == "de") "Anrufen" else "Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (primaryPhone.isNotEmpty()) {
                                onNavigateToSms(primaryPhone)
                            } else {
                                Toast.makeText(context, if (appLanguage == "de") "Keine Nachrichtendetails hinterlegt" else "No contacts details logged for messages", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        enabled = primaryPhone.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (appLanguage == "de") "Nachricht" else "Message", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (primaryAddress.isNotEmpty()) {
                                IntentExecutor.openDirections(context, primaryAddress)
                            } else {
                                Toast.makeText(context, if (appLanguage == "de") "Keine Adresse für Routenführung konfiguriert" else "No address configured for mapping direction", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        enabled = primaryAddress.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (appLanguage == "de") "Route" else "Route", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = {
                            IntentExecutor.shareProfileLink(context, contact, primaryPhone, primaryEmail)
                        },
                        modifier = Modifier.testTag("action_share_profile")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Profile Link representation")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

            // Profile Photo layout (requirement 2.1 display and tap to select)
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .clickable { photoPickerLauncher.launch("image/*") }
                    .testTag("contact_photo_picker_wrapper"),
                contentAlignment = Alignment.Center
            ) {
                if (contact.profilePhotoUri != null) {
                    val safePhotoModel = com.example.util.ImageUtils.rememberProfilePhotoState(contact.profilePhotoUri)
                    AsyncImage(
                        model = safePhotoModel,
                        contentDescription = "Profile avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Upload photo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (contact.profilePhotoUri == null) {
                    if (appLanguage == "de") "Tippen, um Profilbild hochzuladen" else "Tap to upload profile photo"
                } else {
                    if (appLanguage == "de") "Foto ändern" else "Change Photo"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Text inputs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // First Name Input
                OutlinedTextField(
                    value = contact.firstName,
                    onValueChange = { newVal ->
                        viewModel.updateField { it.copy(firstName = newVal) }
                    },
                    label = { Text(if (appLanguage == "de") "Vorname" else "First Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = contactCardOpacity)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_first_name")
                )

                // Last Name Input
                OutlinedTextField(
                    value = contact.lastName,
                    onValueChange = { newVal ->
                        viewModel.updateField { it.copy(lastName = newVal) }
                    },
                    label = { Text(if (appLanguage == "de") "Nachname" else "Last Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = contactCardOpacity)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_last_name")
                )

                // Nickname Input
                OutlinedTextField(
                    value = contact.nickname ?: "",
                    onValueChange = { newVal ->
                        viewModel.updateField { it.copy(nickname = newVal.ifBlank { null }) }
                    },
                    label = { Text(if (appLanguage == "de") "Spitzname" else "Nickname") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = contactCardOpacity)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_nickname")
                )

                // Pronouns Input
                OutlinedTextField(
                    value = contact.pronouns ?: "",
                    onValueChange = { newVal ->
                        viewModel.updateField { it.copy(pronouns = newVal.ifBlank { null }) }
                    },
                    label = { Text(if (appLanguage == "de") "Pronomen" else "Pronouns") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = contactCardOpacity)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_pronouns")
                )

                // Profile Notes Input
                OutlinedTextField(
                    value = contact.notes ?: "",
                    onValueChange = { newVal ->
                        viewModel.updateField { it.copy(notes = newVal.ifBlank { null }) }
                    },
                    label = { Text(if (appLanguage == "de") "Profil-Notizen" else "Profile Notes") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = contactCardOpacity)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_notes")
                )

                // Birth Date picker (with calendar interactions)
                val dateStr = if (contact.birthdayInMillis != null) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(contact.birthdayInMillis))
                } else {
                    if (appLanguage == "de") "Kein Geburtstag konfiguriert" else "No Birthday Configured"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(if (appLanguage == "de") "Geburtstag" else "Birthday", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(dateStr, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Row {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance()
                            if (contact.birthdayInMillis != null) {
                                calendar.timeInMillis = contact.birthdayInMillis
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selectedCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    viewModel.updateField { it.copy(birthdayInMillis = selectedCal.timeInMillis) }
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick calendar date", tint = MaterialTheme.colorScheme.primary)
                        }

                        if (contact.birthdayInMillis != null) {
                            IconButton(onClick = {
                                IntentExecutor.addCalendarBirthdayReminder(context, contact)
                            }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Add Calendar reminder", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Rating bar slider (1-10 slider - Auto-saves)
                Column {
                    Text(
                        text = if (appLanguage == "de") "Beziehungsstärke: ${contact.rating}/10" else "Relationship Strength Rating: ${contact.rating}/10",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = contact.rating.toFloat(),
                        onValueChange = { newVal ->
                            viewModel.updateField { it.copy(rating = newVal.toInt().coerceIn(0, 10)) }
                        },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("rating_strength_slider")
                    )
                }

                // Contact Tags row
                Column {
                    Text(
                        text = if (appLanguage == "de") "Kontakt-Kategorie" else "Contact Tag",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_tag_lazy_row")
                    ) {
                        val tags = listOf("Work", "Family", "Friends", "Favorites", "Business", "Acquaintance")
                        items(tags) { tagOpt ->
                            val selected = contact.tag == tagOpt
                            val displayTag = if (appLanguage == "de") {
                                when (tagOpt) {
                                    "Work" -> "Arbeit"
                                    "Family" -> "Familie"
                                    "Friends" -> "Freunde"
                                    "Favorites" -> "Favoriten"
                                    "Business" -> "Geschäft"
                                    "Acquaintance" -> "Bekannte"
                                    else -> tagOpt
                                }
                            } else tagOpt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.updateField { it.copy(tag = if (selected) null else tagOpt) }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayTag,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Social Media Hub (replaces Modular custom Action ribbon)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "de") "Social-Media-Schnittstelle" else "Social Media Hub",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showSocialIconSelectionDialog = true },
                        modifier = Modifier.size(32.dp).testTag("add_social_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add social icon Selection",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (contactDetails.customActions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (appLanguage == "de") {
                                "Keine Icons in dieser Kontaktschnittstelle gefunden. Tippen Sie auf '+', um Bilder zuzuschneiden und als Aktionssymbole zu speichern!"
                            } else {
                                "No icons found in this contact's Hub. Tap '+' to crop and save any picture as a custom action icon!"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val maxPerRowDetail = 5
                    if (contactDetails.customActions.size <= maxPerRowDetail) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(contactDetails.customActions, key = { action -> action.actionId }) { action ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (action.targetData.isBlank()) {
                                                    activeLinkEditingAction = action
                                                    linkInputText = ""
                                                    showLinkEditingDialog = true
                                                } else {
                                                    try {
                                                        var url = action.targetData.trim()
                                                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                            url = "https://$url"
                                                        }
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Invalid link format: ${action.targetData}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                activeLinkEditingAction = action
                                                linkInputText = action.targetData
                                                showOptionsDialog = true
                                            }
                                        )
                                        .testTag("social_icon_${action.actionId}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!action.iconResName.isNullOrBlank()) {
                                            AsyncImage(
                                                model = action.iconResName,
                                                contentDescription = action.label,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = "Fallback link",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (action.targetData.isBlank()) "Set Link" else {
                                            val display = action.targetData.replace("https://", "").replace("http://", "").replace("www.", "")
                                            if (display.length > 8) display.take(7) + ".." else display
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    } else {
                        val rowBelowCount = maxPerRowDetail
                        val rowAboveCount = contactDetails.customActions.size - rowBelowCount
                        val rowAboveActions = contactDetails.customActions.take(rowAboveCount)
                        val rowBelowActions = contactDetails.customActions.takeLast(rowBelowCount)
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(rowAboveActions, key = { action -> action.actionId }) { action ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    if (action.targetData.isBlank()) {
                                                        activeLinkEditingAction = action
                                                        linkInputText = ""
                                                        showLinkEditingDialog = true
                                                    } else {
                                                        try {
                                                            var url = action.targetData.trim()
                                                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                                url = "https://$url"
                                                            }
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Invalid link format: ${action.targetData}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    activeLinkEditingAction = action
                                                    linkInputText = action.targetData
                                                    showOptionsDialog = true
                                                }
                                            )
                                            .testTag("social_icon_${action.actionId}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!action.iconResName.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = action.iconResName,
                                                    contentDescription = action.label,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = "Fallback link",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (action.targetData.isBlank()) "Set Link" else {
                                                val display = action.targetData.replace("https://", "").replace("http://", "").replace("www.", "")
                                                if (display.length > 8) display.take(7) + ".." else display
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(rowBelowActions, key = { action -> action.actionId }) { action ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    if (action.targetData.isBlank()) {
                                                        activeLinkEditingAction = action
                                                        linkInputText = ""
                                                        showLinkEditingDialog = true
                                                    } else {
                                                        try {
                                                            var url = action.targetData.trim()
                                                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                                url = "https://$url"
                                                            }
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Invalid link format: ${action.targetData}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    activeLinkEditingAction = action
                                                    linkInputText = action.targetData
                                                    showOptionsDialog = true
                                                }
                                            )
                                            .testTag("social_icon_${action.actionId}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!action.iconResName.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = action.iconResName,
                                                    contentDescription = action.label,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = "Fallback link",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (action.targetData.isBlank()) "Set Link" else {
                                                val display = action.targetData.replace("https://", "").replace("http://", "").replace("www.", "")
                                                if (display.length > 8) display.take(7) + ".." else display
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subsections collapsible UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Phones & Emails
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Phone & Email", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row {
                                IconButton(onClick = { showAddPhoneDialog = true }) {
                                    Icon(Icons.Default.Call, contentDescription = "Add phone number")
                                }
                                IconButton(onClick = { showAddEmailDialog = true }) {
                                    Icon(Icons.Default.Email, contentDescription = "Add email address")
                                }
                            }
                        }

                        // Display list of numbers
                        for (phone in contactDetails.phoneNumbers) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(phone.number, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(phone.label.uppercase(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { viewModel.deletePhoneNumber(phone.id) }) {
                                    Icon(Icons.Default.RemoveCircle, contentDescription = "Delete entry", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Display list of emails
                        for (email in contactDetails.emails) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(email.email, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(email.label.uppercase(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                IconButton(onClick = { viewModel.deleteEmail(email.id) }) {
                                    Icon(Icons.Default.RemoveCircle, contentDescription = "Delete entry", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Section: Social Media Hub Configuration
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Manage Social Icons", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            IconButton(
                                onClick = { showSocialIconSelectionDialog = true },
                                modifier = Modifier.testTag("manage_add_social_icon_button")
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Add social icon image")
                            }
                        }

                        if (contactDetails.customActions.isEmpty()) {
                            Text(
                                text = "No social icons added yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            for (action in contactDetails.customActions) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable {
                                            activeLinkEditingAction = action
                                            linkInputText = action.targetData
                                            showLinkEditingDialog = true
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!action.iconResName.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = action.iconResName,
                                                    contentDescription = "Social icon preview",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = "Link fallback",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = if (action.targetData.isBlank()) "Setup Hyperlink" else action.targetData,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 200.dp)
                                            )
                                            Text(
                                                text = "Tap to edit hyperlink",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    IconButton(onClick = { viewModel.deleteCustomAction(action.actionId) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete social icon",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Addresses list (maps deep linking)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Addresses & Navigation", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            IconButton(onClick = { showAddAddressDialog = true }) {
                                Icon(Icons.Default.AddLocationAlt, contentDescription = "Add location address")
                            }
                        }

                        for (adr in contactDetails.addresses) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { IntentExecutor.openDirections(context, adr.formattedAddress) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(adr.formattedAddress, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${adr.label.uppercase()} | Lat: ${adr.latitude}, Lng: ${adr.longitude}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteAddress(adr.addressId) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete address", tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Section: Scheduled SMS & Emails
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scheduled_messages_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Scheduled SMS/Emails", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row {
                                FilledTonalButton(
                                    onClick = {
                                        if (contactDetails.phoneNumbers.isEmpty()) {
                                            Toast.makeText(context, "Please add a phone number first", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val smsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                                            if (smsGranted) {
                                                scheduleType = "SMS"
                                                showScheduleDialog = true
                                            } else {
                                                val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.POST_NOTIFICATIONS)
                                                } else {
                                                    arrayOf(Manifest.permission.SEND_SMS)
                                                }
                                                smsPermissionLauncher.launch(permissionsToRequest)
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("btn_schedule_sms")
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                FilledTonalButton(
                                    onClick = {
                                        if (contactDetails.emails.isEmpty()) {
                                            Toast.makeText(context, "Please add an email address first", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scheduleType = "EMAIL"
                                            showScheduleDialog = true
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("btn_schedule_email")
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Email", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (scheduledMessages.isEmpty()) {
                            Text(
                                "No active scheduled tasks.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            for (msg in scheduledMessages) {
                                val dateTimeStr = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date(msg.scheduleTimeMillis))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (msg.status) {
                                                "SENT" -> Color.Green.copy(alpha = 0.08f)
                                                "FAILED" -> Color.Red.copy(alpha = 0.08f)
                                                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                            }
                                        )
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (msg.type.run { equals("SMS", ignoreCase = true) }) Icons.Default.Sms else Icons.Default.Email,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${msg.type} to ${msg.recipientValue}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "\"${msg.messageContent}\"",
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Delivery: $dateTimeStr • Recur: ${msg.recurrence}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        if (!msg.errorMessage.isNullOrBlank()) {
                                            Text(
                                                text = "Error: ${msg.errorMessage}",
                                                fontSize = 10.sp,
                                                color = Color.Red
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (msg.status) {
                                                        "SENT" -> Color.Green.copy(alpha = 0.2f)
                                                        "FAILED" -> Color.Red.copy(alpha = 0.2f)
                                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    }
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(msg.status, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))

                                        IconButton(
                                            onClick = { viewModel.cancelScheduledMessage(context, msg.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Cancel schedule",
                                                tint = Color.Red.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Personal Horoscope
                val rawHoroscope = remember(contact.birthdayInMillis) {
                    com.example.util.HoroscopeHelper.getHoroscope(contact.birthdayInMillis)
                }
                val horoscopeInfo = remember(rawHoroscope, appLanguage) {
                    if (rawHoroscope != null) {
                        com.example.util.Localization.translateHoroscope(rawHoroscope, appLanguage)
                    } else null
                }
                if (horoscopeInfo != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("personal_horoscope_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (appLanguage == "de") "Persönliches Horoskop" else "Personal Horoscope",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${horoscopeInfo.name} (${horoscopeInfo.dates})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val bulletPoints = listOf(
                                com.example.util.Localization.getString("element", appLanguage) to horoscopeInfo.elementModality,
                                com.example.util.Localization.getString("ruler", appLanguage) to horoscopeInfo.ruler,
                                com.example.util.Localization.getString("drive", appLanguage) to horoscopeInfo.coreDrive,
                                com.example.util.Localization.getString("fear", appLanguage) to horoscopeInfo.deepestFear,
                                com.example.util.Localization.getString("love", appLanguage) to horoscopeInfo.loveLanguage,
                                com.example.util.Localization.getString("money", appLanguage) to horoscopeInfo.moneyStyle,
                                com.example.util.Localization.getString("shadow", appLanguage) to horoscopeInfo.shadow,
                                com.example.util.Localization.getString("sex", appLanguage) to horoscopeInfo.sexualSignature
                            )
                            
                            bulletPoints.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "•",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = value,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Timeline & Notes (auto interactions) header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Timeline & Memory Log", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = { showAddNoteDialog = true }) {
                        Icon(Icons.Default.PostAdd, contentDescription = "Write a free form note")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Define the items for notes / timeline paginated
            if (paginatedNotes.isEmpty() && !isLoadingNotes) {
                item {
                    Text(
                        text = "No memory logs captured yet for this contact.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(
                    items = paginatedNotes,
                    key = { note -> note.noteId }
                ) { note ->
                    val timeStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(note.createdAtMillis))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (note.isInteraction) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                            )
                            .border(
                                1.dp,
                                if (note.isInteraction) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (note.isInteraction) {
                                        Icon(Icons.Default.RingVolume, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("System Connection Log", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Free Note", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                                Text(timeStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(note.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                IconButton(onClick = { viewModel.deleteNote(note.noteId) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Delete note", tint = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom loading trigger element
            item {
                LaunchedEffect(paginatedNotes.size) {
                    viewModel.loadNextNotesPage()
                }

                if (isLoadingNotes) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Modal dialogs block

    if (showAddPhoneDialog) {
        var tempNum by remember { mutableStateOf("") }
        var tempLabel by remember { mutableStateOf("mobile") }
        AlertDialog(
            onDismissRequest = { showAddPhoneDialog = false },
            title = { Text("Add Phone Number") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempNum,
                        onValueChange = { tempNum = it },
                        placeholder = { Text("+1 (555) 019-3829") },
                        label = { Text("Phone Number") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempLabel,
                        onValueChange = { tempLabel = it },
                        placeholder = { Text("mobile, work, home, etc.") },
                        label = { Text("Label") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempNum.isNotEmpty()) {
                            viewModel.addPhoneNumber(tempNum, tempLabel)
                            showAddPhoneDialog = false
                        }
                    }
                ) { Text("Save Phone") }
            },
            dismissButton = {
                TextButton(onClick = { showAddPhoneDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddEmailDialog) {
        var tempMail by remember { mutableStateOf("") }
        var tempLabel by remember { mutableStateOf("personal") }
        AlertDialog(
            onDismissRequest = { showAddEmailDialog = false },
            title = { Text("Add Email Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempMail,
                        onValueChange = { tempMail = it },
                        placeholder = { Text("friend@example.com") },
                        label = { Text("Email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempLabel,
                        onValueChange = { tempLabel = it },
                        placeholder = { Text("personal, work, study...") },
                        label = { Text("Label") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempMail.isNotEmpty()) {
                            viewModel.addEmail(tempMail, tempLabel)
                            showAddEmailDialog = false
                        }
                    }
                ) { Text("Save Email") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmailDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSocialHelpDialog) {
        SocialHelpDialog(
            onDismissRequest = { showSocialHelpDialog = false }
        )
    }

    if (showSocialHowToFindDialog) {
        SocialHowToFindDialog(
            onDismissRequest = { showSocialHowToFindDialog = false }
        )
    }

    if (showSocialIconSelectionDialog) {
        SocialIconSelectionDialog(
            onDismissRequest = { showSocialIconSelectionDialog = false },
            onPresetSelected = { preset ->
                viewModel.addCustomAction(
                    label = preset.name,
                    iconResName = preset.iconUrl,
                    actionType = "SOCIAL_MEDIA",
                    targetData = preset.defaultUrlPrefix
                )
                showSocialIconSelectionDialog = false
                Toast.makeText(context, "${preset.name} preset icon added!", Toast.LENGTH_SHORT).show()
            },
            onSelectFromFileClicked = {
                socialImagePickerLauncher.launch("image/*")
            }
        )
    }

    if (showLinkEditingDialog && activeLinkEditingAction != null) {
        var tempLink by remember(activeLinkEditingAction) { mutableStateOf(activeLinkEditingAction?.targetData ?: "") }

        AlertDialog(
            onDismissRequest = { showLinkEditingDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Configure Social Link")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showSocialHowToFindDialog = true },
                            modifier = Modifier.testTag("social_howto_find_button_details")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "How to find profile links instructions",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = { showSocialHelpDialog = true },
                            modifier = Modifier.testTag("social_help_button_details")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Help,
                                contentDescription = "Social link format instructions",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a web link, hyperlink, http, or https URL:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = tempLink,
                        onValueChange = { tempLink = it },
                        placeholder = { Text("e.g., github.com/user") },
                        label = { Text("URL / Link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("social_link_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentAction = activeLinkEditingAction
                        if (currentAction != null) {
                            val updatedAction = currentAction.copy(targetData = tempLink)
                            viewModel.updateCustomAction(updatedAction)
                            Toast.makeText(context, "Link updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                        showLinkEditingDialog = false
                    },
                    modifier = Modifier.testTag("submit_social_link_button")
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkEditingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showOptionsDialog && activeLinkEditingAction != null) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Icon Shortcut Options") },
            text = {
                Text("What would you like to do with this social media icon?", fontSize = 14.sp)
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        onClick = {
                            activeLinkEditingAction?.let {
                                viewModel.deleteCustomAction(it.actionId)
                            }
                            showOptionsDialog = false
                        }
                    ) {
                        Text("Delete Icon")
                    }
                    TextButton(
                        onClick = {
                            showOptionsDialog = false
                            showLinkEditingDialog = true
                        }
                    ) {
                        Text("Edit Link")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddNoteDialog) {
        var tempContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Write Memory Log Entry") },
            text = {
                OutlinedTextField(
                    value = tempContent,
                    onValueChange = { tempContent = it },
                    placeholder = { Text("Met for coffee, discussed project updates...") },
                    label = { Text("Timeline Note") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempContent.isNotEmpty()) {
                            viewModel.addNote(tempContent, isInteraction = false)
                            showAddNoteDialog = false
                        }
                    }
                ) { Text("Commit Note") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddAddressDialog) {
        var tempLabel by remember { mutableStateOf("home") }
        var tempAddrString by remember { mutableStateOf("") }
        var tempLat by remember { mutableStateOf("0.0") }
        var tempLng by remember { mutableStateOf("0.0") }

        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            title = { Text("Add Physical Address Location") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = tempLabel,
                        onValueChange = { tempLabel = it },
                        placeholder = { Text("home, office, campus...") },
                        label = { Text("Address Label") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tempAddrString,
                        onValueChange = { tempAddrString = it },
                        placeholder = { Text("1600 Amphitheatre Pkwy, Mountain View, CA") },
                        label = { Text("Formatted Street Address") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempLat,
                            onValueChange = { tempLat = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = tempLng,
                            onValueChange = { tempLng = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempAddrString.isNotEmpty()) {
                            val latVal = tempLat.toDoubleOrNull() ?: 0.0
                            val lngVal = tempLng.toDoubleOrNull() ?: 0.0
                            viewModel.addAddress(tempLabel, latVal, lngVal, tempAddrString)
                            showAddAddressDialog = false
                        }
                    }
                ) { Text("Save Location") }
            },
            dismissButton = {
                TextButton(onClick = { showAddAddressDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showScheduleDialog) {
        var scheduleMessageText by remember { mutableStateOf("") }
        var selectedRecurrence by remember { mutableStateOf("ONCE") }

        val options = if (scheduleType == "SMS") {
            contactDetails.phoneNumbers.map { it.number }
        } else {
            contactDetails.emails.map { it.email }
        }

        var selectedRecipientValue by remember { mutableStateOf(options.firstOrNull() ?: "") }

        val tempCal = remember { Calendar.getInstance().apply { add(Calendar.MINUTE, 5) } }
        var tempTimeMillis by remember { mutableStateOf(tempCal.timeInMillis) }

        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (scheduleType == "SMS") Icons.Default.Sms else Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule $scheduleType")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (options.size > 1) {
                        var expandedDropdown by remember { mutableStateOf(false) }
                        Box {
                            OutlinedTextField(
                                value = selectedRecipientValue,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Recipient Value") },
                                trailingIcon = {
                                    IconButton(onClick = { expandedDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                options.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text(value) },
                                        onClick = {
                                            selectedRecipientValue = value
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = selectedRecipientValue,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Send To") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = scheduleMessageText,
                        onValueChange = { scheduleMessageText = it },
                        label = { Text("Message Body") },
                        placeholder = { Text("Write your text here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("schedule_msg_textarea"),
                        maxLines = 5
                    )

                    Text("Scheduled Send Time:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tempTimeMillis))
                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tempTimeMillis))

                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = tempTimeMillis }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val newCal = Calendar.getInstance().apply {
                                            timeInMillis = tempTimeMillis
                                            set(Calendar.YEAR, y)
                                            set(Calendar.MONTH, m)
                                            set(Calendar.DAY_OF_MONTH, d)
                                        }
                                        tempTimeMillis = newCal.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f).testTag("btn_pick_date")
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dateStr, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = tempTimeMillis }
                                android.app.TimePickerDialog(
                                    context,
                                    { _, h, min ->
                                        val newCal = Calendar.getInstance().apply {
                                            timeInMillis = tempTimeMillis
                                            set(Calendar.HOUR_OF_DAY, h)
                                            set(Calendar.MINUTE, min)
                                            set(Calendar.SECOND, 0)
                                        }
                                        tempTimeMillis = newCal.timeInMillis
                                    },
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            modifier = Modifier.weight(1f).testTag("btn_pick_time")
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(timeStr, fontSize = 11.sp)
                        }
                    }

                    var recurrenceExpanded by remember { mutableStateOf(false) }
                    val recurrenceOptions = listOf(
                        "ONCE" to "One-time Only",
                        "ONE_HOUR" to "Delay exactly 1 hour",
                        "DAILY" to "Repeat Daily",
                        "WEEKLY" to "Repeat Weekly",
                        "MONTHLY" to "Repeat Monthly",
                        "BIRTHDAY" to "Annual Birthday Text",
                        "FOLLOW_UP" to "Follow-up in 3 days"
                    )
                    val currentRecurrenceLabel = recurrenceOptions.firstOrNull { it.first == selectedRecurrence }?.second ?: selectedRecurrence

                    Box {
                        OutlinedTextField(
                            value = currentRecurrenceLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recurrence Plan") },
                            trailingIcon = {
                                IconButton(onClick = { recurrenceExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("schedule_recurrence_input")
                        )
                        DropdownMenu(
                            expanded = recurrenceExpanded,
                            onDismissRequest = { recurrenceExpanded = false }
                        ) {
                            recurrenceOptions.forEach { pair ->
                                DropdownMenuItem(
                                    text = { Text(pair.second) },
                                    onClick = {
                                        selectedRecurrence = pair.first
                                        recurrenceExpanded = false
                                        if (pair.first == "ONE_HOUR") {
                                            tempTimeMillis = System.currentTimeMillis() + 3600_000L
                                        } else if (pair.first == "FOLLOW_UP") {
                                            tempTimeMillis = System.currentTimeMillis() + (3 * 24 * 3600_000L)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (scheduleMessageText.isBlank()) {
                            Toast.makeText(context, "Please enter some message body", Toast.LENGTH_SHORT).show()
                        } else if (selectedRecipientValue.isBlank()) {
                            Toast.makeText(context, "Recipient details cannot be empty", Toast.LENGTH_SHORT).show()
                        } else if (tempTimeMillis <= System.currentTimeMillis()) {
                            Toast.makeText(context, "Please select a date/time in the future", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.scheduleMessage(
                                context = context,
                                recipientName = "${contact.firstName} ${contact.lastName}".trim().ifEmpty { "My Profile" },
                                type = scheduleType,
                                recipientValue = selectedRecipientValue,
                                messageContent = scheduleMessageText,
                                scheduleTimeMillis = tempTimeMillis,
                                recurrence = selectedRecurrence
                            )
                            Toast.makeText(context, "Message scheduled successfully!", Toast.LENGTH_SHORT).show()
                            showScheduleDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_schedule_btn")
                ) {
                    Text("Save Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun saveUriAsResizedIcon(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()

        val size = 120
        val croppedBitmap = if (originalBitmap.width >= originalBitmap.height) {
            Bitmap.createBitmap(
                originalBitmap,
                originalBitmap.width / 2 - originalBitmap.height / 2,
                0,
                originalBitmap.height,
                originalBitmap.height
            )
        } else {
            Bitmap.createBitmap(
                originalBitmap,
                0,
                originalBitmap.height / 2 - originalBitmap.width / 2,
                originalBitmap.width,
                originalBitmap.width
            )
        }
        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, size, size, true)

        // Recycle original and cropped bitmaps after scaling
        originalBitmap.recycle()
        if (croppedBitmap != originalBitmap) croppedBitmap.recycle()

        val iconsDir = java.io.File(context.filesDir, "social_icons")
        if (!iconsDir.exists()) {
            iconsDir.mkdirs()
        }
        val file = java.io.File(iconsDir, "icon_${System.currentTimeMillis()}.png")
        val outputStream = java.io.FileOutputStream(file)
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        
        // Recycle scaled bitmap after saving
        scaledBitmap.recycle()

        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
