package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.example.ui.viewmodel.OtsViewModel
import com.example.util.DatabaseSharingManager
import com.example.util.SettingsManager
import com.example.util.SharedDatabaseItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseSharingScreen(
    viewModel: OtsViewModel,
    settingsManager: SettingsManager,
    initialTab: Int = 0,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }
    val receivedShares by viewModel.receivedShares.collectAsState()
    val sentShares by viewModel.sentShares.collectAsState()
    val unreadCount by viewModel.unreadReceivedSharesCount.collectAsState()

    val questions by viewModel.questions.collectAsState()
    val books by viewModel.books.collectAsState()
    val papers by viewModel.papers.collectAsState()

    val currentUserEmail = settingsManager.googleAccountEmail.ifBlank { settingsManager.userEmail }
    val currentUserName = settingsManager.googleAccountName.ifBlank { settingsManager.userName }

    var previewShareItem by remember { mutableStateOf<SharedDatabaseItem?>(null) }
    var shareToDelete by remember { mutableStateOf<SharedDatabaseItem?>(null) }

    LaunchedEffect(currentUserEmail) {
        if (currentUserEmail.isNotBlank()) {
            viewModel.initSharingListeners(currentUserEmail)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cloud Database Sharing",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (currentUserEmail.isNotBlank()) "Logged in as: $currentUserEmail" else "Not logged in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Row
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Received")
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text("$unreadCount")
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Share New") },
                    icon = { Icon(Icons.Default.Send, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Sent (${sentShares.size})") },
                    icon = { Icon(Icons.Default.Outbox, contentDescription = null) }
                )
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> ReceivedSharesTab(
                    receivedShares = receivedShares,
                    onImport = { shareItem ->
                        scope.launch {
                            val result = viewModel.importFullDatabaseBundle(shareItem.payloadJson)
                            if (result.success) {
                                viewModel.markShareImported(shareItem.id)
                                Toast.makeText(
                                    context,
                                    "Successfully imported ${result.questionsImported} questions, ${result.booksImported} subjects, and ${result.papersImported} papers!",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Import failed: ${result.errorMessage ?: "Unknown error"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onPreview = { previewShareItem = it },
                    onDelete = { shareToDelete = it }
                )
                1 -> ShareNewDatabaseTab(
                    viewModel = viewModel,
                    senderEmail = currentUserEmail,
                    senderName = currentUserName,
                    onShareSuccess = {
                        selectedTabIndex = 2
                    }
                )
                2 -> SentSharesTab(
                    sentShares = sentShares,
                    onDelete = { shareToDelete = it }
                )
            }
        }
    }

    // Preview Dialog
    if (previewShareItem != null) {
        SharedDatabasePreviewDialog(
            item = previewShareItem!!,
            onDismiss = { previewShareItem = null },
            onImport = {
                val item = previewShareItem!!
                previewShareItem = null
                scope.launch {
                    val result = viewModel.importFullDatabaseBundle(item.payloadJson)
                    if (result.success) {
                        viewModel.markShareImported(item.id)
                        Toast.makeText(
                            context,
                            "Successfully imported ${result.questionsImported} questions, ${result.booksImported} subjects, and ${result.papersImported} papers!",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Import failed: ${result.errorMessage ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (shareToDelete != null) {
        AlertDialog(
            onDismissRequest = { shareToDelete = null },
            title = { Text("Delete Shared Package") },
            text = { Text("Are you sure you want to delete '${shareToDelete!!.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = shareToDelete!!.id
                        shareToDelete = null
                        viewModel.deleteOrRevokeShare(id) { success ->
                            if (success) {
                                Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to delete package", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { shareToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReceivedSharesTab(
    receivedShares: List<SharedDatabaseItem>,
    onImport: (SharedDatabaseItem) -> Unit,
    onPreview: (SharedDatabaseItem) -> Unit,
    onDelete: (SharedDatabaseItem) -> Unit
) {
    if (receivedShares.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "No Shared Databases Received",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "When other authorized users share questions or test papers with your email address, they will appear here in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Databases Shared With You (${receivedShares.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(receivedShares, key = { it.id }) { item ->
                ReceivedShareCard(
                    item = item,
                    onImport = { onImport(item) },
                    onPreview = { onPreview(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
private fun ReceivedShareCard(
    item: SharedDatabaseItem,
    onImport: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    val isImported = item.status == "imported"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isImported) MaterialTheme.colorScheme.surfaceContainerLow
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (!isImported) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "From: ${item.senderName} (${item.senderEmail})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isImported) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isImported) "Imported" else "New",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isImported) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Description / Note if present
            if (item.description.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${item.description}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Item Pills Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemPill(
                    icon = Icons.Default.Quiz,
                    label = "${item.questionsCount} Questions"
                )
                ItemPill(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "${item.booksCount} Subjects"
                )
                ItemPill(
                    icon = Icons.Default.Description,
                    label = "${item.papersCount} Papers"
                )
            }

            Text(
                text = "Shared on $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview", fontSize = 13.sp)
                }

                Button(
                    onClick = onImport,
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isImported) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isImported) "Re-Import" else "Import to DB", fontSize = 13.sp)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ShareNewDatabaseTab(
    viewModel: OtsViewModel,
    senderEmail: String,
    senderName: String,
    onShareSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val questions by viewModel.questions.collectAsState()
    val books by viewModel.books.collectAsState()
    val papers by viewModel.papers.collectAsState()

    var recipientEmailInput by remember { mutableStateOf("") }
    var packageTitleInput by remember { mutableStateOf("Full Question Bank Repository") }
    var packageNoteInput by remember { mutableStateOf("") }

    var shareEntireDb by remember { mutableStateOf(true) }
    var selectedBookIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var knownEmails by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSharing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        knownEmails = DatabaseSharingManager.getKnownUserEmails().filter { it.lowercase() != senderEmail.lowercase() }
    }

    val selectedQuestionsCount = remember(shareEntireDb, selectedBookIds, questions) {
        if (shareEntireDb) questions.size
        else {
            val bookIds = selectedBookIds
            val bookTitles = books.filter { it.id in bookIds }.map { it.title.trim().lowercase() }.toSet()
            questions.count { it.bookId in bookIds || it.bookTitle.trim().lowercase() in bookTitles }
        }
    }

    val selectedBooksCount = remember(shareEntireDb, selectedBookIds, books) {
        if (shareEntireDb) books.size
        else selectedBookIds.size
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Share questions, subjects, and exam papers with other logged-in users instantly over cloud storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 1. Recipient Email Field
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Recipient User Email",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = recipientEmailInput,
                    onValueChange = { recipientEmailInput = it },
                    label = { Text("Enter Recipient Email") },
                    placeholder = { Text("colleague@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (knownEmails.isNotEmpty()) {
                    Text(
                        text = "Quick Select Registered Users:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(knownEmails) { email ->
                            SuggestionChip(
                                onClick = { recipientEmailInput = email },
                                label = { Text(email, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }
            }
        }

        // 2. Package Title & Note
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Package Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = packageTitleInput,
                    onValueChange = { packageTitleInput = it },
                    label = { Text("Package Title") },
                    placeholder = { Text("e.g. Mathematics & Physics Question Bank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageNoteInput,
                    onValueChange = { packageNoteInput = it },
                    label = { Text("Optional Note / Message") },
                    placeholder = { Text("e.g. Includes latest midterm sample questions and solutions") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Selection Scope
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "What would you like to share?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = shareEntireDb,
                        onClick = { shareEntireDb = true },
                        label = { Text("Entire Database (${questions.size} Qs)") },
                        leadingIcon = { if (shareEntireDb) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) else null },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = !shareEntireDb,
                        onClick = { shareEntireDb = false },
                        label = { Text("Selective Subjects") },
                        leadingIcon = { if (!shareEntireDb) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) else null },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!shareEntireDb) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Select Subjects to Include:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            books.forEach { book ->
                                val isChecked = book.id in selectedBookIds
                                val qCountInBook = questions.count { it.bookId == book.id || it.bookTitle.equals(book.title, ignoreCase = true) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedBookIds = if (isChecked) selectedBookIds - book.id else selectedBookIds + book.id
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedBookIds = if (checked) selectedBookIds + book.id else selectedBookIds - book.id
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(book.title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                        Text("$qCountInBook questions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Summary & Submit Button
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Package Payload Summary",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$selectedQuestionsCount Questions • $selectedBooksCount Subjects",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (recipientEmailInput.isBlank()) {
                        Toast.makeText(context, "Please enter recipient email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!recipientEmailInput.contains("@") || !recipientEmailInput.contains(".")) {
                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (recipientEmailInput.trim().lowercase() == senderEmail.trim().lowercase()) {
                        Toast.makeText(context, "You cannot share a database with your own email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!shareEntireDb && selectedBookIds.isEmpty()) {
                        Toast.makeText(context, "Please select at least one subject to share", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isSharing = true
                    viewModel.shareDatabaseWithUser(
                        senderEmail = senderEmail,
                        senderName = senderName,
                        recipientEmail = recipientEmailInput.trim(),
                        title = packageTitleInput.trim().ifBlank { "Shared Question Bank" },
                        description = packageNoteInput.trim(),
                        selectedBookIds = if (shareEntireDb) null else selectedBookIds
                    ) { success, message ->
                        isSharing = false
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        if (success) {
                            onShareSuccess()
                        }
                    }
                },
                enabled = !isSharing,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("button_share_database_now")
            ) {
                if (isSharing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading & Sharing...")
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Database via Cloud", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SentSharesTab(
    sentShares: List<SharedDatabaseItem>,
    onDelete: (SharedDatabaseItem) -> Unit
) {
    if (sentShares.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Outbox,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Text(
                    text = "No Sent Databases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Packages you share with other users will be listed here. You can monitor their status or revoke access anytime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Outgoing Shares (${sentShares.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(sentShares, key = { it.id }) { item ->
                SentShareCard(
                    item = item,
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}

@Composable
private fun SentShareCard(
    item: SharedDatabaseItem,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "To: ${item.recipientEmail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (item.status == "imported") MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = if (item.status == "imported") "Imported by User" else "Shared (Pending)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.status == "imported") MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemPill(Icons.Default.Quiz, "${item.questionsCount} Qs")
                ItemPill(Icons.AutoMirrored.Filled.MenuBook, "${item.booksCount} Subjects")
                ItemPill(Icons.Default.Description, "${item.papersCount} Papers")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sent on $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Revoke Share", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SharedDatabasePreviewDialog(
    item: SharedDatabaseItem,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(item.title)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Shared by: ${item.senderName} (${item.senderEmail})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (item.description.isNotBlank()) {
                    Text(
                        text = "Note: ${item.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Text(
                    text = "Package Contents:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• Total Questions:")
                    Text("${item.questionsCount}", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• Subjects / Books:")
                    Text("${item.booksCount}", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• Exam Papers:")
                    Text("${item.papersCount}", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Importing will automatically merge and deduplicate questions with your existing question repository.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onImport) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import into My Database")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
