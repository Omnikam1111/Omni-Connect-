package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.app.role.RoleManager
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.SmsAndCallLogger
import com.example.util.SmsMessage
import com.example.util.SmsThread
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SmsMessagingScreen(
    currentLanguage: String = "en",
    prefilledNumber: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDefaultSms by remember { mutableStateOf(checkIsDefaultSms(context)) }
    var hasSmsPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    var threads by remember { mutableStateOf<List<SmsThread>>(emptyList()) }
    var selectedThread by remember { mutableStateOf<SmsThread?>(null) }
    var messagesList by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
    var contactsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingThreads by remember { mutableStateOf(false) }
    var isLoadingMessages by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    var draftText by remember { mutableStateOf("") }
    var searchRecipient by remember(prefilledNumber) { mutableStateOf(prefilledNumber ?: "") }
    var isCreatingNewSms by remember(prefilledNumber) { mutableStateOf(!prefilledNumber.isNullOrBlank()) }

    fun refreshThreads() {
        if (hasSmsPermissions) {
            isLoadingThreads = true
            scope.launch {
                val map = SmsAndCallLogger.loadContactNamesMap(context)
                val fetched = withContext(Dispatchers.IO) {
                    SmsAndCallLogger.fetchSmsThreads(context)
                }
                withContext(Dispatchers.Main) {
                    contactsMap = map
                    threads = fetched
                    isLoadingThreads = false
                }
            }
        }
    }

    fun loadMessagesForThread(threadId: Long) {
        if (hasSmsPermissions) {
            isLoadingMessages = true
            scope.launch {
                val fetched = withContext(Dispatchers.IO) {
                    SmsAndCallLogger.fetchMessagesForThread(context, threadId)
                }
                withContext(Dispatchers.Main) {
                    messagesList = fetched
                    isLoadingMessages = false
                }
            }
        }
    }

    LaunchedEffect(hasSmsPermissions) {
        refreshThreads()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultSms = checkIsDefaultSms(context)
                hasSmsPermissions = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                if (hasSmsPermissions) {
                    refreshThreads()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermissions = results[Manifest.permission.READ_SMS] == true &&
                results[Manifest.permission.SEND_SMS] == true
        if (hasSmsPermissions) {
            refreshThreads()
        }
    }

    val defaultSmsSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultSms = checkIsDefaultSms(context)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Screen Header & Back option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedThread != null || isCreatingNewSms) {
                        IconButton(
                            onClick = {
                                selectedThread = null
                                isCreatingNewSms = false
                                refreshThreads()
                            },
                            modifier = Modifier.testTag("sms_back_to_threads")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text(
                        text = if (isCreatingNewSms) {
                            if (currentLanguage == "de") "Neue SMS" else "New Message"
                        } else if (selectedThread != null) {
                            SmsAndCallLogger.getContactName(selectedThread!!.address, contactsMap)
                        } else {
                            if (currentLanguage == "de") "SMS-Nachrichten" else "SMS Messages"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (selectedThread == null && !isCreatingNewSms) {
                    IconButton(
                        onClick = { isCreatingNewSms = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .testTag("new_sms_btn")
                    ) {
                        Icon(Icons.Default.AddComment, contentDescription = "New chat", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Default App Check Baner
            if (!isDefaultSms && selectedThread == null && !isCreatingNewSms) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentLanguage == "de") "Nicht Standard-SMS-App" else "Not Default SMS App",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (currentLanguage == "de") "Erlaubt das Senden und Empfangen von Kurzmitteilungen." else "Receive and reply to SMS instantly.",
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                                    if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                                        defaultSmsSelector.launch(intent)
                                    }
                                } else {
                                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                        putExtra("package", context.packageName)
                                    }
                                    defaultSmsSelector.launch(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (currentLanguage == "de") "Standard" else "Set Default", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Permissions Needed Check Banner
            if (!hasSmsPermissions && selectedThread == null && !isCreatingNewSms) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentLanguage == "de") "SMS-Berechtigung fehlt" else "SMS Permission Needed",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (currentLanguage == "de") "Bedingung für den Nachrichtenzugriff auf Ihrem Gerät." else "Grant secure access to sync your inbox.",
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.SEND_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(if (currentLanguage == "de") "Zulassen" else "Grant", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Screen State flow rendering
            if (isCreatingNewSms) {
                // New SMS setup screen
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchRecipient,
                        onValueChange = { searchRecipient = it },
                        label = { Text(if (currentLanguage == "de") "Empfängernummer" else "Recipient Number") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("sms_recipient_input"),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        label = { Text(if (currentLanguage == "de") "Nachrichtentext" else "Message text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("sms_new_message_body"),
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (searchRecipient.isNotBlank() && draftText.isNotBlank()) {
                                SmsAndCallLogger.sendSms(context, searchRecipient, draftText) { success ->
                                    if (success) {
                                        draftText = ""
                                        searchRecipient = ""
                                        isCreatingNewSms = false
                                        refreshThreads()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_new_sms_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentLanguage == "de") "Senden" else "Send SMS")
                    }
                }
            } else if (selectedThread != null) {
                // Inside an active message conversation Thread
                LaunchedEffect(selectedThread) {
                    selectedThread?.let { loadMessagesForThread(it.threadId) }
                }

                if (isLoadingMessages) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(messagesList) { msg ->
                                val isMe = msg.type == 2 // 2 corresponds to outbox/sent messages
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMe) 12.dp else 0.dp,
                                            bottomEnd = if (isMe) 0.dp else 12.dp
                                        ),
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = msg.body,
                                                fontSize = 13.sp,
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                             Spacer(modifier = Modifier.height(4.dp))
                                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                            Text(
                                                text = timeFormat.format(Date(msg.date)),
                                                fontSize = 9.sp,
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Reply message text field row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = draftText,
                                onValueChange = { draftText = it },
                                placeholder = { Text(if (currentLanguage == "de") "Antwort eingeben..." else "Type message...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .testTag("sms_reply_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )

                            IconButton(
                                onClick = {
                                    val destination = selectedThread?.address
                                    if (!destination.isNullOrBlank() && draftText.isNotBlank()) {
                                        SmsAndCallLogger.sendSms(context, destination, draftText) { success ->
                                            if (success) {
                                                draftText = ""
                                                loadMessagesForThread(selectedThread!!.threadId)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("sms_reply_send_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Chats History threads list browser
                if (isLoadingThreads) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentLanguage == "de") "Keine Nachrichten vorhanden. Starten Sie ein Gespräch, indem Sie oben rechts tippen!" else "No threads in messages.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                        items(threads) { thread ->
                            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                            val prettyDate = sdf.format(Date(thread.date))
                            val displayName = SmsAndCallLogger.getContactName(thread.address, contactsMap)

                            Card(
                                onClick = { selectedThread = thread },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sms_thread_${thread.threadId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val avatarChar = if (displayName.firstOrNull()?.isLetter() == true) displayName.take(1).uppercase() else "#"
                                        Text(
                                            text = avatarChar,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = prettyDate,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = thread.snippet,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun checkIsDefaultSms(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        roleManager.isRoleHeld(RoleManager.ROLE_SMS)
    } else {
        Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }
}
