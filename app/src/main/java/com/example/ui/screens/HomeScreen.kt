package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.OtsViewModel

data class HomeTileItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val countBadge: String?,
    val containerColor: Color,
    val contentColor: Color
)

@Composable
fun HomeScreen(
    viewModel: OtsViewModel,
    onNavigateToTile: (String) -> Unit
) {
    val questions by viewModel.questions.collectAsState()
    val books by viewModel.books.collectAsState()
    val papers by viewModel.papers.collectAsState()
    val unreadReceivedCount by viewModel.unreadReceivedSharesCount.collectAsState()

    val bookmarkedCount = remember(questions) { questions.count { it.isBookmarked } }

    val tileItems = listOf(
        HomeTileItem(
            id = "quiz",
            title = "Quiz / Practice",
            description = "Interactive test with answers & explanations",
            icon = Icons.Default.Quiz,
            countBadge = "${questions.size} Qs",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        HomeTileItem(
            id = "manage_bank",
            title = "Manage Bank",
            description = "Browse, search, edit & bookmark questions",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            countBadge = "${books.size} Subjects",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        HomeTileItem(
            id = "build_bank",
            title = "Build Bank",
            description = "Add new questions & register subjects",
            icon = Icons.Default.AddCircleOutline,
            countBadge = "+ New",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        HomeTileItem(
            id = "share_database",
            title = "Cloud DB Sharing",
            description = "Share & receive question banks with users via email",
            icon = Icons.Default.Share,
            countBadge = if (unreadReceivedCount > 0) "$unreadReceivedCount New" else "Cloud",
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        HomeTileItem(
            id = "assemble_paper",
            title = "Assemble Paper",
            description = "Build custom exam papers manually or auto",
            icon = Icons.Default.Build,
            countBadge = "Builder",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        HomeTileItem(
            id = "saved_papers",
            title = "Saved Papers",
            description = "Formatted test papers, answers & text export",
            icon = Icons.Default.Description,
            countBadge = "${papers.size} Papers",
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        HomeTileItem(
            id = "recycle_bin",
            title = "Backup & Recycle Bin",
            description = "JSON import, export backup & data reset",
            icon = Icons.Default.RestoreFromTrash,
            countBadge = "${bookmarkedCount} Favs",
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        HomeTileItem(
            id = "live_test_portal",
            title = "Live Test Portal",
            description = "Saved papers timing setup, web server & candidate supervisor",
            icon = Icons.Default.Language,
            countBadge = "Live",
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        HomeTileItem(
            id = "admin_mode",
            title = "Admin Mode",
            description = "Secured admin portal & remote web server supervisor",
            icon = Icons.Default.Security,
            countBadge = "Admin",
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        HomeTileItem(
            id = "expert_review",
            title = "Expert Review",
            description = "Peer evaluation system & subjective answers analysis",
            icon = Icons.Default.CheckCircle,
            countBadge = "Review",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        HomeTileItem(
            id = "settings",
            title = "Settings & Security",
            description = "Profile, Fingerprint App Lock & preferences",
            icon = Icons.Default.Settings,
            countBadge = "Security",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Welcome Banner / Repository Quick Overview
        item(span = { GridItemSpan(2) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Offline Question Bank",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "100% Local Room SQLite Database",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Pills Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatPill(
                            label = "Questions",
                            value = "${questions.size}",
                            modifier = Modifier.weight(1f)
                        )
                        StatPill(
                            label = "Subjects",
                            value = "${books.size}",
                            modifier = Modifier.weight(1f)
                        )
                        StatPill(
                            label = "Papers",
                            value = "${papers.size}",
                            modifier = Modifier.weight(1f)
                        )
                        StatPill(
                            label = "Bookmarks",
                            value = "$bookmarkedCount",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Dashboard Header
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Subsections & Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        // Grid Tiles for all 7 Subsections
        items(tileItems, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clickable { onNavigateToTile(item.id) }
                    .testTag("home_tile_${item.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = item.containerColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = item.contentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.contentColor,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }

                        item.countBadge?.let { badge ->
                            Surface(
                                color = item.contentColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = item.contentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = item.contentColor,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = item.contentColor.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
