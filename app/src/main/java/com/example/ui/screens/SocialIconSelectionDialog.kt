package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class SocialIconPreset(
    val id: String,
    val name: String,
    val iconUrl: String,
    val defaultUrlPrefix: String,
    val keyDetail: String
)

val socialIconPresetsList = listOf(
    SocialIconPreset("signal", "Signal", "https://img.icons8.com/color/144/signal-app.png", "https://signal.me/#p/+", "Signal phone format link"),
    SocialIconPreset("facebook", "Facebook", "https://img.icons8.com/color/144/facebook-new.png", "facebook.com/", "Facebook profile / custom ID"),
    SocialIconPreset("instagram", "Instagram", "https://img.icons8.com/color/144/instagram-new.png", "instagram.com/", "Instagram mention style @handle"),
    SocialIconPreset("twitter", "Twitter (X)", "https://img.icons8.com/color/144/twitterx--v1.png", "x.com/", "X or classic twitter profile"),
    SocialIconPreset("tiktok", "TikTok", "https://img.icons8.com/color/144/tiktok.png", "tiktok.com/@", "TikTok username profile pattern"),
    SocialIconPreset("linkedin", "LinkedIn", "https://img.icons8.com/color/144/linkedin.png", "linkedin.com/in/", "LinkedIn public in/ profile"),
    SocialIconPreset("youtube", "YouTube", "https://img.icons8.com/color/144/youtube-play.png", "youtube.com/@", "YouTube channel and custom @handle"),
    SocialIconPreset("whatsapp", "WhatsApp", "https://img.icons8.com/color/144/whatsapp.png", "wa.me/", "Omit +, spaces, or dashes from number"),
    SocialIconPreset("snapchat", "Snapchat", "https://img.icons8.com/color/144/snapchat.png", "snapchat.com/add/", "Snapchat add profile URLs"),
    SocialIconPreset("telegram", "Telegram", "https://img.icons8.com/color/144/telegram-app.png", "t.me/", "Telegram direct username channel t.me"),
    SocialIconPreset("github", "GitHub", "https://img.icons8.com/color/144/github--v1.png", "github.com/", "GitHub open source developer handle"),
    SocialIconPreset("discord", "Discord", "https://img.icons8.com/color/144/discord-logo.png", "discordapp.com/users/", "Discord userID or user tag path"),
    SocialIconPreset("reddit", "Reddit", "https://img.icons8.com/color/144/reddit.png", "reddit.com/user/", "Reddit user profile thread handle")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialIconSelectionDialog(
    onDismissRequest: () -> Unit,
    onPresetSelected: (SocialIconPreset) -> Unit,
    onSelectFromFileClicked: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredPresets = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            socialIconPresetsList
        } else {
            socialIconPresetsList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.keyDetail.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Social Media Icon",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close dialog",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fuzzy search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search social brand...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search platforms",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    singleLine = true
                )

                Text(
                    text = "Select a built-in platform option below:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Grid view of built-in presets
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    if (filteredPresets.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No matches found for \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredPresets, key = { it.id }) { preset ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onPresetSelected(preset)
                                        }
                                        .testTag("preset_card_${preset.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = preset.iconUrl,
                                                contentDescription = preset.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = preset.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = preset.keyDetail.take(18) + (if (preset.keyDetail.length > 18) ".." else ""),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Select from Local device file
                Button(
                    onClick = {
                        onSelectFromFileClicked()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("select_from_file_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Select Icon from Device Storage",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Select Icon from Device File",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            // Dismiss handle under standard model styling is done on Close Icon button
        }
    )
}
