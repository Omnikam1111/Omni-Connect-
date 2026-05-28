package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ContactWithListDetails
import com.example.util.CompatibilityInfo
import com.example.util.HoroscopeHelper
import com.example.util.HoroscopeInfo
import com.example.util.RelationshipCompatibilityHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipResultScreen(
    person1: ContactWithListDetails,
    person2: ContactWithListDetails,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLanguage = remember(context) {
        com.example.data.SettingsPreferences(context).appLanguage
    }

    val rawH1 = remember(person1.contact.birthdayInMillis) {
        HoroscopeHelper.getHoroscope(person1.contact.birthdayInMillis)
    }
    val rawH2 = remember(person2.contact.birthdayInMillis) {
        HoroscopeHelper.getHoroscope(person2.contact.birthdayInMillis)
    }

    val h1 = remember(rawH1, appLanguage) {
        if (rawH1 != null) {
            com.example.util.Localization.translateHoroscope(rawH1, appLanguage)
        } else null
    }
    val h2 = remember(rawH2, appLanguage) {
        if (rawH2 != null) {
            com.example.util.Localization.translateHoroscope(rawH2, appLanguage)
        } else null
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("relationship_result_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (appLanguage == "de") "Kompatibilitäts-Blaupause" else "Compatibility Blueprint",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("relationship_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to List")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (h1 == null || h2 == null || rawH1 == null || rawH2 == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (appLanguage == "de") "Berechnung Unvollständig" else "Calculations Incomplete",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (appLanguage == "de") "Beide Kontakte müssen ein konfiguriertes Geburtsdatum haben." else "Both selected contacts must have a configured birthdate.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onDismiss) {
                        Text(if (appLanguage == "de") "Zurückgehen" else "Go Back")
                    }
                }
            }
        } else {
            val rawCompatibility = remember(rawH1.name, rawH2.name) {
                RelationshipCompatibilityHelper.getCompatibility(rawH1.name, rawH2.name)
            }
            val compatibility = remember(rawCompatibility, appLanguage) {
                com.example.util.Localization.translateCompatibility(rawCompatibility, appLanguage)
            }

            val ratingColor = remember(compatibility.rating) {
                when (compatibility.rating) {
                    in 85..100 -> Color(0xFFFF2E93) // Passionate Pink
                    in 65..84 -> Color(0xFF673AB7)  // Mystic Violet
                    in 40..64 -> Color(0xFFFF9800)  // Energetic Amber
                    else -> Color(0xFFE53935)       // Conflict Crimson
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Score Card
                ScoreIndicatorHeader(
                    p1Name = "${person1.contact.firstName} ${person1.contact.lastName}",
                    p2Name = "${person2.contact.firstName} ${person2.contact.lastName}",
                    p1Sign = h1.name,
                    p2Sign = h2.name,
                    compatibility = compatibility,
                    ratingColor = ratingColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Profile Comparison Row
                ProfileComparisonRow(
                    p1Name = person1.contact.firstName,
                    p2Name = person2.contact.firstName,
                    h1 = h1,
                    h2 = h2,
                    appLanguage = appLanguage
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Core Analysis Report
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("compatibility_analysis_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ratingColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (appLanguage == "de") "Beziehungs-Analysebericht" else "Match Analysis Report",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (compatibility.condensedText != null) {
                            // Render condensed paragraph details
                            Text(
                                text = compatibility.condensedText,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Generate and display visual blocks from the condensed details
                            Spacer(modifier = Modifier.height(12.dp))
                            VerticalDivider(modifier = Modifier.fillMaxWidth().height(1.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            CondensedCategoriesReport(compatibility.condensedText)

                        } else {
                            // Render detailed split lists (1-25)
                            val sections = listOf(
                                Triple(com.example.util.Localization.getString("category_love", appLanguage), compatibility.love, Icons.Default.Favorite),
                                Triple(com.example.util.Localization.getString("category_comm", appLanguage), compatibility.communication, Icons.Default.Forum),
                                Triple(com.example.util.Localization.getString("category_fin", appLanguage), compatibility.finances, Icons.Default.Payments),
                                Triple(com.example.util.Localization.getString("category_intimacy", appLanguage), compatibility.intimacy, Icons.Default.FavoriteBorder),
                                Triple(com.example.util.Localization.getString("category_conflict", appLanguage), compatibility.conflict, Icons.Default.Gavel),
                                Triple(com.example.util.Localization.getString("category_parenting", appLanguage), compatibility.parenting, Icons.Default.EscalatorWarning),
                                Triple(com.example.util.Localization.getString("category_longterm", appLanguage), compatibility.longTermPotentialString, Icons.Default.StarBorder)
                            )

                            sections.forEach { (title, detail, icon) ->
                                if (detail != null) {
                                    TopicAnalysisRow(title = title, detail = detail, icon = icon, activeColor = ratingColor)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }

                            if (compatibility.shadowWarning != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Shadow Warning",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (appLanguage == "de") "SCHATTEN-WARNUNG" else "SHADOW WARNING",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = compatibility.shadowWarning,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ScoreIndicatorHeader(
    p1Name: String,
    p2Name: String,
    p1Sign: String,
    p2Sign: String,
    compatibility: CompatibilityInfo,
    ratingColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Person
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = p1Name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = p1Sign,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Center Score Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(ratingColor.copy(alpha = 0.1f))
                        .border(3.dp, ratingColor, CircleShape)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${compatibility.rating}%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = ratingColor
                        )
                        Text(
                            text = "MATCH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ratingColor,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Right Person
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = p2Name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = p2Sign,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rating description
            Text(
                text = compatibility.ratingLegendString,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = ratingColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ratingColor.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ProfileComparisonRow(
    p1Name: String,
    p2Name: String,
    h1: HoroscopeInfo,
    h2: HoroscopeInfo,
    appLanguage: String
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = if (appLanguage == "de") "Vergleichender Entwurf" else "Comparative Blueprint",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        val parameters = if (appLanguage == "de") {
            listOf(
                Triple("Element / Modalität", h1.elementModality, h2.elementModality),
                Triple("Herrschender Planet", h1.ruler, h2.ruler),
                Triple("Zentraler Antrieb", h1.coreDrive, h2.coreDrive),
                Triple("Tiefste Angst", h1.deepestFear, h2.deepestFear),
                Triple("Liebessprache", h1.loveLanguage, h2.loveLanguage),
                Triple("Finanzstil", h1.moneyStyle, h2.moneyStyle),
                Triple("Schattenseite", h1.shadow, h2.shadow),
                Triple("Sexualität", h1.sexualSignature, h2.sexualSignature)
            )
        } else {
            listOf(
                Triple("Element / Modality", h1.elementModality, h2.elementModality),
                Triple("Ruling Planet", h1.ruler, h2.ruler),
                Triple("Core Drive", h1.coreDrive, h2.coreDrive),
                Triple("Deepest Fear", h1.deepestFear, h2.deepestFear),
                Triple("Love Language", h1.loveLanguage, h2.loveLanguage),
                Triple("Money Style", h1.moneyStyle, h2.moneyStyle),
                Triple("Shadow Tendency", h1.shadow, h2.shadow),
                Triple("Sexual Signature", h1.sexualSignature, h2.sexualSignature)
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = p1Name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (appLanguage == "de") "Attribute" else "Attributes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = p2Name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                parameters.forEach { (label, val1, val2) ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = val1,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier
                                .weight(0.8f)
                                .padding(horizontal = 4.dp)
                        )
                        Text(
                            text = val2,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopicAnalysisRow(
    title: String,
    detail: String,
    icon: ImageVector,
    activeColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = detail,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CondensedCategoriesReport(condensedText: String) {
    // We can extract categories or display them in structured highlight badges
    val categoryHelpers = listOf(
        "Sex" to Icons.Default.FavoriteBorder,
        "Finances" to Icons.Default.Payments,
        "Conflict" to Icons.Default.Gavel,
        "Parenting" to Icons.Default.EscalatorWarning,
        "Shadow" to Icons.Default.Warning
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categoryHelpers.forEach { (cat, icon) ->
            val indexStart = condensedText.indexOf(cat, ignoreCase = true)
            if (indexStart != -1) {
                // Find sentence or portion containing this keyword
                val phrase = findSentenceContaining(condensedText, indexStart)
                if (phrase.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp).padding(top = 1.dp)
                        )
                        Column {
                            Text(
                                text = cat.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = phrase,
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

private fun findSentenceContaining(text: String, startIdx: Int): String {
    // Find the boundary of the sentence containing the index
    var start = text.lastIndexOf('.', startIdx)
    if (start == -1) start = text.lastIndexOf(';', startIdx)
    if (start == -1) start = 0 else start += 1

    var end = text.indexOf('.', startIdx)
    if (end == -1) end = text.indexOf(';', startIdx)
    if (end == -1) end = text.length

    return text.substring(start, end).trim()
}
