package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
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

data class FinderPlatformInfo(
    val title: String,
    val iconEmoji: String,
    val intro: String,
    val introDe: String,
    val steps: List<String>,
    val stepsDe: List<String>,
    val copyableText: String? = null,
    val copyableLabel: String? = null,
    val copyableLabelDe: String? = null
)

val howToFindPlatforms = listOf(
    FinderPlatformInfo(
        title = "Signal",
        iconEmoji = "📱",
        intro = "Signal doesn't use a dedicated username for links. Instead, it utilizes your phone number or an encrypted link from your profile.",
        introDe = "Signal verwendet keinen dedizierten Benutzernamen für Links. Stattdessen nutzt es Ihre Telefonnummer oder einen verschlüsselten Link aus Ihrem Profil.",
        steps = listOf(
            "For your phone number: The format is https://signal.me/#p/+[countrycode][number]. For example, https://signal.me/#p/+61412345678.",
            "For an encrypted link: Go to Signal Settings → tap your profile → QR Code or Link → then Copy Link. This provides a unique, resettable URL."
        ),
        stepsDe = listOf(
            "Für Ihre Telefonnummer: Das Format ist https://signal.me/#p/+[Ländercode][Nummer]. Zum Beispiel: https://signal.me/#p/+491701234567.",
            "Für einen verschlüsselten Link: Gehen Sie zu Signal-Einstellungen → tippen Sie auf Ihr Profil → QR-Code oder Link → Kopieren. Dies liefert eine eindeutige, zurücksetzbare URL."
        ),
        copyableText = "https://signal.me/#p/+",
        copyableLabel = "Copy prefix: https://signal.me/#p/+",
        copyableLabelDe = "Kopieren: https://signal.me/#p/+"
    ),
    FinderPlatformInfo(
        title = "Facebook",
        iconEmoji = "📘",
        intro = "Facebook offers a few different methods to copy your profile link:",
        introDe = "Facebook bietet verschiedene Methoden, um Ihren Profil-Link zu kopieren:",
        steps = listOf(
            "Desktop: Go to your profile and copy the link directly from your browser's address bar.",
            "Mobile App: Go to your profile, tap the three dots (...) below your cover photo, and select Copy Link to Profile."
        ),
        stepsDe = listOf(
            "Desktop: Gehen Sie auf Ihr Profil und kopieren Sie den Link direkt aus der Adresszeile des Browsers.",
            "Mobile App: Gehen Sie auf Ihr Profil, tippen Sie auf die drei Punkte (...) unter Ihrem Titelbild und wählen Sie 'Profil-Link kopieren'."
        ),
        copyableText = "facebook.com/",
        copyableLabel = "Copy: facebook.com/",
        copyableLabelDe = "Kopieren: facebook.com/"
    ),
    FinderPlatformInfo(
        title = "Instagram",
        iconEmoji = "📸",
        intro = "Find your profile link through the web search bar or internal share settings:",
        introDe = "Finden Sie Ihren Profil-Link über die Adressleiste des Browsers oder die Freigabeeinstellungen:',",
        steps = listOf(
            "Website: Go to your profile and copy the URL from the browser's address bar.",
            "App: Go to your profile, tap the Share profile button (near your bio), then tap Copy link."
        ),
        stepsDe = listOf(
            "Website: Gehen Sie auf Ihr Profil und kopieren Sie die URL aus der Adresszeile des Browsers.",
            "App: Gehen Sie auf Ihr Profil, tippen Sie auf 'Profil teilen' (in der Nähe Ihrer Bio) und wählen Sie 'Link kopieren'."
        ),
        copyableText = "instagram.com/",
        copyableLabel = "Copy: instagram.com/",
        copyableLabelDe = "Kopieren: instagram.com/"
    ),
    FinderPlatformInfo(
        title = "Twitter (X)",
        iconEmoji = "🐦",
        intro = "Quickly share your profile with either the modern x.com handle or classic twitter.com address:",
        introDe = "Teilen Sie Ihr Profil schnell mit dem modernen x.com oder dem klassischen twitter.com Format:",
        steps = listOf(
            "Desktop: Go to your profile and copy the URL from the browser's address bar.",
            "App: Go to your profile, tap the share icon (a small arrow), and select Copy link."
        ),
        stepsDe = listOf(
            "Desktop: Gehen Sie auf Ihr Profil und kopieren Sie die URL aus der Adresszeile des Browsers.",
            "App: Gehen Sie auf Ihr Profil, tippen Sie auf das Teilen-Symbol (kleiner Pfeil) und wählen Sie 'Link kopieren'."
        ),
        copyableText = "x.com/",
        copyableLabel = "Copy: x.com/",
        copyableLabelDe = "Kopieren: x.com/"
    ),
    FinderPlatformInfo(
        title = "TikTok",
        iconEmoji = "🎵",
        intro = "Ensure to include the required '@' symbol when defining your TikTok handle:",
        introDe = "Achten Sie darauf, das erforderliche '@'-Symbol anzugeben, wenn Sie Ihren TikTok-Handle definieren:",
        steps = listOf(
            "Website: Go to your profile and copy the URL from the browser's address bar.",
            "App: Go to your profile, tap the share icon (an arrow pointing right), and select Copy link."
        ),
        stepsDe = listOf(
            "Website: Gehen Sie auf Ihr Profil und kopieren Sie die URL aus der Adresszeile des Browsers.",
            "App: Gehen Sie auf Ihr Profil, tippen Sie auf das Teilen-Symbol (Pfeil nach rechts) und wählen Sie 'Link kopieren'."
        ),
        copyableText = "tiktok.com/@",
        copyableLabel = "Copy: tiktok.com/@",
        copyableLabelDe = "Kopieren: tiktok.com/@"
    ),
    FinderPlatformInfo(
        title = "LinkedIn",
        iconEmoji = "💼",
        intro = "Your LinkedIn profile link is your unique public profile URL found inside the contact profile panel:",
        introDe = "Ihr LinkedIn-Pfad ist Ihre eindeutige öffentliche Profil-URL, die sich im Kontaktprofil-Bereich befindet:",
        steps = listOf(
            "Desktop: Click the Me icon → View Profile → your public profile URL is displayed on the right-hand pane under \"Public profile & URL\".",
            "App: Tap your profile photo → tap the More icon (...) next to the \"Add section\" button → select Contact info → your public profile URL will be listed there."
        ),
        stepsDe = listOf(
            "Desktop: Klicken Sie auf das 'Ich'-Symbol → Profil anzeigen → Ihre öffentliche Profil-URL wird rechts unter 'Öffentliches Profil & URL' angezeigt.",
            "App: Tippen Sie auf Ihr Profilbild → tippen Sie auf die drei Punkte (...) neben 'Profilbereich hinzufügen' → wählen Sie Kontaktdaten aus."
        ),
        copyableText = "linkedin.com/in/",
        copyableLabel = "Copy: linkedin.com/in/",
        copyableLabelDe = "Kopieren: linkedin.com/in/"
    ),
    FinderPlatformInfo(
        title = "YouTube",
        iconEmoji = "▶️",
        intro = "Your YouTube channel link is the address of your channel page, formatted around your handle:",
        introDe = "Ihr YouTube-Kanallink ist das Format basierend auf Ihrem eindeutigen Handle-Namen:",
        steps = listOf(
            "Desktop: Click your profile picture → Your Channel → copy the URL from your browser's address bar.",
            "App: Tap your profile picture → Your Channel → tap the three dots (⁝) in the top-right → select Share and then Copy link."
        ),
        stepsDe = listOf(
            "Desktop: Klicken Sie auf Ihr Profilbild → Mein Kanal → kopieren Sie die URL aus der Adresszeile des Browsers.",
            "App: Tippen Sie auf Ihr Profilbild → Mein Kanal → tippen Sie oben rechts auf die drei Punkte (⁝) → Teilen und dann 'Link kopieren'."
        ),
        copyableText = "youtube.com/@",
        copyableLabel = "Copy: youtube.com/@",
        copyableLabelDe = "Kopieren: youtube.com/@"
    ),
    FinderPlatformInfo(
        title = "WhatsApp",
        iconEmoji = "💬",
        intro = "Standard personal WhatsApp accounts do not have public profile usernames. The wa.me/[number] format is the primary method.",
        introDe = "Standardmäßige persönliche WhatsApp-Konten haben keine öffentlichen Benutzernamen. Das wa.me/[Nummer]-Format ist die Hauptmethode.",
        steps = listOf(
            "Manual Format: The main shareable address is wa.me/[number]. For the number, omit any +, spaces, or dashes. For example, wa.me/61412345678.",
            "From the App (for Business Accounts): For WhatsApp Business accounts, you can go to Settings → Business tools → Short link."
        ),
        stepsDe = listOf(
            "Manuelles Format: Die Adresse lautet wa.me/[Nummer]. Lassen Sie alle +, Leerzeichen oder Striche weg (z.B. wa.me/491701234567).",
            "Über die App (für Business-Konten): Bei WhatsApp Business können Sie unter Einstellungen → Business-Tools → Kurzlink gehen."
        ),
        copyableText = "wa.me/",
        copyableLabel = "Copy prefix: wa.me/",
        copyableLabelDe = "Kopieren: wa.me/"
    ),
    FinderPlatformInfo(
        title = "Snapchat",
        iconEmoji = "👻",
        intro = "Snapchat has moved away from traditional profile URLs and officially announced it is sunsetting profile link configuration strings:",
        introDe = "Snapchat zieht sich von klassischen Profil-URLs zurück und stellt die Konfigurationen ein:",
        steps = listOf(
            "Main URL Format: While the classic format is snapchat.com/add/[username], note that official support is sunsetting.",
            "Official Method: Use Snapcodes or \"Share My Profile\" links generated within the app to add friends."
        ),
        stepsDe = listOf(
            "Haupt-URL-Format: Obwohl das klassische Format snapchat.com/add/[Benutzername] ist, wird der offizielle Support eingestellt.",
            "Offizielle Methode: Nutzen Sie Snapcodes oder die Funktion 'Mein Profil teilen' in der App, um Freunde hinzuzufügen."
        ),
        copyableText = "snapchat.com/add/",
        copyableLabel = "Copy: snapchat.com/add/",
        copyableLabelDe = "Kopieren: snapchat.com/add/"
    ),
    FinderPlatformInfo(
        title = "Telegram",
        iconEmoji = "✈️",
        intro = "Telegram profile links are based purely on your customized username, not your phone number:",
        introDe = "Telegram-Profil-Links basieren rein auf Ihrem benutzerdefinierten Namen, nicht auf der Telefonnummer:",
        steps = listOf(
            "Main URL Format: The format is t.me/[username] (e.g., t.me/YourUsername).",
            "From the App: Go to Settings → your username will be displayed. Long-press on it to copy the full t.me/[username] link."
        ),
        stepsDe = listOf(
            "Haupt-URL-Format: Das Format ist t.me/[Benutzername] (z.B. t.me/IhrBenutzername).",
            "Aus der App: Gehen Sie in die Einstellungen → Ihr Benutzername wird angezeigt. Halten Sie ihn gedrückt, um den Link zu kopieren."
        ),
        copyableText = "t.me/",
        copyableLabel = "Copy: t.me/",
        copyableLabelDe = "Kopieren: t.me/"
    )
)

@Composable
fun SocialHowToFindDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val appLanguage = remember(context) {
        com.example.data.SettingsPreferences(context).appLanguage
    }

    val dialogTitle = if (appLanguage == "de") "Profil-Links finden" else "How to Find Profile Links"
    val introTitleText = if (appLanguage == "de") {
        "Hier ist eine Kurzanleitung, wie Sie Ihren teilbaren Profil-Link für die einzelnen gängigen Plattformen finden. Alle Schritte werden auf der Plattform selbst durchgeführt."
    } else {
        "Here's a quick guide to finding your shareable profile link for each common platform. All steps are done within the app or website itself."
    }
    val closeBtnText = if (appLanguage == "de") "Anleitung schließen" else "Close Guide"

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(dialogTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = introTitleText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                howToFindPlatforms.forEach { platform ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Header with emoji
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(platform.iconEmoji, fontSize = 16.sp)
                                Text(
                                    text = platform.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Text(
                                text = if (appLanguage == "de") platform.introDe else platform.intro,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Instructions Bullet List
                            Column(
                                modifier = Modifier.padding(start = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val currentSteps = if (appLanguage == "de") platform.stepsDe else platform.steps
                                currentSteps.forEach { step ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "•",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = step,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Copy Helper Button if present
                            if (platform.copyableText != null) {
                                val currentLabel = if (appLanguage == "de") platform.copyableLabelDe ?: platform.copyableLabel else platform.copyableLabel
                                if (currentLabel != null) {
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(platform.copyableText))
                                            val copiedMsg = if (appLanguage == "de") "Link/Präfix kopiert!" else "Copied link/prefix to clipboard!"
                                            Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                            contentColor = MaterialTheme.colorScheme.secondary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 4.dp)
                                            .height(30.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy text template",
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = currentLabel,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
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

