package com.example.ui.screens

import android.util.Log
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.data.SettingsPreferences
import com.example.util.BackupRestoreHelper
import com.example.worker.InteractionTrackingWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.example.ui.viewmodel.ContactListViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class ThemePresetInfo(
    val id: String,
    val name: String,
    val description: String,
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val delete: Color
)

val themePresets = listOf(
    ThemePresetInfo("classic", "Classic Dark", "Original purple Material 3 dark elegance", Color(0xFFD0BCFF), Color(0xFFCCC2DC), Color(0xFF141218), Color(0xFF1D1B20), Color(0xFFFF5252)),
    ThemePresetInfo("midnight_ocean", "Midnight Ocean", "Teal highlights on deep blue depth", Color(0xFF00ADB5), Color(0xFF393E46), Color(0xFF1A1F29), Color(0xFF242B35), Color(0xFFFF2E63)),
    ThemePresetInfo("forest_sage", "Forest Sage", "Earthy tones, warm forest moss greens", Color(0xFF81B214), Color(0xFF206A5D), Color(0xFF101715), Color(0xFF16211D), Color(0xFFD24E4E)),
    ThemePresetInfo("sunset_glow", "Sunset Glow", "Warm sunset terracotta and amber beauty", Color(0xFFF07B3F), Color(0xFFEA5455), Color(0xFF251B17), Color(0xFF312520), Color(0xFFD80032)),
    ThemePresetInfo("cyberpunk_neon", "Cyberpunk Neon", "Futuristic neon cyan with magenta glow", Color(0xFF00F0FF), Color(0xFFFF007F), Color(0xFF0D0B18), Color(0xFF18142C), Color(0xFFFF0055))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ContactListViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = viewModel.preferences

    var biometricEnabled by remember { mutableStateOf(preferences.isBiometricEnabled) }
    var realPinState by remember { mutableStateOf(preferences.realPin) }
    var decoyPinState by remember { mutableStateOf(preferences.decoyPin) }

    var smtpHostState by remember { mutableStateOf(preferences.smtpHost) }
    var smtpPortState by remember { mutableStateOf(preferences.smtpPort) }
    var smtpUsernameState by remember { mutableStateOf(preferences.smtpUsername) }
    var smtpPasswordState by remember { mutableStateOf(preferences.smtpPassword) }
    var smtpSenderState by remember { mutableStateOf(preferences.smtpSender) }
    var smtpUseSslState by remember { mutableStateOf(preferences.smtpUseSsl) }
    var smtpUseTlsState by remember { mutableStateOf(preferences.smtpUseTls) }

    var googleAccountEmailState by remember { mutableStateOf(preferences.googleAccountEmail) }
    var googleAccountNameState by remember { mutableStateOf(preferences.googleAccountName) }
    var googleOAuthClientIdState by remember { mutableStateOf(preferences.googleOAuthClientId) }
    var googleOAuthClientSecretState by remember { mutableStateOf(preferences.googleOAuthClientSecret) }

    val isCustomThemeEnabled by viewModel.isCustomThemeEnabled.collectAsStateWithLifecycle()
    val isCustomThemeUnlocked by viewModel.isCustomThemeUnlocked.collectAsStateWithLifecycle()
    val customPrimary by viewModel.customPrimaryColor.collectAsStateWithLifecycle()
    val customSecondary by viewModel.customSecondaryColor.collectAsStateWithLifecycle()
    val customBackground by viewModel.customBackgroundColor.collectAsStateWithLifecycle()
    val customSurface by viewModel.customSurfaceColor.collectAsStateWithLifecycle()
    val customDelete by viewModel.customDeleteColor.collectAsStateWithLifecycle()
    val selectedThemePreset by viewModel.selectedThemePreset.collectAsStateWithLifecycle()
    val isShaderEnabled by viewModel.isShaderBackgroundEnabled.collectAsStateWithLifecycle()
    val selectedShaderPresetState by viewModel.selectedShaderPreset.collectAsStateWithLifecycle()
    val customShaderCodeState by viewModel.customShaderCode.collectAsStateWithLifecycle()
    val contactCardOpacity by viewModel.contactCardOpacity.collectAsStateWithLifecycle()
    val savedShaderNames by viewModel.savedShaderNames.collectAsStateWithLifecycle(initialValue = emptySet())
    val refreshTrigger by viewModel.preferencesRefreshTrigger.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            biometricEnabled = preferences.isBiometricEnabled
            realPinState = preferences.realPin
            decoyPinState = preferences.decoyPin
            smtpHostState = preferences.smtpHost
            smtpPortState = preferences.smtpPort
            smtpUsernameState = preferences.smtpUsername
            smtpPasswordState = preferences.smtpPassword
            smtpSenderState = preferences.smtpSender
            smtpUseSslState = preferences.smtpUseSsl
            smtpUseTlsState = preferences.smtpUseTls
            googleAccountEmailState = preferences.googleAccountEmail
            googleAccountNameState = preferences.googleAccountName
            googleOAuthClientIdState = preferences.googleOAuthClientId
            googleOAuthClientSecretState = preferences.googleOAuthClientSecret
        }
    }

    // Dialog trigger controls
    var showEditPinDialog by remember { mutableStateOf(false) }
    var showImportMergeDialog by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var showSmtpHelpDialog by remember { mutableStateOf(false) }

    // Permission check triggers
    var hasCallLogPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCallLogPermission = results[Manifest.permission.READ_CALL_LOG] ?: hasCallLogPermission
        hasSmsPermission = results[Manifest.permission.READ_SMS] ?: hasSmsPermission

        if (hasCallLogPermission && hasSmsPermission) {
            Toast.makeText(context, "Permissions enabled. Tracking worker initialized.", Toast.LENGTH_SHORT).show()
            InteractionTrackingWorker.schedule(context)
        } else {
            Toast.makeText(context, "Permissions denied. Some auto-logging feature may be locked.", Toast.LENGTH_SHORT).show()
        }
    }

    // Export Document Launcher (requirement 2.9 JSON Export)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val success = BackupRestoreHelper.exportToStream(context, outputStream)
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            if (success) {
                                Toast.makeText(context, "Database exported successfully", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Export failed. Please check storage.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Import Document Launcher (requirement 2.9 JSON Import)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedImportUri = uri
            showImportMergeDialog = true // Display option: overwrite or merge!
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (isShaderEnabled) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(com.example.util.Localization.getString("vault_settings", currentLanguage)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isShaderEnabled) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Debug Logs Section
            var errorLogs by remember { mutableStateOf(com.example.util.ErrorLogger.getLogs(context)) }
            Text(com.example.util.Localization.getString("diagnostic_logs", currentLanguage), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    errorLogs.forEach { log ->
                        Text(log, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    Button(onClick = { errorLogs = com.example.util.ErrorLogger.getLogs(context) }) {
                        Text(if (currentLanguage == "de") "Protokolle aktualisieren" else "Refresh Logs")
                    }
                }
            }

            // Language Settings Section
            Text(
                text = com.example.util.Localization.getString("lang_settings_title", currentLanguage),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = com.example.util.Localization.getString("select_lang", currentLanguage),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "en" to "English",
                            "de" to "Deutsch (German)"
                        ).forEach { (code, name) ->
                            val isSelected = currentLanguage == code
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setAppLanguage(code) },
                                label = { Text(name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
            // Section 1: Biometric and Decoy Protection
            Text(if (currentLanguage == "de") "Privatsphäre & Tresorschutz" else "Privacy & Vault Shield", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Biometric Lock Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (currentLanguage == "de") "Biometrische Verschlüsselung" else "Biometric Encryption", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (currentLanguage == "de") "Biologischen Fingerabdruck/Gesichtserkennung vor dem Start des Tastenfelds anfordern" else "Request biological fingerprint/face signature before keypad launch", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { newVal ->
                                biometricEnabled = newVal
                                preferences.isBiometricEnabled = newVal
                                val toastMsg = if (currentLanguage == "de") {
                                    "Biometrischer Schutz ${if (newVal) "aktiviert" else "deaktiviert"}"
                                } else {
                                    "Biometric shield ${if (newVal) "enabled" else "disabled"}"
                                }
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("biometric_shield_toggle")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                    // Customize Passcodes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditPinDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (currentLanguage == "de") "Tresor-Passcodes konfigurieren" else "Configure Vault Passcodes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (currentLanguage == "de") {
                                    "Echten PIN ($realPinState) und plausiblen Schein-PIN ($decoyPinState) anpassen"
                                } else {
                                    "Adjust Real PIN ($realPinState) and plausible Decoy PIN ($decoyPinState)"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            // Section: Selectable Theme Profiles
            Text(if (currentLanguage == "de") "Auswählbare Design-Profile" else "Selectable Theme Profiles", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (currentLanguage == "de") {
                            "Wählen Sie unten ein integriertes Design- oder Farbprofil aus. Durch Anpassen der Schieberegler unten werden Ihre Designparameter in benutzerdefinierte Überschreibungen umgewandelt."
                        } else {
                            "Choose a built-in style or color profile below. Customizing sliders below will update your theme parameters into custom overrides."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    themePresets.forEach { preset ->
                        val isSelected = selectedThemePreset == preset.id && isCustomThemeEnabled
                        Card(
                            onClick = {
                                viewModel.selectThemePreset(preset.id)
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.5.dp, 
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f * contactCardOpacity)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f * contactCardOpacity),
                                contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("theme_preset_${preset.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectThemePreset(preset.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("radio_theme_${preset.id}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (currentLanguage == "de") {
                                            when (preset.id) {
                                                "classic" -> "Klassisch Dunkel"
                                                "midnight_ocean" -> "Mitternachtsozean"
                                                "forest_sage" -> "Waldsalbei"
                                                "sunset_glow" -> "Abendrot"
                                                "cyberpunk_neon" -> "Cyberpunk-Neon"
                                                else -> preset.name
                                            }
                                        } else preset.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (currentLanguage == "de") {
                                            when (preset.id) {
                                                "classic" -> "Originale violette Material 3 dunkle Eleganz"
                                                "midnight_ocean" -> "Türkisfarbene Highlights auf tiefblauem Grund"
                                                "forest_sage" -> "Erdige Töne, warme Waldmoosgrüns"
                                                "sunset_glow" -> "Warme Terracotta- und bernsteinfarbene Schönheit"
                                                "cyberpunk_neon" -> "Futuristisches Neon-Cyan mit magentafarbenem Leuchten"
                                                else -> preset.description
                                            }
                                        } else preset.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // Visual color pills
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val localizedLabels = if (currentLanguage == "de") {
                                            listOf(
                                                preset.primary to "Primär",
                                                preset.secondary to "Sekundär",
                                                preset.background to "Hintergrund",
                                                preset.surface to "Oberfläche",
                                                preset.delete to "Aktion Warnung"
                                            )
                                        } else {
                                            listOf(
                                                preset.primary to "Primary",
                                                preset.secondary to "Secondary",
                                                preset.background to "Background",
                                                preset.surface to "Surface",
                                                preset.delete to "Action Warning"
                                            )
                                        }
                                        localizedLabels.forEach { (color, label) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(color)
                                                    .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
               // Section: Custom Theme Color Customizer with Sliders for all major UI items
            Text(if (currentLanguage == "de") "Designfarben-Konfigurator" else "App Theme Colors Customizer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (currentLanguage == "de") "Benutzerdefinierte Farben freischalten" else "Unlock Custom Colors", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (currentLanguage == "de") "Farben manuell anpassen und auf alle wichtigen Layouts anwenden" else "Manually slide and overrides colors across all major view items", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = isCustomThemeUnlocked,
                            onCheckedChange = { viewModel.updateThemeUnlocked(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("custom_theme_toggle")
                        )
                    }

                    if (isCustomThemeUnlocked) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentLanguage == "de") "Verwenden Sie die Regler unten, um eigene Designfarben festzulegen:" else "Move sliders below to design custom app theme values:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { viewModel.resetToDefaultThemeColors() },
                                modifier = Modifier.testTag("reset_custom_colors_button")
                            ) {
                                Text(if (currentLanguage == "de") "Zurücksetzen" else "Reset to Default", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        ColorCustomizerSection(
                            title = if (currentLanguage == "de") "Primärfarbe" else "Primary Color",
                            colorValue = customPrimary,
                            onColorChanged = { viewModel.updatePrimaryColor(it) },
                            tagPrefix = "primary_color",
                            contactCardOpacity = contactCardOpacity
                        )

                        ColorCustomizerSection(
                            title = if (currentLanguage == "de") "Sekundärfarbe" else "Secondary Color",
                            colorValue = customSecondary,
                            onColorChanged = { viewModel.updateSecondaryColor(it) },
                            tagPrefix = "secondary_color",
                            contactCardOpacity = contactCardOpacity
                        )

                        ColorCustomizerSection(
                            title = if (currentLanguage == "de") "Hintergrundfarbe" else "Background Color",
                            colorValue = customBackground,
                            onColorChanged = { viewModel.updateBackgroundColor(it) },
                            tagPrefix = "background_color",
                            contactCardOpacity = contactCardOpacity
                        )

                        ColorCustomizerSection(
                            title = if (currentLanguage == "de") "Oberflächenfarbe" else "Surface Color",
                            colorValue = customSurface,
                            onColorChanged = { viewModel.updateSurfaceColor(it) },
                            tagPrefix = "surface_color",
                            contactCardOpacity = contactCardOpacity
                        )

                        ColorCustomizerSection(
                            title = if (currentLanguage == "de") "Warnfarbe (Wischen / Aktion)" else "Swipe / Action Warning Color",
                            colorValue = customDelete,
                            onColorChanged = { viewModel.updateDeleteColor(it) },
                            tagPrefix = "delete_color",
                            contactCardOpacity = contactCardOpacity
                        )
                    }
                }
            }           }

            // Section: GLSL Shader Background Control
            Text(if (currentLanguage == "de") "GLSL-Shader-Hintergrund" else "GLSL Shader Background", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("shader_background_settings_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (currentLanguage == "de") "Flüssiger Shader-Hintergrund" else "Fluid Shader Background", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (currentLanguage == "de") "Dynamische Live-GPU-Hintergründe direkt unter Database-Layouts rendern" else "Render dynamic live GPU wallpapers directly under database layouts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = isShaderEnabled,
                            onCheckedChange = { viewModel.updateShaderBackgroundEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("shader_background_toggle")
                        )
                    }

                    if (isShaderEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text(if (currentLanguage == "de") "Visuellen Effekt wählen:" else "Select Visual Effect:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        val cyclablePresets = com.example.util.ShaderPresets.PRESETS.map { it.id } + listOf("custom")
                        val currentPresetInfo = com.example.util.ShaderPresets.PRESETS.firstOrNull { it.id == selectedShaderPresetState }
                        val currentShaderName = if (selectedShaderPresetState == "custom") {
                            if (currentLanguage == "de") "Benutzerdefinierter Code" else "Custom Code"
                        } else {
                            currentPresetInfo?.let { if (currentLanguage == "de") it.nameDe else it.nameEn } ?: (if (currentLanguage == "de") "Unbekannt" else "Unknown")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val idx = cyclablePresets.indexOf(selectedShaderPresetState)
                                    val nextIdx = if (idx != -1) {
                                        (idx - 1 + cyclablePresets.size) % cyclablePresets.size
                                    } else {
                                        0
                                    }
                                    viewModel.updateSelectedShaderPreset(cyclablePresets[nextIdx])
                                },
                                modifier = Modifier.testTag("shader_backward_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = if (currentLanguage == "de") "Vorheriger Shader" else "Previous Shader"
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentShaderName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val idx = cyclablePresets.indexOf(selectedShaderPresetState)
                                    val nextIdx = if (idx != -1) {
                                        (idx + 1) % cyclablePresets.size
                                    } else {
                                        0
                                    }
                                    viewModel.updateSelectedShaderPreset(cyclablePresets[nextIdx])
                                },
                                modifier = Modifier.testTag("shader_forward_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = if (currentLanguage == "de") "Nächster Shader" else "Next Shader"
                                )
                            }
                        }

                        if (selectedShaderPresetState == "custom") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                            
                            Text(if (currentLanguage == "de") "Eingefügtes GLSL-Fragment-Skript:" else "Pasted GLSL fragment script:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (currentLanguage == "de") {
                                    "Fügen Sie unten einen Standard-GLSL-Fragment-Shader oder Shadertoy-kompatiblen Code ein. Er wird automatisch kompiliert und in Echtzeit aktualisiert."
                                } else {
                                    "Paste a standard GLSL Fragment Shader or Shadertoy compliant code below. It will automatically compile and update in real-time."
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            var pasteCodeInput by remember(customShaderCodeState) {
                                mutableStateOf(customShaderCodeState.ifBlank { com.example.util.ShaderPresets.PRESET_CUSTOM_DEFAULT })
                            }

                            OutlinedTextField(
                                value = pasteCodeInput,
                                onValueChange = { pasteCodeInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .testTag("custom_shader_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                placeholder = { Text(if (currentLanguage == "de") "Fügen Sie hier das Shadertoy-mainImage-Skript ein..." else "Paste Shadertoy mainImage script here...") }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.updateCustomShaderCode(pasteCodeInput)
                                        val msg = if (currentLanguage == "de") "Shader-Parameter kompiliert!" else "Shader parameters compiled!"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).testTag("save_shader_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (currentLanguage == "de") "Kompilieren & Anwenden" else "Compile & Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        pasteCodeInput = com.example.util.ShaderPresets.PRESET_CUSTOM_DEFAULT
                                        viewModel.updateCustomShaderCode(com.example.util.ShaderPresets.PRESET_CUSTOM_DEFAULT)
                                        val msg = if (currentLanguage == "de") "Zurückgesetzt auf Standard-Plasma." else "Reset to default plasma."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("reset_shader_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (currentLanguage == "de") "Standardzurücksetzung" else "Reset Default", fontSize = 12.sp)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                            var newShaderName by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            OutlinedTextField(
                                value = newShaderName,
                                onValueChange = { newShaderName = it },
                                label = { Text(if (currentLanguage == "de") "Aktuellen Code speichern unter..." else "Save Current Code As...", fontSize = 11.sp) },
                                placeholder = { Text(if (currentLanguage == "de") "z. B. Neon-Welle" else "e.g., Neon Wave", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).testTag("save_shader_name_input"),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                            Button(
                                onClick = {
                                    if (newShaderName.isNotBlank()) {
                                        val nameTrimmed = newShaderName.trim()
                                        viewModel.saveCustomShader(nameTrimmed, pasteCodeInput)
                                        val msg = if (currentLanguage == "de") "Benutzerdefinierten Shader gespeichert: $nameTrimmed" else "Saved custom shader: $nameTrimmed"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        newShaderName = ""
                                    } else {
                                        val msg = if (currentLanguage == "de") "Bitte geben Sie zuerst einen Namen ein" else "Please enter a name first"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = newShaderName.isNotBlank(),
                                modifier = Modifier.align(Alignment.CenterVertically).testTag("save_custom_preset_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (currentLanguage == "de") "Sparen" else "Save", fontSize = 12.sp)
                            }
                        }

                        if (savedShaderNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            var savedShadersTabState by remember { mutableStateOf(0) } // 0 = Display, 1 = Hide
                            
                            TabRow(
                                selectedTabIndex = savedShadersTabState,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("saved_shaders_tab_row")
                            ) {
                                Tab(
                                    selected = savedShadersTabState == 0,
                                    onClick = { savedShadersTabState = 0 },
                                    text = { Text(if (currentLanguage == "de") "Gespeicherte anzeigen" else "Display Saved", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("tab_display_saved")
                                )
                                Tab(
                                    selected = savedShadersTabState == 1,
                                    onClick = { savedShadersTabState = 1 },
                                    text = { Text(if (currentLanguage == "de") "Liste ausblenden" else "Hide List", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("tab_hide_saved")
                                )
                            }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                if (savedShadersTabState == 0) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("saved_shaders_display_card"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f * contactCardOpacity),
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            savedShaderNames.sorted().forEach { shaderName ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = shaderName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                val loadedCode = viewModel.getSavedShaderCode(shaderName)
                                                                if (loadedCode.isNotBlank()) {
                                                                    pasteCodeInput = loadedCode
                                                                    viewModel.updateCustomShaderCode(loadedCode)
                                                                    Toast.makeText(context, "Loaded shader: $shaderName", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                            .padding(vertical = 4.dp)
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteSavedShader(shaderName)
                                                            Toast.makeText(context, "Deleted shader: $shaderName", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp).testTag("delete_shader_$shaderName")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Shader",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (currentLanguage == "de") "Karten- & Panel-Deckkraft" else "Card & Panel Opacity", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${(contactCardOpacity * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(if (currentLanguage == "de") "Passen Sie die Transparenz an, damit der flüssige Shader-Hintergrund durch Ihre Kontakte, Protokolle und Einstellungen schimmert." else "Adjust transparency to let the fluid shader background shine through your contacts, logs, and settings panels.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = contactCardOpacity,
                            onValueChange = { viewModel.updateContactCardOpacity(it) },
                            valueRange = 0.0f..1.0f,
                            modifier = Modifier.fillMaxWidth().testTag("contact_card_opacity_slider")
                        )
                    }
                }
            }

            // Section 2: Permissions and Smart Syncing
            Text(if (currentLanguage == "de") "Intelligente CRM-Protokollierung" else "Smart CRM Logs Tracking", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (currentLanguage == "de") "Automatisch protokollierte Interaktionen" else "Auto-Logged Relationship Tracking", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (currentLanguage == "de") {
                            "Nexus CRM erfasst Anruf- und SMS-Zähler sicher im Hintergrund. Wenn protokollierte Nummern mit bestehenden Kontaktdetails übereinstimmen, wird automatisch ein System-Interaktionsprotokoll in die Kontakt-Chronik eingetragen."
                        } else {
                            "Nexus CRM watches call and sms counters safely on system files. When logged numbers match an established contact details, an automated System Interaction Log enters the contact timeline."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 18.sp
                    )

                    // Call Log toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (currentLanguage == "de") "Anruferkennung-Abgleich" else "Call Logs Matching", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            val accessLabel = if (currentLanguage == "de") {
                                if (hasCallLogPermission) "Zugriff gewährt" else "Klicken Sie hier, um den Anrufzugriff zu autorisieren"
                            } else {
                                if (hasCallLogPermission) "Access granted" else "Click to authorize calls access"
                            }
                            Text(accessLabel, fontSize = 11.sp, color = if (hasCallLogPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                        Button(
                            onClick = {
                                if (!hasCallLogPermission) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_SMS))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasCallLogPermission) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (hasCallLogPermission) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (hasCallLogPermission) {
                                    if (currentLanguage == "de") "Autorisiert" else "Authorized"
                                } else {
                                    if (currentLanguage == "de") "Zulassen" else "Grant"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // SMS toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (currentLanguage == "de") "SMS-Protokoll-Abgleich" else "SMS Messages Log Matching", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            val smsLabel = if (currentLanguage == "de") {
                                if (hasSmsPermission) "Zugriff gewährt" else "Klicken Sie hier, um den SMS-Zugriff zu autorisieren"
                            } else {
                                if (hasSmsPermission) "Access granted" else "Click to authorize letters logs matching"
                            }
                            Text(smsLabel, fontSize = 11.sp, color = if (hasSmsPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                        Button(
                            onClick = {
                                if (!hasSmsPermission) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_SMS))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasSmsPermission) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (hasSmsPermission) MaterialTheme.colorScheme.onSecondaryContainer else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (hasSmsPermission) {
                                    if (currentLanguage == "de") "Autorisiert" else "Authorized"
                                } else {
                                    if (currentLanguage == "de") "Zulassen" else "Grant"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Manual log sync button
                    Button(
                        onClick = {
                            if (hasSmsPermission || hasCallLogPermission) {
                                val msg = if (currentLanguage == "de") "Interaktionsprotokoll scannen..." else "Scanning interactions log..."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                GlobalScope.launch {
                                    try {
                                        val workManager = androidx.work.WorkManager.getInstance(context)
                                        val request = androidx.work.OneTimeWorkRequestBuilder<InteractionTrackingWorker>().build()
                                        workManager.enqueue(request)
                                    } catch (e: Exception) {
                                        Log.e("SettingsScreen", "Failed manual scan trigger", e)
                                    }
                                }
                            } else {
                                val msg = if (currentLanguage == "de") "Bitte erteilen Sie zuerst die Anruf-/SMS-Berechtigungen" else "Please grant call/SMS permissions first"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentLanguage == "de") "Sofortigen Protokoll-Scan starten" else "Trigger Immediate Log Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section: Outgoing SMTP & Google Gmail API Mail Settings
            Text(if (currentLanguage == "de") "Automatisiertes E-Mail-Hintergrund-Gateway" else "Automated Background Email Gateway", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            // Card 1: Google Gmail Serverless API (Recommended)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("gmail_api_gateway_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (googleAccountEmailState.isNotBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f * contactCardOpacity) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f * contactCardOpacity),
                    contentColor = if (googleAccountEmailState.isNotBlank()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (googleAccountEmailState.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (currentLanguage == "de") "Google Gmail API Integration" else "Google Gmail API Integration", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        if (googleAccountEmailState.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(if (currentLanguage == "de") "VERBUNDEN" else "CONNECTED", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }

                    Text(
                        text = if (currentLanguage == "de") {
                            "Verbinden Sie sich direkt mit Ihrem Google-Konto, um automatische, geplante Hintergrund-E-Mails sicher über die Gmail-API von Google zu versenden. Dies ist der zuverlässigste Weg, um Spam-Filter zu umgehen. Bitte geben Sie unten Ihre Web-OAuth-Zugangsdaten ein."
                        } else {
                            "Connect directly with your Google Account to send automatic scheduled background emails securely via Google's Gmail API. This is the most reliable way to avoid spam filters. Please enter your custom Web OAuth credentials below."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )

                    var showSetupGuide by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = googleOAuthClientIdState,
                        onValueChange = {
                            googleOAuthClientIdState = it
                            preferences.googleOAuthClientId = it
                        },
                        label = { Text(if (currentLanguage == "de") "Google OAuth-Client-ID" else "Google OAuth Client ID") },
                        placeholder = { Text(if (currentLanguage == "de") "z. B. 123-abc.apps.googleusercontent.com" else "e.g. 123-abc.apps.googleusercontent.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_google_client_id")
                    )

                    OutlinedTextField(
                        value = googleOAuthClientSecretState,
                        onValueChange = {
                            googleOAuthClientSecretState = it
                            preferences.googleOAuthClientSecret = it
                        },
                        label = { Text(if (currentLanguage == "de") "Google OAuth-Client-Geheimnis" else "Google OAuth Client Secret") },
                        placeholder = { Text(if (currentLanguage == "de") "z. B. GOCSPX-xxxxxxxxx" else "e.g. GOCSPX-xxxxxxxxx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_google_client_secret")
                    )

                    TextButton(
                        onClick = { showSetupGuide = !showSetupGuide },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (showSetupGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showSetupGuide) {
                                if (currentLanguage == "de") "OAuth-Einrichtungsanleitung ausblenden" else "Hide OAuth Setup Instructions"
                            } else {
                                if (currentLanguage == "de") "Wie erhalte ich eine Client-ID und ein Client-Geheimnis?" else "How to get a Client ID & Client Secret?"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showSetupGuide) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (currentLanguage == "de") "1. Öffnen Sie die Google Cloud Console (console.cloud.google.com)." else "1. Go to Google Cloud Console (console.cloud.google.com).", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (currentLanguage == "de") "2. Erstellen Sie ein Projekt, gehen Sie zu „APIs & Dienste“ -> „OAuth-Zustimmungsbildschirm“, konfigurieren Sie ihn (Extern) und fügen Sie den Bereich hinzu: https://www.googleapis.com/auth/gmail.send" else "2. Create a Project, go to \"APIs & Services\" -> \"OAuth consent screen\", configure it (External), and add the scope: https://www.googleapis.com/auth/gmail.send", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (currentLanguage == "de") "3. Gehen Sie zu „Anmeldedaten“ -> „Anmeldedaten erstellen“ -> „OAuth-Client-ID“." else "3. Go to \"Credentials\" -> \"Create Credentials\" -> \"OAuth client ID\".", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (currentLanguage == "de") "4. Wählen Sie den Anwendungstyp: Webanwendung. Fügen Sie eine autorisierte Weiterleitungs-URI hinzu: com.aistudio.oauth://callback" else "4. Select Application Type: Web Application. Add an Authorized Redirect URI: com.aistudio.oauth://callback", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                Text(if (currentLanguage == "de") "5. Speichern und kopieren Sie die generierte Client-ID und das Client-Geheimnis in die Textfelder oben." else "5. Save and copy the generated Client ID and Client Secret into the text fields above.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (googleAccountEmailState.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(googleAccountNameState, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(googleAccountEmailState, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                IconButton(
                                    onClick = {
                                        googleAccountEmailState = ""
                                        googleAccountNameState = ""
                                        preferences.googleAccountEmail = ""
                                        preferences.googleAccountName = ""
                                        preferences.googleRefreshToken = ""
                                        preferences.googleAccessToken = ""
                                        preferences.googleTokenExpiry = 0L
                                        val msg = if (currentLanguage == "de") "Google-Konto getrennt." else "Google Account disconnected."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Disconnect Account", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (googleOAuthClientIdState.trim().isBlank() || googleOAuthClientSecretState.trim().isBlank()) {
                                    val msg = if (currentLanguage == "de") "Bitte geben Sie zuerst Ihre Google-OAuth-Client-ID und Ihr Client-Geheimnis ein." else "Please enter your Google OAuth Client ID and Secret first."
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                } else {
                                    val success = com.example.util.GoogleOAuthManager.initiateOAuthFlow(context)
                                    if (!success) {
                                        val msg = if (currentLanguage == "de") "Google-Login konnte nicht gestartet werden. Stellen Sie sicher, dass die Client-ID-Details korrekt sind." else "Failed to start Google Login. Ensure Client ID details are correct."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (currentLanguage == "de") "Gmail-Konto verbinden (Web-Flow)" else "Connect Gmail Account (Web Flow)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Card 2: Manual Outgoing SMTP Configuration (Fallback)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("smtp_config_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (googleAccountEmailState.isBlank()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f * contactCardOpacity) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f * contactCardOpacity),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (googleAccountEmailState.isBlank()) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color.Transparent
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (currentLanguage == "de") "Manuelle SMTP-Einstellungen (Fallback)" else "Manual SMTP Settings (Fallback)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { showSmtpHelpDialog = true },
                                modifier = Modifier.size(24.dp).testTag("smtp_help_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Help,
                                    contentDescription = "SMTP Help Guide",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (googleAccountEmailState.isBlank()) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            ) {
                                Text(if (currentLanguage == "de") "AKTIVES Fallback" else "ACTIVE fallback", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }
                    }

                    Text(
                        text = if (currentLanguage == "de") {
                            "Oder konfigurieren Sie Standard-SMTP als Backup oder Alternative. Um automatische Hintergrund-E-Mails über SMTP auszuführen, geben Sie bitte Host/Port/Schlüssel Ihres eigenen Servers ein."
                        } else {
                            "Or configure standard SMTP as a backup or alternative. To run automatic background emails through SMTP, please input custom servers host/port/keys."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                smtpHostState = "smtp.gmail.com"
                                smtpPortState = "587"
                                smtpUseSslState = false
                                smtpUseTlsState = true
                                preferences.smtpHost = "smtp.gmail.com"
                                preferences.smtpPort = "587"
                                preferences.smtpUseSsl = false
                                preferences.smtpUseTls = true
                                val msg = if (currentLanguage == "de") "Gmail-SMTP-Voreinstellung angewendet!" else "Gmail SMTP preset applied!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (currentLanguage == "de") "Gmail-Voreinstellung" else "Gmail Preset", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                smtpHostState = "sandbox.smtp.mailtrap.io"
                                smtpPortState = "2525"
                                smtpUseSslState = false
                                smtpUseTlsState = true
                                preferences.smtpHost = "sandbox.smtp.mailtrap.io"
                                preferences.smtpPort = "2525"
                                preferences.smtpUseSsl = false
                                preferences.smtpUseTls = true
                                val msg = if (currentLanguage == "de") "Mailtrap-SMTP-Voreinstellung angewendet!" else "Mailtrap SMTP preset applied!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (currentLanguage == "de") "Mailtrap-Voreinstellung" else "Mailtrap Preset", fontSize = 10.sp)
                        }
                    }

                    OutlinedTextField(
                        value = smtpHostState,
                        onValueChange = {
                            smtpHostState = it
                            preferences.smtpHost = it
                        },
                        label = { Text(if (currentLanguage == "de") "SMTP-Host-Server" else "SMTP Host Server") },
                        placeholder = { Text(if (currentLanguage == "de") "z. B. smtp.gmail.com" else "e.g. smtp.gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_smtp_host")
                    )

                    OutlinedTextField(
                        value = smtpPortState,
                        onValueChange = {
                            smtpPortState = it
                            preferences.smtpPort = it
                        },
                        label = { Text(if (currentLanguage == "de") "SMTP-Port-Nummer" else "SMTP Port Number") },
                        placeholder = { Text(if (currentLanguage == "de") "z. B. 587 oder 465" else "e.g. 587 or 465") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_smtp_port")
                    )

                    OutlinedTextField(
                        value = smtpUsernameState,
                        onValueChange = {
                            smtpUsernameState = it
                            preferences.smtpUsername = it
                        },
                        label = { Text(if (currentLanguage == "de") "SMTP-Benutzername / E-Mail" else "SMTP Username / Email") },
                        placeholder = { Text(if (currentLanguage == "de") "z. B. name@gmail.com" else "e.g. yourname@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_smtp_username")
                    )

                    OutlinedTextField(
                        value = smtpPasswordState,
                        onValueChange = {
                            smtpPasswordState = it
                            preferences.smtpPassword = it
                        },
                        label = { Text(if (currentLanguage == "de") "SMTP-Passwort / App-Passwort" else "SMTP Password / App Password") },
                        placeholder = { Text(if (currentLanguage == "de") "z. B. abcd efgh ijkl mnop" else "e.g. abcd efgh ijkl mnop") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_smtp_password")
                    )

                    OutlinedTextField(
                        value = smtpSenderState,
                        onValueChange = {
                            smtpSenderState = it
                            preferences.smtpSender = it
                        },
                        label = { Text(if (currentLanguage == "de") "Absender-Anzeigename / Von (Optional)" else "Sender Display-Name / From (Optional)") },
                        placeholder = { Text(if (currentLanguage == "de") "z. B. name@gmail.com" else "e.g. yourname@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_smtp_sender")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = smtpUseSslState,
                                onCheckedChange = {
                                    smtpUseSslState = it
                                    preferences.smtpUseSsl = it
                                }
                            )
                            Text(if (currentLanguage == "de") "SSL-Verbindung verwenden" else "Use SSL Connection", fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = smtpUseTlsState,
                                onCheckedChange = {
                                    smtpUseTlsState = it
                                    preferences.smtpUseTls = it
                                }
                            )
                            Text(if (currentLanguage == "de") "STARTTLS-Sicherheit verwenden" else "Use STARTTLS security", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (smtpUsernameState.isBlank() || smtpPasswordState.isBlank()) {
                                val msg = if (currentLanguage == "de") "Geben Sie zuerst Benutzername & Passwort ein" else "Provide Username & Password first to test"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                val msg = if (currentLanguage == "de") "Verifizierte SMTP-Verbindungseinstellungen..." else "Verifying SMTP connection configs..."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                        try {
                                            val props = Properties().apply {
                                                put("mail.smtp.host", smtpHostState)
                                                put("mail.smtp.port", smtpPortState)
                                                put("mail.smtp.auth", "true")
                                                if (smtpUseSslState) {
                                                    put("mail.smtp.socketFactory.port", smtpPortState)
                                                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                                                    put("mail.smtp.socketFactory.fallback", "false")
                                                }
                                                if (smtpUseTlsState) {
                                                    put("mail.smtp.starttls.enable", "true")
                                                }
                                            }

                                            val session = Session.getInstance(props, object : Authenticator() {
                                                override fun getPasswordAuthentication(): PasswordAuthentication {
                                                    return PasswordAuthentication(smtpUsernameState, smtpPasswordState)
                                                }
                                            })

                                            val transport = session.transport
                                            transport.connect()
                                            transport.close()
                                            val successMsg = if (currentLanguage == "de") "SMTP-Verbindung erfolgreich verifiziert!" else "SMTP Connection verified successfully!"
                                            true to successMsg
                                        } catch (e: Exception) {
                                            false to (e.localizedMessage ?: "Unknown connection validation failure")
                                        }
                                    }
                                    Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_test_smtp_auth")
                    ) {
                        Icon(Icons.Default.SettingsEthernet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (currentLanguage == "de") "Verbindungsauthentifizierung testen" else "Test Connection Authentication", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 3: Import / Export backup utilities
            Text(if (currentLanguage == "de") "Datensicherung & Portabilität" else "Data Backup & Portability", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (currentLanguage == "de") "Sichere lokale Backups" else "Secure Local Backups", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(if (currentLanguage == "de") "Exportieren oder importieren Sie alle Elemente (Kontakte, benutzerdefinierte Verknüpfungen, Koordinaten, Chronik-Tabellen und benutzerdefinierte Shader-Voreinstellungen) geschützt durch eine sichere AES-128-Verschlüsselung." else "Export or import all elements (contacts, custom shortcuts, coordinates, timeline sheets, and custom shader presets) protected by secure AES-128 encryption.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { exportLauncher.launch("crm_backup_${System.currentTimeMillis()}.json") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_export_db"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (currentLanguage == "de") "Backup exportieren" else "Export Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_import_db"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (currentLanguage == "de") "Backup importieren" else "Import Backup", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Section 4: About & Version info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = contactCardOpacity),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(if (currentLanguage == "de") "Über Omni Connect" else "About Omni Connect", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (currentLanguage == "de") "Entwickelt als modularer, datenschutzorientierter, beziehungsbewusster und sicherer Offline-Dienst. Alle Protokolle, biometrischen Sperren, Koordinaten und Kontaktordner bleiben auf diesem Gerät lokal sicher verschlüsselt." else "Engineered as an offline-first modular, relationship-aware secure intelligence suite. All logs, biometric locks, coordinates, and contact folders remain safely encrypted locally on this device.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (currentLanguage == "de") "Build-Version: 1.0.4-NexusSecure. Room-Datenbank aktiv." else "Build Version: 1.0.4-NexusSecure. Room Database active.", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // Passcode Configuration Dialog Sheet
    if (showEditPinDialog) {
        var tempRealPin by remember { mutableStateOf(realPinState) }
        var tempDecoyPin by remember { mutableStateOf(decoyPinState) }

        AlertDialog(
            onDismissRequest = { showEditPinDialog = false },
            title = { Text(if (currentLanguage == "de") "Tastatur-PINs konfigurieren" else "Configure Keypad Pins") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (currentLanguage == "de") "Beide PINs müssen exakt 4-stellige Zahlen sein. Falsche PINs auf dem Sicherheitsbildschirm schließen die Anwendung." else "Both PINs must be exact 4-digit numbers. Wrong PINs on security screen close the application.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    
                    OutlinedTextField(
                        value = tempRealPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) tempRealPin = it },
                        label = { Text(if (currentLanguage == "de") "Echte Tresor-PIN" else "Real Vault PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempDecoyPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) tempDecoyPin = it },
                        label = { Text(if (currentLanguage == "de") "Schein-PIN (Scheindaten)" else "Plausible Decoy PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempRealPin.length == 4 && tempDecoyPin.length == 4) {
                            if (tempRealPin == tempDecoyPin) {
                                val msg = if (currentLanguage == "de") "Die echte und die Schein-PIN dürfen nicht identisch sein!" else "Real and Decoy pin cannot be identical!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                preferences.realPin = tempRealPin
                                preferences.decoyPin = tempDecoyPin
                                realPinState = tempRealPin
                                decoyPinState = tempDecoyPin
                                showEditPinDialog = false
                                val msg = if (currentLanguage == "de") "Zugangscodes aktualisiert" else "Passcodes updated"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val msg = if (currentLanguage == "de") "PINs müssen genau 4 Ziffern lang sein!" else "Pins must be exactly 4 digits!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text(if (currentLanguage == "de") "PINs bestätigen" else "Confirm Pins", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showEditPinDialog = false }) { Text(if (currentLanguage == "de") "Abbrechen" else "Cancel") }
            }
        )
    }

    // Import Options sheet: Overwrite database or Merging items
    if (showImportMergeDialog && selectedImportUri != null) {
        AlertDialog(
            onDismissRequest = { showImportMergeDialog = false },
            title = { Text(if (currentLanguage == "de") "Datenbank-Importmodus" else "Database Import Mode") },
            text = {
                Text(if (currentLanguage == "de") "Möchten Sie die importierten Kontakte mit Ihren bestehenden Datenbankkontakten zusammenführen (Merge) oder die vorhandenen Kontakte überschreiben und bestehende Ordner löschen (Overwrite)?" else "Would you like to Merge the imported contacts with your existing database contacts, or Overwrite standard contacts and clear the existing folders?", fontSize = 13.sp)
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    context.contentResolver.openInputStream(selectedImportUri!!)?.use { inputStream ->
                                        val ok = BackupRestoreHelper.importFromStream(context, inputStream, mergeMode = true)
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            if (ok) {
                                                viewModel.refreshAllPreferences()
                                                val msg = if (currentLanguage == "de") "Kontakte erfolgreich zusammengeführt!" else "Contacts merged successfully!"
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            } else {
                                                val msg = if (currentLanguage == "de") "Import fehlgeschlagen. Ungültiges JSON-Modell." else "Import failed. Invalid JSON model."
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        val msg = if (currentLanguage == "de") "Zusammenführungsfehler: ${e.localizedMessage}" else "Merge error: ${e.localizedMessage}"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            showImportMergeDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (currentLanguage == "de") "Zusammenführen" else "Merge", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    TextButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    context.contentResolver.openInputStream(selectedImportUri!!)?.use { inputStream ->
                                        val ok = BackupRestoreHelper.importFromStream(context, inputStream, mergeMode = false)
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            if (ok) {
                                                viewModel.refreshAllPreferences()
                                                val msg = if (currentLanguage == "de") "Bestehende Datenbanken erfolgreich überschrieben!" else "Existing databases overwritten successfully!"
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            } else {
                                                val msg = if (currentLanguage == "de") "Import fehlgeschlagen. JSON-Format prüfen." else "Import failed. Check JSON format."
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        val msg = if (currentLanguage == "de") "Importfehler: ${e.localizedMessage}" else "Import error: ${e.localizedMessage}"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            showImportMergeDialog = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (currentLanguage == "de") "Überschreiben" else "Overwrite", color = Color(0xFFFF5252))
                    }
                }
            }
        )
    }

    if (showSmtpHelpDialog) {
        AlertDialog(
            onDismissRequest = { showSmtpHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentLanguage == "de") "Anleitung für Google-App-Passwörter" else "Google App Passwords Guide", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (currentLanguage == "de") {
                            "Um ein Gmail-SMTP-Passwort zu erhalten, müssen Sie in Ihrem Google-Konto ein 16-stelliges App-Passwort generieren."
                        } else {
                            "To get a Gmail SMTP password, you must generate a 16-character App password from your Google Account."
                        },
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text(
                        text = if (currentLanguage == "de") "Voraussetzungen" else "Requirements",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (currentLanguage == "de") {
                            "• Die Bestätigung in zwei Schritten muss für Ihr Google-Konto aktiviert sein."
                        } else {
                            "• You must have 2-Step Verification enabled on your Google Account."
                        },
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text(
                        text = if (currentLanguage == "de") "Schritt-für-Schritt-Anleitung" else "Step-by-Step Guide",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val steps = if (currentLanguage == "de") {
                        listOf(
                            "Öffnen Sie die Sicherheitseinstellungen Ihres Google-Kontos.",
                            "Klicken Sie im Bereich „Sicherheit bei der Anmeldung“ auf „Bestätigung in zwei Schritten“ und führen Sie die Einrichtung durch, falls noch nicht geschehen.",
                            "Navigieren Sie nach der Aktivierung zur Seite „App-Passwörter“ (nutzen Sie die Suchleiste im Google-Konto und suchen Sie nach „App-Passwörter“).",
                            "Geben Sie im Feld „App-Name“ einen leicht identifizierbaren Namen ein, z. B. SMTP oder den Namen dieser App.",
                            "Klicken Sie auf Erstellen.",
                            "Google zeigt ein 16-stelliges App-Passwort an. Kopieren Sie dieses Passwort sorgfältig, da es später nicht mehr angezeigt wird.",
                            "Das ist alles! Sie haben jetzt Ihr SMTP-Passwort."
                        )
                    } else {
                        listOf(
                            "Go to your Google Account Security settings.",
                            "Under the \"How you sign in to Google\" section, click on 2-Step Verification and complete the setup if you haven’t already.",
                            "Once enabled, go to the App Passwords page. (use the search option; search \"App passwords\")",
                            "Under the \"App name\" field, type something easily identifiable like SMTP or the name of the app/website you are connecting.",
                            "Click Create.",
                            "Google will display a 16-character App Password. Copy this password, as it won't be shown again.",
                            "That's it, you now have your SMTP password!"
                        )
                    }

                    steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = step,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text(
                        text = if (currentLanguage == "de") "Einrichtungs-Checkliste" else "Setup Checklist",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (currentLanguage == "de") {
                            "1. Jetzt können Sie die erforderlichen Felder ausfüllen.\n" +
                            "2. Geben Sie Ihre E-Mail-Adresse ein, z. B. JohnSmith@Gmail.com\n" +
                            "3. Fügen Sie Ihr App-Passwort in das Feld für das SMTP-Passwort ein.\n" +
                            "4. Drücken Sie auf „Verbindungsauthentifizierung testen“ – Sie sollten eine erfolgreiche Bestätigungsmeldung erhalten.\n\n" +
                            "Herzlichen Glückwunsch, Sie können jetzt im Kontaktdetails-Bildschirm E-Mails planen!"
                        } else {
                            "1. Now you can fill out the required fields needed.\n" +
                            "2. Enter your email address eg. JohnSmith@Gmail.com\n" +
                            "3. Paste your App Password in the SMTP password box.\n" +
                            "4. Press \"Test connection authentication\" - you should see a successful validation message.\n\n" +
                            "Congratulations, you can now Schedule emails in the Contact details screen!"
                        },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSmtpHelpDialog = false },
                    modifier = Modifier.testTag("smtp_help_close_button")
                ) {
                    Text(if (currentLanguage == "de") "Verstanden" else "Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ColorCustomizerSection(
    title: String,
    colorValue: Int,
    onColorChanged: (Int) -> Unit,
    tagPrefix: String,
    contactCardOpacity: Float = 1.0f
) {
    val r = (colorValue shr 16) and 0xFF
    val g = (colorValue shr 8) and 0xFF
    val b = colorValue and 0xFF

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f * contactCardOpacity),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                // Color preview box
                Box(
                    modifier = Modifier
                        .size(48.dp, 24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(colorValue))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // R Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "R: $r",
                    fontSize = 11.sp,
                    modifier = Modifier.width(40.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
                Slider(
                    value = r.toFloat(),
                    onValueChange = { newVal ->
                        onColorChanged(packRgb(newVal.toInt(), g, b))
                    },
                    valueRange = 0f..255f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_r_slider")
                )
            }

            // G Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "G: $g",
                    fontSize = 11.sp,
                    modifier = Modifier.width(40.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C)
                )
                Slider(
                    value = g.toFloat(),
                    onValueChange = { newVal ->
                        onColorChanged(packRgb(r, newVal.toInt(), b))
                    },
                    valueRange = 0f..255f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_g_slider")
                )
            }

            // B Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "B: $b",
                    fontSize = 11.sp,
                    modifier = Modifier.width(40.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Slider(
                    value = b.toFloat(),
                    onValueChange = { newVal ->
                        onColorChanged(packRgb(r, g, newVal.toInt()))
                    },
                    valueRange = 0f..255f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_b_slider")
                )
            }
        }
    }
}

private fun packRgb(r: Int, g: Int, b: Int): Int {
    return (0xFF shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)
}
