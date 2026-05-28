package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telecom.TelecomManager
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.app.role.RoleManager
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.CallLogEntry
import com.example.util.SmsAndCallLogger
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PhoneDialerScreen(
    currentLanguage: String = "en",
    prefilledNumber: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDefaultDialer by remember { mutableStateOf(checkIsDefaultDialer(context)) }
    var hasCallLogPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        )
    }

    var callLogs by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    var contactsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingLogs by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var dialNumber by remember(prefilledNumber) { mutableStateOf(prefilledNumber ?: "") }
    var showDialPad by remember { mutableStateOf(true) }

    fun refreshCallLogs() {
        if (hasCallLogPermission) {
            isLoadingLogs = true
            scope.launch {
                val map = SmsAndCallLogger.loadContactNamesMap(context)
                val fetched = withContext(Dispatchers.IO) {
                    SmsAndCallLogger.fetchCallLogs(context)
                }
                withContext(Dispatchers.Main) {
                    contactsMap = map
                    callLogs = fetched
                    isLoadingLogs = false
                }
            }
        }
    }

    LaunchedEffect(hasCallLogPermission) {
        refreshCallLogs()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultDialer = checkIsDefaultDialer(context)
                hasCallLogPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                if (hasCallLogPermission) {
                    refreshCallLogs()
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
        hasCallLogPermission = results[Manifest.permission.READ_CALL_LOG] == true &&
                results[Manifest.permission.CALL_PHONE] == true
        if (hasCallLogPermission) {
            refreshCallLogs()
        }
    }

    val defaultDialerSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultDialer = checkIsDefaultDialer(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Header
            Text(
                text = if (currentLanguage == "de") "Telefon-Dialer" else "Phone Dialer",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Default Dialer Status Banner
            if (!isDefaultDialer) {
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
                                text = if (currentLanguage == "de") "Nicht Standard-App" else "Not Default Phone App",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (currentLanguage == "de") "Standard-Dialer festlegen, um Anrufe auszuführen." else "Set default calling role to handle system calls.",
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                                    if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                                        defaultDialerSelector.launch(intent)
                                    }
                                } else {
                                    val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                        putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                                    }
                                    defaultDialerSelector.launch(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(if (currentLanguage == "de") "Aktivieren" else "Set Default", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Call Permissions Banner
            if (!hasCallLogPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
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
                                text = if (currentLanguage == "de") "Anrufberechtigung fehlt" else "Call Permission Needed",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (currentLanguage == "de") "Benötigt, um Anrufprotokolle anzuzeigen." else "Access callers history securely.",
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALL_LOG,
                                        Manifest.permission.WRITE_CALL_LOG,
                                        Manifest.permission.CALL_PHONE
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text(if (currentLanguage == "de") "Zulassen" else "Grant", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Call Logs Section
            Text(
                text = if (currentLanguage == "de") "Letzte Anrufe" else "Recent Calls",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isLoadingLogs) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (callLogs.isEmpty()) {
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
                        text = if (currentLanguage == "de") "Keine Anrufe vorhanden oder Berechtigungen fehlen." else "No phone records available.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(callLogs) { log ->
                        val sdf = SimpleDateFormat("dd.MM - HH:mm", Locale.getDefault())
                        val flowDate = sdf.format(Date(log.date))

                        val icon = when (log.type) {
                            CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived
                            CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
                            CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
                            else -> Icons.Default.Call
                        }

                        val iconColor = when (log.type) {
                            CallLog.Calls.MISSED_TYPE -> MaterialTheme.colorScheme.error
                            CallLog.Calls.INCOMING_TYPE -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        val resolvedName = log.cachedName ?: SmsAndCallLogger.getContactName(log.number, contactsMap)
                        val hasResolvedName = resolvedName != log.number

                        Card(
                            onClick = {
                                dialNumber = log.number
                                showDialPad = true
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Call Type",
                                        tint = iconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = resolvedName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (hasResolvedName) {
                                            Text(
                                                text = log.number,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = flowDate,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active typed dial display area
        AnimatedVisibility(visible = showDialPad) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dial String input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(48.dp))
                    Text(
                        text = dialNumber.ifEmpty { " " },
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    if (dialNumber.isNotEmpty()) {
                        IconButton(
                            onClick = { dialNumber = dialNumber.dropLast(1) },
                            modifier = Modifier.testTag("dialer_backspace")
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Delete")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // Dial Pad Keys Grid
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("*", "0", "#")
                )

                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowKeys.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                    .clickable { dialNumber += key }
                                    .testTag("dial_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom dialed trigger call action call row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showDialPad = false }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Dialpad")
                    }

                    FloatingActionButton(
                        onClick = {
                            if (dialNumber.isNotBlank()) {
                                SmsAndCallLogger.makePhoneCall(context, dialNumber)
                            }
                        },
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("dial_call_btn")
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Place Call", modifier = Modifier.size(24.dp))
                    }

                    IconButton(
                        onClick = { dialNumber = "" },
                        enabled = dialNumber.isNotEmpty()
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear all", tint = if (dialNumber.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    }
                }
            }
        }

        if (!showDialPad) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { showDialPad = true },
                    modifier = Modifier.testTag("show_dialpad_btn")
                ) {
                    Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentLanguage == "de") "Wähltastatur anzeigen" else "Show Dialpad")
                }
            }
        }
    }
}

private fun checkIsDefaultDialer(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
        roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        telecomManager.defaultDialerPackage == context.packageName
    }
}
