package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.ContactWithListDetails
import com.example.ui.viewmodel.ContactListViewModel
import com.example.util.IntentExecutor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(
    viewModel: ContactListViewModel,
    onContactSelect: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.example.ui.components.SafeContent {
        ContactListScreenContent(
            viewModel = viewModel,
            onContactSelect = onContactSelect,
            onNavigateToSettings = onNavigateToSettings,
            onAddContact = onAddContact,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreenContent(
    viewModel: ContactListViewModel,
    onContactSelect: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val isDecoy by viewModel.isDecoyActive.collectAsStateWithLifecycle()

    val isCustomThemeEnabled by viewModel.isCustomThemeEnabled.collectAsStateWithLifecycle()
    val customDeleteColor by viewModel.customDeleteColor.collectAsStateWithLifecycle()
    val isShaderEnabled by viewModel.isShaderBackgroundEnabled.collectAsStateWithLifecycle()
    val contactCardOpacity by viewModel.contactCardOpacity.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    var isRelationshipModeActive by remember { mutableStateOf(false) }
    var showRelationshipInstructionDialog by remember { mutableStateOf(false) }
    var firstSelectedContactForRelationship by remember { mutableStateOf<com.example.data.entity.ContactWithListDetails?>(null) }
    var secondSelectedContactForRelationship by remember { mutableStateOf<com.example.data.entity.ContactWithListDetails?>(null) }
    var showRelationshipResultScreen by remember { mutableStateOf(false) }

    val activeContacts = remember(contacts, isRelationshipModeActive) {
        if (isRelationshipModeActive) {
            contacts.filter { it.contact.birthdayInMillis != null }
        } else {
            contacts
        }
    }

    var showUserQuickProfile by remember { mutableStateOf(false) }
    var showImportMenu by remember { mutableStateOf(false) }

    // Dialog state for updating social media action link directly on first page
    var showListLinkEditingDialog by remember { mutableStateOf(false) }
    var showListSocialHelpDialog by remember { mutableStateOf(false) }
    var showListSocialHowToFindDialog by remember { mutableStateOf(false) }
    var activeListLinkEditingAction by remember { mutableStateOf<com.example.data.entity.CustomAction?>(null) }
    var emailsSelectionList by remember { mutableStateOf<List<com.example.data.entity.Email>?>(null) }

    var showBulkImportDialog by remember { mutableStateOf(false) }
    var loadedPhoneContacts by remember { mutableStateOf<List<com.example.util.PhoneContact>>(emptyList()) }
    var isLoadingPhoneContacts by remember { mutableStateOf(false) }

    var isFabVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastInteractionTime) {
        delay(4000) // disappears after 4 seconds of no touch
        isFabVisible = false
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { contactUri ->
            coroutineScope.launch {
                try {
                    val contactWithDetails = com.example.util.PhoneContactImporter.loadContactFromUri(context, contactUri)
                    if (contactWithDetails != null) {
                        viewModel.importContactWithDetails(context, contactWithDetails) {
                            Toast.makeText(
                                context,
                                "Imported: ${contactWithDetails.contact.firstName} ${contactWithDetails.contact.lastName}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to resolve selected contact.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error importing contact: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val triggerBulkImportLoad = {
        isLoadingPhoneContacts = true
        showBulkImportDialog = true
        coroutineScope.launch {
            try {
                val list = com.example.util.PhoneContactImporter.fetchAllPhoneContacts(context)
                loadedPhoneContacts = list
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading contacts: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isLoadingPhoneContacts = false
            }
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerBulkImportLoad()
        } else {
            Toast.makeText(context, "READ_CONTACTS permission is required to list system contacts.", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInteractionTime = System.currentTimeMillis()
                        isFabVisible = true
                    }
                }
            },
        containerColor = if (isShaderEnabled) Color.Transparent else MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isDecoy) "Omni Connect (Decoy)" else "Omni Connect",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (isDecoy) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(containerColor = Color(0xFFFF9800)) {
                                Text("Decoy Mode", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        FilledTonalButton(
                            onClick = {
                                if (isRelationshipModeActive) {
                                    isRelationshipModeActive = false
                                    firstSelectedContactForRelationship = null
                                    secondSelectedContactForRelationship = null
                                } else {
                                    showRelationshipInstructionDialog = true
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isRelationshipModeActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("relationship_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isRelationshipModeActive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Relationship Mode",
                                modifier = Modifier.size(14.dp),
                                tint = if (isRelationshipModeActive) Color(0xFFFF2E93) else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Relationship",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRelationshipModeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showImportMenu = true },
                            modifier = Modifier.testTag("import_contacts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Import Contacts Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = showImportMenu,
                            onDismissRequest = { showImportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Pick Single Contact") },
                                leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                                onClick = {
                                    showImportMenu = false
                                    contactPickerLauncher.launch(null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bulk Import (Checklist)") },
                                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                onClick = {
                                    showImportMenu = false
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.READ_CONTACTS
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        triggerBulkImportLoad()
                                    } else {
                                        contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                    }
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = { showUserQuickProfile = !showUserQuickProfile },
                        modifier = Modifier.testTag("user_profile_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "My Profile Hub",
                            tint = if (showUserQuickProfile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("settings_nav_button")) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings View")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = onAddContact,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("add_contact_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Personal Contact")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Fuzzy Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(com.example.util.Localization.getString("search_hint", currentLanguage)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Fuzzy search query") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear fuzzy input")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("fuzzy_search_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Tag Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .testTag("tag_filter_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        text = com.example.util.Localization.getString("tag_filter", currentLanguage),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                val tags = listOf("Work", "Family", "Friends", "Favorites", "Business", "Acquaintance")
                items(tags) { tagOpt ->
                    val isSelected = selectedTag == tagOpt
                    val displayTag = when(tagOpt) {
                        "Work" -> if(currentLanguage == "de") "Arbeit" else "Work"
                        "Family" -> if(currentLanguage == "de") "Familie" else "Family"
                        "Friends" -> if(currentLanguage == "de") "Freunde" else "Friends"
                        "Favorites" -> if(currentLanguage == "de") "Favoriten" else "Favorites"
                        "Business" -> if(currentLanguage == "de") "Geschäftlich" else "Business"
                        "Acquaintance" -> if(currentLanguage == "de") "Bekannte" else "Acquaintance"
                        else -> tagOpt
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) viewModel.filterByTag(null)
                            else viewModel.filterByTag(tagOpt)
                        },
                        label = { Text(displayTag) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Sort order selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = com.example.util.Localization.getString("sort_by", currentLanguage),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                val sortingOptions = listOf(
                    Triple(com.example.util.Localization.getString("rating", currentLanguage), com.example.ui.viewmodel.ContactSortOrder.RATING_DESC, Icons.Default.Star),
                    Triple(com.example.util.Localization.getString("tag", currentLanguage), com.example.ui.viewmodel.ContactSortOrder.TAG_ASC, Icons.AutoMirrored.Filled.Label),
                    Triple(com.example.util.Localization.getString("name", currentLanguage), com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_ASC, Icons.Default.SortByAlpha)
                )

                for ((label, optOrder, icon) in sortingOptions) {
                    val isSorted = if (optOrder == com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_ASC) {
                        sortOrder == com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_ASC ||
                                sortOrder == com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_DESC
                    } else {
                        sortOrder == optOrder
                    }

                    val chipLabel = if (optOrder == com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_ASC) {
                        if (sortOrder == com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_DESC) {
                            if (currentLanguage == "de") "Name Z-A" else "Name Z-A"
                        } else if (sortOrder == com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_ASC) {
                            if (currentLanguage == "de") "Name A-Z" else "Name A-Z"
                        } else {
                            label
                        }
                    } else {
                        label
                    }

                    InputChip(
                        selected = isSorted,
                        onClick = {
                            if (optOrder == com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_ASC) {
                                if (sortOrder == com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_ASC) {
                                    viewModel.setSortOrder(com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_DESC)
                                } else {
                                    viewModel.setSortOrder(com.example.ui.viewmodel.ContactSortOrder.FIRST_NAME_ASC)
                                }
                            } else {
                                viewModel.setSortOrder(optOrder)
                            }
                        },
                        label = { Text(chipLabel, fontSize = 11.sp) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Active Relationship Selection Mode banner
            AnimatedVisibility(
                visible = isRelationshipModeActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("relationship_selection_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Selection Mode",
                            tint = Color(0xFFFF2E93),
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Relationship Blueprint Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val selectionText = firstSelectedContactForRelationship?.let {
                                "Tap 2nd profile to test with ${it.contact.firstName}"
                            } ?: "Tap 1st profile to start comparison"
                            Text(
                                text = selectionText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = {
                                isRelationshipModeActive = false
                                firstSelectedContactForRelationship = null
                                secondSelectedContactForRelationship = null
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close mode",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Expandable Personal Profile Hub Card
            AnimatedVisibility(
                visible = showUserQuickProfile,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f * contactCardOpacity), RoundedCornerShape(16.dp))
                            .testTag("user_profile_card"),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "My Profile Label",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "My Personal Profile Card",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = { onContactSelect(-1) }) { // -1 represents personal profile edit
                                    Icon(Icons.Default.Edit, contentDescription = "Edit My Profile", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "This is your permanent local-first profile card. You can configure custom share-links, birthday reminders, and your basic details directly on device.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onContactSelect(-1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Open Profile Panel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { showUserQuickProfile = false },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Dismiss", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Contacts List View with local swipe actions
            if (activeContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Placeholder",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isRelationshipModeActive) "No Contacts with Birthdays" else "No Contacts Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRelationshipModeActive) "Add contacts with specified birthdates in details view first." else "Tap the plus button below to create your first secure relationship model.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("contacts_lazy_column"),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(activeContacts, key = { it.contact.id }) { item ->
                        ContactDismissibleRow(
                            item = item,
                            onSelect = {
                                if (isRelationshipModeActive) {
                                    if (firstSelectedContactForRelationship == null) {
                                        firstSelectedContactForRelationship = item
                                        val msg = if (currentLanguage == "de") "${item.contact.firstName} ausgewählt. Wählen Sie nun den zweiten Kontakt." else "Selected ${item.contact.firstName}. Now select the second contact."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    } else {
                                        if (firstSelectedContactForRelationship?.contact?.id == item.contact.id) {
                                            val msg = if (currentLanguage == "de") "Bitte wählen Sie ein anderes Profil aus." else "Please select a different profile."
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        } else {
                                            secondSelectedContactForRelationship = item
                                            isRelationshipModeActive = false
                                            showRelationshipResultScreen = true
                                        }
                                    }
                                } else {
                                    onContactSelect(item.contact.id)
                                }
                            },
                            onDelete = {
                                viewModel.deleteContact(item)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Deleted ${item.contact.firstName}",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            },
                            swipeBackgroundColor = if (isShaderEnabled) Color.Transparent else if (isCustomThemeEnabled) Color(customDeleteColor) else Color(0xFFFF5252),
                            onConfigureSocialLink = { action ->
                                activeListLinkEditingAction = action
                                showListLinkEditingDialog = true
                            },
                            onEmailActionClick = { emails, primaryEmailStr ->
                                if (emails.size > 1) {
                                    emailsSelectionList = emails
                                } else {
                                    IntentExecutor.executeAction(context, "EMAIL", primaryEmailStr)
                                }
                            },
                            contactCardOpacity = contactCardOpacity
                        )
                    }
                }
            }
        }
    }

    if (showBulkImportDialog) {
        var bulkSearchQuery by remember { mutableStateOf("") }
        var selectedIds by remember { mutableStateOf(emptySet<String>()) }

        val filteredPhoneContacts = remember(loadedPhoneContacts, bulkSearchQuery) {
            if (bulkSearchQuery.isBlank()) {
                loadedPhoneContacts
            } else {
                loadedPhoneContacts.filter {
                    it.name.contains(bulkSearchQuery, ignoreCase = true) ||
                    it.phoneNumbers.any { num -> num.contains(bulkSearchQuery) } ||
                    it.emails.any { email -> email.contains(bulkSearchQuery, ignoreCase = true) }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showBulkImportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Contacts, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bulk Phone Import")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    if (isLoadingPhoneContacts) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Reading contact directory...", fontSize = 13.sp)
                            }
                        }
                    } else if (loadedPhoneContacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "No contacts found on device (or in emulator).",
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        loadedPhoneContacts = listOf(
                                            com.example.util.PhoneContact(id = "sim_1", name = "Alice Vance", phoneNumbers = listOf("+1-555-0199"), emails = listOf("alice@example.com")),
                                            com.example.util.PhoneContact(id = "sim_2", name = "Bob Dylan", phoneNumbers = listOf("+1-555-0211"), emails = listOf("bob@example.com")),
                                            com.example.util.PhoneContact(id = "sim_3", name = "Charlie Prince", phoneNumbers = listOf("+1-555-0322"), emails = listOf("charlie@example.com")),
                                            com.example.util.PhoneContact(id = "sim_4", name = "Diana Ross", phoneNumbers = listOf("+1-555-0455"), emails = listOf("diana@example.com")),
                                            com.example.util.PhoneContact(id = "sim_5", name = "Ethan Hunt", phoneNumbers = listOf("+1-555-0566"), emails = listOf("ethan@impossible.com")),
                                            com.example.util.PhoneContact(id = "sim_6", name = "Fiona Gallagher", phoneNumbers = listOf("+1-555-0677"), emails = listOf("fiona@example.com")),
                                            com.example.util.PhoneContact(id = "sim_7", name = "George Clooney", phoneNumbers = listOf("+1-555-0788"), emails = listOf("george@example.com"))
                                        )
                                    },
                                    modifier = Modifier.testTag("load_mock_contacts_button")
                                ) {
                                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Load Simulated Contacts")
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = bulkSearchQuery,
                            onValueChange = { bulkSearchQuery = it },
                            placeholder = { Text("Filter phone contacts...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedIds.size == filteredPhoneContacts.size) {
                                        selectedIds = selectedIds - filteredPhoneContacts.map { it.id }.toSet()
                                    } else {
                                        selectedIds = selectedIds + filteredPhoneContacts.map { it.id }.toSet()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allSelected = filteredPhoneContacts.isNotEmpty() && filteredPhoneContacts.all { it.id in selectedIds }
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = {
                                    if (allSelected) {
                                        selectedIds = selectedIds - filteredPhoneContacts.map { it.id }.toSet()
                                    } else {
                                        selectedIds = selectedIds + filteredPhoneContacts.map { it.id }.toSet()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (allSelected) "Deselect All Filtered" else "Select All Filtered",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredPhoneContacts) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds = if (contact.id in selectedIds) {
                                                selectedIds - contact.id
                                            } else {
                                                selectedIds + contact.id
                                            }
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = contact.id in selectedIds,
                                        onCheckedChange = { isChecked ->
                                            selectedIds = if (isChecked) {
                                                selectedIds + contact.id
                                            } else {
                                                selectedIds - contact.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        val phoneText = contact.phoneNumbers.firstOrNull() ?: "No number"
                                        val emailText = contact.emails.firstOrNull()?.let { " | $it" } ?: ""
                                        Text(
                                            text = "$phoneText$emailText",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toImport = loadedPhoneContacts.filter { it.id in selectedIds }
                        if (toImport.isNotEmpty()) {
                            viewModel.importBulkPhoneContacts(context, toImport) { count ->
                                val msg = when {
                                    count == 0 -> "Selected contact(s) already exist. No duplicates imported."
                                    count < toImport.size -> "Successfully imported $count contact(s). Duplicates were skipped."
                                    else -> "Successfully imported $count contact(s)!"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                        showBulkImportDialog = false
                    },
                    enabled = !isLoadingPhoneContacts && selectedIds.isNotEmpty()
                ) {
                    Text("Import (${selectedIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showListSocialHelpDialog) {
        SocialHelpDialog(
            onDismissRequest = { showListSocialHelpDialog = false }
        )
    }

    if (showListSocialHowToFindDialog) {
        SocialHowToFindDialog(
            onDismissRequest = { showListSocialHowToFindDialog = false }
        )
    }

    if (emailsSelectionList != null) {
        AlertDialog(
            onDismissRequest = { emailsSelectionList = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Select Email Address")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "This contact has multiple email addresses. Choose which one to use:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    emailsSelectionList?.forEach { emailObj ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    IntentExecutor.executeAction(context, "EMAIL", emailObj.email)
                                    emailsSelectionList = null
                                }
                                .testTag("email_selection_item_${emailObj.email}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = emailObj.email,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (emailObj.label.isNotBlank()) {
                                        Text(
                                            text = emailObj.label.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Select email",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { emailsSelectionList = null }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("email_selection_dialog")
        )
    }

    if (showListLinkEditingDialog && activeListLinkEditingAction != null) {
        var tempLink by remember(activeListLinkEditingAction) { mutableStateOf(activeListLinkEditingAction?.targetData ?: "") }

        AlertDialog(
            onDismissRequest = { showListLinkEditingDialog = false },
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
                            onClick = { showListSocialHowToFindDialog = true },
                            modifier = Modifier.testTag("social_howto_find_button_list")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "How to find profile links instructions",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = { showListSocialHelpDialog = true },
                            modifier = Modifier.testTag("social_help_button_list")
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
                        placeholder = { Text("e.g. twitter.com/username") },
                        label = { Text("URL / Link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("list_social_link_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentAction = activeListLinkEditingAction
                        if (currentAction != null) {
                            val updatedAction = currentAction.copy(targetData = tempLink)
                            viewModel.updateCustomAction(updatedAction)
                            Toast.makeText(context, "Link configured successfully!", Toast.LENGTH_SHORT).show()
                        }
                        showListLinkEditingDialog = false
                    },
                    modifier = Modifier.testTag("list_submit_social_link_button")
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showListLinkEditingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRelationshipInstructionDialog) {
        AlertDialog(
            onDismissRequest = { showRelationshipInstructionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF2E93),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (currentLanguage == "de") "Astro-Kompatibilität" else "Astro Compatibility",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = if (currentLanguage == "de") {
                        "Um die Kompatibilität zwischen 2 Personen zu prüfen, wählen Sie diese aus, indem Sie auf 2 verschiedene Profile mit Geburtsdatum in Ihrer Liste tippen.\n\nHinweis: Kontakte ohne konfiguriertes Geburtsdatum werden während dieser Aktion vorübergehend gefiltert."
                    } else {
                        "To check compatibility between 2 people, select them by tapping on 2 different profiles with birthdates in your list.\n\nNote: Contacts without configured birthdates will be temporarily filtered during this event."
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRelationshipInstructionDialog = false
                        isRelationshipModeActive = true
                        firstSelectedContactForRelationship = null
                        secondSelectedContactForRelationship = null
                        val msg = if (currentLanguage == "de") "Beziehungsmodus aktiv! Scrollen und tippen Sie auf das erste Profil." else "Relationship mode active! Scroll & tap 1st profile."
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF2E93),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_relationship_instructions_button")
                ) {
                    Text(if (currentLanguage == "de") "Lass uns beginnen" else "Let's Begin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRelationshipInstructionDialog = false }) {
                    Text(if (currentLanguage == "de") "Abbrechen" else "Cancel")
                }
            }
        )
    }

    if (showRelationshipResultScreen) {
        val p1 = firstSelectedContactForRelationship
        val p2 = secondSelectedContactForRelationship
        if (p1 != null && p2 != null) {
            RelationshipResultScreen(
                person1 = p1,
                person2 = p2,
                onDismiss = {
                    showRelationshipResultScreen = false
                    firstSelectedContactForRelationship = null
                    secondSelectedContactForRelationship = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDismissibleRow(
    item: ContactWithListDetails,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    swipeBackgroundColor: Color = Color(0xFFFF5252),
    onConfigureSocialLink: (com.example.data.entity.CustomAction) -> Unit = {},
    onEmailActionClick: (List<com.example.data.entity.Email>, String) -> Unit = { _, _ -> },
    contactCardOpacity: Float = 1.0f
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    // Reset swipe state of recycled or restored items to prevent visually sticking in the swiped-away state
    LaunchedEffect(item) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart || dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
            val color = if (isSwiping) swipeBackgroundColor else Color.Transparent
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwiping) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Swipe to delete relation",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        ContactRowCard(
            item = item,
            onClick = onSelect,
            onConfigureSocialLink = onConfigureSocialLink,
            onEmailActionClick = onEmailActionClick,
            contactCardOpacity = contactCardOpacity
        )
    }
}

@Composable
fun ContactRowCard(
    item: ContactWithListDetails,
    onClick: () -> Unit,
    onConfigureSocialLink: (com.example.data.entity.CustomAction) -> Unit = {},
    onEmailActionClick: (List<com.example.data.entity.Email>, String) -> Unit = { _, _ -> },
    contactCardOpacity: Float = 1.0f
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f * contactCardOpacity), RoundedCornerShape(16.dp))
            .testTag("contact_item_card_${item.contact.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // profile photo
            Surface(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (item.contact.profilePhotoUri != null) {
                    val safePhotoModel = com.example.util.ImageUtils.rememberProfilePhotoState(item.contact.profilePhotoUri)
                    AsyncImage(
                        model = safePhotoModel,
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.contact.firstName.take(1).uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${item.contact.firstName} ${item.contact.lastName}".trim(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (!item.contact.nickname.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${item.contact.nickname})",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!item.contact.pronouns.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${item.contact.pronouns})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rating Badge
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${item.contact.rating}/10",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Contact Tag Badge
                    if (!item.contact.tag.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.contact.tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Profile Notes Preview Snippet
                if (!item.contact.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.contact.notes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Dedicated first-screen quick action buttons + Social Media Hub (next to other quick press items)
                val primaryPhone = item.phoneNumbers.firstOrNull()?.number
                val primaryEmail = item.emails.firstOrNull()?.email
                val primaryAddress = item.addresses.firstOrNull()?.formattedAddress

                if (!primaryPhone.isNullOrBlank() || !primaryEmail.isNullOrBlank() || !primaryAddress.isNullOrBlank() || item.customActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val quickIcons = remember(primaryPhone, primaryEmail, primaryAddress, item.customActions) {
                        val list = mutableListOf<@Composable () -> Unit>()
                        if (!primaryPhone.isNullOrBlank()) {
                            list.add {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { IntentExecutor.executeAction(context, "PHONE_CALL", primaryPhone) }
                                        .testTag("quick_call_${item.contact.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Contact",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            list.add {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { IntentExecutor.executeAction(context, "SMS", primaryPhone) }
                                        .testTag("quick_sms_${item.contact.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sms,
                                        contentDescription = "Message Contact",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (!primaryAddress.isNullOrBlank()) {
                            list.add {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { IntentExecutor.openDirections(context, primaryAddress) }
                                        .testTag("quick_map_${item.contact.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Map Location",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (!primaryEmail.isNullOrBlank()) {
                            list.add {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .clickable { onEmailActionClick(item.emails, primaryEmail) }
                                        .testTag("quick_email_${item.contact.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Compose Email",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        for (action in item.customActions) {
                            list.add {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), CircleShape)
                                        .clickable {
                                            if (action.targetData.isBlank()) {
                                                onConfigureSocialLink(action)
                                            } else {
                                                try {
                                                    var url = action.targetData.trim()
                                                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                                        url = "https://$url"
                                                    }
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Invalid link format: ${action.targetData}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .testTag("list_social_icon_${action.actionId}"),
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        list
                    }

                    val maxPerRow = 5
                    val iconRows = quickIcons.chunked(maxPerRow)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        iconRows.forEach { rowIcons ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowIcons.forEach { icon -> icon() }
                            }
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
