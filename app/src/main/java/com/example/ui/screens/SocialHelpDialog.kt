package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SocialPlatformInfo(
    val platform: String,
    val format: String,
    val keyDetail: String,
    val keyDetailDe: String,
    val starter: String
)

val socialPlatformsList = listOf(
    SocialPlatformInfo("Signal", "https://signal.me/#p/+[countrycode][number]", "Uses phone number +[countrycode][number]", "Verwendet die Telefonnummer +[Ländercode][Nummer]", "https://signal.me/#p/+"),
    SocialPlatformInfo("Facebook", "facebook.com/[username]", "Custom username; falls back to numeric ID", "Benutzerdefinierter Benutzername; weicht auf numerische ID aus", "facebook.com/"),
    SocialPlatformInfo("Instagram", "instagram.com/[username]", "Uses @mention style handle", "Verwendet den Handle-Stil mit @Symbol", "instagram.com/"),
    SocialPlatformInfo("Twitter (X)", "x.com/[username]", "Either domain works (x.com or twitter.com)", "Beide Domains funktionieren (x.com oder twitter.com)", "x.com/"),
    SocialPlatformInfo("TikTok", "tiktok.com/@[username]", "Includes @ symbol in URL path", "Enthält das @-Symbol im URL-Pfad", "tiktok.com/@"),
    SocialPlatformInfo("LinkedIn", "linkedin.com/in/[username]", "Custom public profile URL (/in/)", "Benutzerdefinierte öffentliche Profil-URL (/in/)", "linkedin.com/in/"),
    SocialPlatformInfo("YouTube", "youtube.com/@[handle]", "Official handle format; fallback /channel/[ID]", "Offizielles Handle-Format; Fallback /channel/[ID]", "youtube.com/@"),
    SocialPlatformInfo("WhatsApp", "wa.me/[number]", "No +, spaces, or dashes: e.g. 61412345678", "Kein +, keine Leerzeichen oder Bindestriche: z.B. 49171234567", "wa.me/"),
    SocialPlatformInfo("Snapchat", "snapchat.com/add/[username]", "Uses /add/ to prompt adding", "Verwendet /add/ für eine Hinzufügen-Aufforderung", "snapchat.com/add/"),
    SocialPlatformInfo("Telegram", "t.me/[username]", "Username required; phone number not used in URL", "Benutzername erforderlich; Telefonnummer wird in URL nicht verwendet", "t.me/")
)

@Composable
fun SocialHelpDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val appLanguage = remember(context) {
        com.example.data.SettingsPreferences(context).appLanguage
    }

    val dialogTitle = if (appLanguage == "de") "Social-Link-Verzeichnis" else "Social Link Directory"
    val introText = if (appLanguage == "de") {
        "Beachten Sie die folgenden Plattform-Richtlinien zur Formatierung Ihrer Social-Media-Symbole. Tippen Sie auf das Kopiersymbol neben einem Feld, um es in der Zwischenablage zu speichern."
    } else {
        "Refer to the platform guidelines below to format your social icons. Tap a copy icon next to a field to save it to your clipboard."
    }
    val formatTitle = if (appLanguage == "de") "Vollständiges Format-Muster" else "Complete Format Pattern"
    val baseLinkTitle = if (appLanguage == "de") "Basis-Link-URL-Präfix" else "Base Link URL prefix"
    val closeBtnText = if (appLanguage == "de") "Referenz schließen" else "Close Reference"

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(dialogTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = introText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                socialPlatformsList.forEach { platformInfo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = platformInfo.platform,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = if (appLanguage == "de") platformInfo.keyDetailDe else platformInfo.keyDetail,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 2.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                            )

                            // Complete Format Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formatTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = platformInfo.format,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(platformInfo.format))
                                        val m = if (appLanguage == "de") "Muster in die Zwischenablage kopiert" else "Copied Pattern to Clipboard"
                                        Toast.makeText(context, m, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy entire pattern text template",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Prefix Starter Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = baseLinkTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = platformInfo.starter,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(platformInfo.starter))
                                        val m = if (appLanguage == "de") "Basis-Präfix in die Zwischenablage kopiert" else "Copied Starter Prefix to Clipboard"
                                        Toast.makeText(context, m, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy base starter link prefix",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(closeBtnText)
            }
        }
    )
}

