package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.example.util.BiometricAuthManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import com.example.data.model.QuestionEntity
import com.example.ui.viewmodel.OtsViewModel
import org.json.JSONArray
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankScreen(viewModel: OtsViewModel, initialMode: String = "manage") {
    val context = LocalContext.current
    val settingsManager = remember { com.example.util.SettingsManager(context) }
    val questions by viewModel.questions.collectAsState()
    val books by viewModel.books.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedBookFilter by viewModel.selectedBookFilter.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedDifficultyFilter by viewModel.selectedDifficultyFilter.collectAsState()
    val showBookmarkedOnly by viewModel.showBookmarkedOnly.collectAsState()

    val isPracticeMode by viewModel.isPracticeMode.collectAsState()
    val practiceQuestions by viewModel.practiceQuestions.collectAsState()
    val practiceIndex by viewModel.practiceIndex.collectAsState()
    val userSelectedOption by viewModel.userSelectedOption.collectAsState()
    val showPracticeExplanation by viewModel.showPracticeExplanation.collectAsState()
    val practiceAnswers by viewModel.practiceAnswers.collectAsState()

    var showAddModal by remember { mutableStateOf(initialMode == "add") }
    var editingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }
    var showAddBookModal by remember { mutableStateOf(false) }
    var showImportExportModal by remember { mutableStateOf(initialMode == "backup") }
    var showDuplicatesModal by remember { mutableStateOf(false) }
    var showSingleDeleteAuthDialog by remember { mutableStateOf(false) }
    var questionToDeleteId by remember { mutableStateOf<String?>(null) }
    var isFiltersExpanded by remember { mutableStateOf(false) }
    var showNoAnswerOnly by remember { mutableStateOf(false) }
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedQuestionIds by remember { mutableStateOf(setOf<String>()) }
    var showBatchSubjectDialog by remember { mutableStateOf(false) }
    var showBatchDeleteAuthDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialMode) {
        if (initialMode == "add") showAddModal = true
        if (initialMode == "backup") showImportExportModal = true
    }

    // Compute dynamic list of subjects from both books DB and imported questions
    val allSubjects = remember(books, questions) {
        val list = mutableListOf<Pair<String, String>>()
        val addedTitles = mutableSetOf<String>()
        books.forEach { book ->
            list.add(Pair(book.id, book.title))
            addedTitles.add(book.title.lowercase().trim())
        }
        questions.map { it.bookTitle.trim() }.distinct().forEach { title ->
            if (title.isNotBlank() && !addedTitles.contains(title.lowercase())) {
                list.add(Pair(title, title))
                addedTitles.add(title.lowercase())
            }
        }
        list
    }

    val selectedBookTitle = remember(allSubjects, selectedBookFilter) { allSubjects.find { it.first == selectedBookFilter }?.second }

    // Filter questions based on state
    val filteredQuestions = remember(
        questions, searchQuery, selectedBookFilter, selectedBookTitle, selectedTypeFilter, selectedDifficultyFilter, showBookmarkedOnly, showNoAnswerOnly
    ) {
        questions.filter { q ->
            val matchesQuery = searchQuery.isEmpty() ||
                    q.question.contains(searchQuery, ignoreCase = true) ||
                    q.chapter.contains(searchQuery, ignoreCase = true) ||
                    q.bookTitle.contains(searchQuery, ignoreCase = true)

            val matchesBook = if (selectedBookFilter == null) {
                true
            } else {
                q.bookId == selectedBookFilter ||
                q.bookTitle.equals(selectedBookFilter, ignoreCase = true) ||
                (selectedBookTitle != null && q.bookTitle.equals(selectedBookTitle, ignoreCase = true))
            }
            val matchesType = selectedTypeFilter == null || q.type.equals(selectedTypeFilter, ignoreCase = true)
            val matchesDiff = selectedDifficultyFilter == null || q.difficulty.equals(selectedDifficultyFilter, ignoreCase = true)
            val matchesBookmark = !showBookmarkedOnly || q.isBookmarked
            val matchesNoAnswer = !showNoAnswerOnly || q.answer.trim().isEmpty()

            matchesQuery && matchesBook && matchesType && matchesDiff && matchesBookmark && matchesNoAnswer
        }
    }

    Scaffold(
        floatingActionButton = {
            if (!isPracticeMode) {
                if (isBatchMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedQuestionIds.isNotEmpty()) {
                            SmallFloatingActionButton(
                                onClick = { showBatchDeleteAuthDialog = true },
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                            }
                        }
                        ExtendedFloatingActionButton(
                            onClick = { 
                                if (selectedQuestionIds.isNotEmpty()) {
                                    showBatchSubjectDialog = true 
                                } else {
                                    Toast.makeText(context, "Please select questions first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            icon = { Icon(Icons.Default.Edit, contentDescription = "Batch Edit") },
                            text = { Text("Change Subject (${selectedQuestionIds.size})") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { showAddModal = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Question") },
                        text = { Text("Add Question") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("add_question_fab")
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Practice Mode View vs Standard Question Explorer
            if (isPracticeMode) {
                androidx.activity.compose.BackHandler {
                    viewModel.stopPracticeMode()
                }
                PracticeModeView(
                    questions = practiceQuestions,
                    currentIndex = practiceIndex,
                    selectedOption = userSelectedOption,
                    showExplanation = showPracticeExplanation,
                    practiceAnswers = practiceAnswers,
                    allSubjects = allSubjects,
                    selectedSubjectFilter = selectedBookFilter,
                    selectedTypeFilter = selectedTypeFilter,
                    allQuestionsCount = questions.size,
                    allQuestionsList = questions,
                    onSelectSubjectFilter = {
                        viewModel.setBookFilter(it)
                        viewModel.startPracticeMode()
                    },
                    onSelectTypeFilter = {
                        viewModel.setTypeFilter(it)
                        viewModel.startPracticeMode()
                    },
                    onSelectOption = { viewModel.selectPracticeOption(it) },
                    onJumpToQuestion = { viewModel.jumpToPracticeQuestion(it, practiceQuestions.size) },
                    onNext = { viewModel.nextPracticeQuestion(practiceQuestions.size) },
                    onPrev = { viewModel.prevPracticeQuestion() },
                    onToggleBookmark = { viewModel.toggleBookmark(it) },
                    onExit = { viewModel.stopPracticeMode() },
                    onEdit = { editingQuestion = it }
                )
            } else {
                // COLLAPSIBLE FILTERS & ACTIONS BAR
                val activeFiltersCount = remember(selectedBookFilter, selectedTypeFilter, selectedDifficultyFilter, showBookmarkedOnly, showNoAnswerOnly) {
                    (if (selectedBookFilter != null) 1 else 0) +
                    (if (selectedTypeFilter != null) 1 else 0) +
                    (if (selectedDifficultyFilter != null) 1 else 0) +
                    (if (showBookmarkedOnly) 1 else 0) +
                    (if (showNoAnswerOnly) 1 else 0)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Search field and filter toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_question_input"),
                            placeholder = { Text("Search statements, chapters...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        )

                        // Batch Mode Toggle
                        IconButton(
                            onClick = { 
                                isBatchMode = !isBatchMode 
                                if (!isBatchMode) selectedQuestionIds = setOf()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isBatchMode) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "Toggle Batch Mode",
                                tint = if (isBatchMode) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Collapsible Filter Button with Badge
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(
                                onClick = { isFiltersExpanded = !isFiltersExpanded },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isFiltersExpanded || activeFiltersCount > 0) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isFiltersExpanded) Icons.Default.Close else Icons.Default.FilterList,
                                    contentDescription = "Toggle Filters",
                                    tint = if (isFiltersExpanded || activeFiltersCount > 0) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (activeFiltersCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape,
                                    modifier = Modifier.size(16.dp).offset(x = 2.dp, y = (-2).dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = activeFiltersCount.toString(),
                                            color = MaterialTheme.colorScheme.onError,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Collapsed Active Filter Chips Row
                    AnimatedVisibility(
                        visible = !isFiltersExpanded && activeFiltersCount > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text(
                                    text = "Active:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (selectedBookFilter != null) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setBookFilter(null) },
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(selectedBookTitle ?: "", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                            if (selectedDifficultyFilter != null) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setDifficultyFilter(null) },
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(selectedDifficultyFilter?.replaceFirstChar { it.uppercase() } ?: "", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                            if (showBookmarkedOnly) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.toggleBookmarkedOnly() },
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Bookmarks Only", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                            if (showNoAnswerOnly) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showNoAnswerOnly = false },
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("No Answer Set", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                            item {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.setBookFilter(null)
                                            viewModel.setDifficultyFilter(null)
                                            if (showBookmarkedOnly) viewModel.toggleBookmarkedOnly()
                                            showNoAnswerOnly = false
                                        },
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Text(
                                        text = "Clear All",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Collapsed Simple Stats line
                    AnimatedVisibility(
                        visible = !isFiltersExpanded && activeFiltersCount == 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredQuestions.size} shown of ${questions.size} total questions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(
                                onClick = {
                                    if (!isBatchMode) {
                                        isBatchMode = true
                                        selectedQuestionIds = filteredQuestions.map { it.id }.toSet()
                                    } else {
                                        val allFilteredIds = filteredQuestions.map { it.id }.toSet()
                                        if (selectedQuestionIds.containsAll(allFilteredIds) && allFilteredIds.isNotEmpty()) {
                                            selectedQuestionIds = selectedQuestionIds - allFilteredIds
                                        } else {
                                            selectedQuestionIds = selectedQuestionIds + allFilteredIds
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBatchMode && filteredQuestions.isNotEmpty() && selectedQuestionIds.containsAll(filteredQuestions.map { it.id })) "Deselect All Filtered" else "Select All Filtered",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Expanded Filters and Actions Panel
                    AnimatedVisibility(
                        visible = isFiltersExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            // Mini Repository Stats Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Local Question Bank Repository",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "${filteredQuestions.size} Shown • ${questions.size} Total questions in Device DB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Storage,
                                                contentDescription = "Room Storage",
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "SQLite Room",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            // Repository Actions
                            Text(
                                text = "Repository Actions",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                item {
                                    Button(
                                        onClick = {
                                            if (filteredQuestions.isNotEmpty()) {
                                                viewModel.startPracticeMode()
                                            } else {
                                                Toast.makeText(context, "No questions to practice", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Practice Quiz", fontSize = 13.sp)
                                    }
                                }

                                item {
                                    FilterChip(
                                        selected = showBookmarkedOnly,
                                        onClick = { viewModel.toggleBookmarkedOnly() },
                                        label = { Text("Bookmarks") },
                                        leadingIcon = {
                                            Icon(
                                                if (showBookmarkedOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (showBookmarkedOnly) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )
                                }

                                item {
                                    OutlinedButton(
                                        onClick = { showAddBookModal = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Subject", fontSize = 12.sp)
                                    }
                                }

                                item {
                                    OutlinedButton(
                                        onClick = { showDuplicatesModal = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.CopyAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Find Duplicates", fontSize = 12.sp)
                                    }
                                }

                                item {
                                    OutlinedButton(
                                        onClick = { showImportExportModal = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import / Backup", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Subjects Filters
                            Text(
                                text = "Subjects Filter",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedBookFilter == null,
                                        onClick = { viewModel.setBookFilter(null) },
                                        label = { Text("All Subjects") }
                                    )
                                }
                                items(allSubjects) { subject ->
                                    val (subjectKey, subjectTitle) = subject
                                    val isSelected = selectedBookFilter == subjectKey || selectedBookFilter.equals(subjectTitle, ignoreCase = true)
                                    val qCount = questions.count { q ->
                                        q.bookId == subjectKey || q.bookTitle.equals(subjectKey, ignoreCase = true) || q.bookTitle.equals(subjectTitle, ignoreCase = true)
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setBookFilter(if (isSelected) null else subjectKey) },
                                        label = { Text("$subjectTitle ($qCount)") }
                                    )
                                }
                            }

                            // Difficulty Levels Filters
                            Text(
                                text = "Difficulty Levels",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 4.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedDifficultyFilter == null,
                                        onClick = { viewModel.setDifficultyFilter(null) },
                                        label = { Text("All Levels") }
                                    )
                                }
                                listOf("easy", "medium", "hard").forEach { diff ->
                                    item {
                                        FilterChip(
                                            selected = selectedDifficultyFilter == diff,
                                            onClick = {
                                                viewModel.setDifficultyFilter(if (selectedDifficultyFilter == diff) null else diff)
                                            },
                                            label = { Text(diff.replaceFirstChar { it.uppercase() }) }
                                        )
                                    }
                                }
                            }

                            // Answer Status Filters
                            Text(
                                text = "Answer Status",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 4.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = !showNoAnswerOnly,
                                        onClick = { showNoAnswerOnly = false },
                                        label = { Text("All Questions") }
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = showNoAnswerOnly,
                                        onClick = { showNoAnswerOnly = true },
                                        label = { Text("Answer Not Set") }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))
                }

                // BATCH SELECTION HEADER CARD
                if (isBatchMode) {
                    val areAllFilteredSelected = remember(selectedQuestionIds, filteredQuestions) {
                        filteredQuestions.isNotEmpty() && filteredQuestions.all { selectedQuestionIds.contains(it.id) }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = areAllFilteredSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedQuestionIds = selectedQuestionIds + filteredQuestions.map { it.id }
                                        } else {
                                            val filteredIds = filteredQuestions.map { it.id }.toSet()
                                            selectedQuestionIds = selectedQuestionIds - filteredIds
                                        }
                                    }
                                )
                                Column {
                                    Text(
                                        text = if (areAllFilteredSelected) "All Filtered Selected" else "Select All Filtered",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${selectedQuestionIds.size} of ${filteredQuestions.size} questions selected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (areAllFilteredSelected) {
                                            val filteredIds = filteredQuestions.map { it.id }.toSet()
                                            selectedQuestionIds = selectedQuestionIds - filteredIds
                                        } else {
                                            selectedQuestionIds = selectedQuestionIds + filteredQuestions.map { it.id }
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (areAllFilteredSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (areAllFilteredSelected) "Deselect All" else "Select All (${filteredQuestions.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        isBatchMode = false
                                        selectedQuestionIds = setOf()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Exit Batch Mode",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Questions List View
                if (filteredQuestions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No questions match your filter",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredQuestions, key = { it.id }) { q ->
                            QuestionCardItem(
                                question = q,
                                onEdit = { editingQuestion = q },
                                onDelete = {
                                    questionToDeleteId = q.id
                                    showSingleDeleteAuthDialog = true
                                },
                                onToggleBookmark = { viewModel.toggleBookmark(q) },
                                isBatchMode = isBatchMode,
                                isSelected = selectedQuestionIds.contains(q.id),
                                onSelectToggle = { selected ->
                                    if (selected) selectedQuestionIds = selectedQuestionIds + q.id
                                    else selectedQuestionIds = selectedQuestionIds - q.id
                                },
                                onQuickEditAnswer = { newAns ->
                                    val opts = try {
                                        val arr = org.json.JSONArray(q.optionsJson)
                                        List(arr.length()) { i -> arr.getString(i) }
                                    } catch (e: Exception) { emptyList<String>() }
                                    viewModel.updateQuestion(
                                        q.id, q.bookId, q.bookTitle, q.chapter, q.type, q.difficulty, q.question, opts, newAns, q.explanation, q.marks, q.isBookmarked
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add New Question Dialog
    if (showAddModal) {
        AddEditQuestionDialog(
            books = books,
            existingQuestion = null,
            defaultBookIdOrTitle = selectedBookFilter,
            onDismiss = { showAddModal = false },
            onConfirm = { bookId, bookTitle, chapter, type, difficulty, qText, options, ans, exp, marks, isBookmarked ->
                viewModel.addQuestion(
                    bookId, bookTitle, chapter, type, difficulty, qText, options, ans, exp, marks
                )
                showAddModal = false
            }
        )
    }

    // Edit Question Dialog
    editingQuestion?.let { q ->
        AddEditQuestionDialog(
            books = books,
            existingQuestion = q,
            onDismiss = { editingQuestion = null },
            onConfirm = { bookId, bookTitle, chapter, type, difficulty, qText, options, ans, exp, marks, isBookmarked ->
                viewModel.updateQuestion(
                    q.id, bookId, bookTitle, chapter, type, difficulty, qText, options, ans, exp, marks, isBookmarked
                )
                editingQuestion = null
            }
        )
    }

    // Add Subject Dialog
    if (showAddBookModal) {
        AddBookDialog(
            onDismiss = { showAddBookModal = false },
            onConfirm = { title, chapters ->
                viewModel.addBook(title, chapters)
                showAddBookModal = false
            }
        )
    }

    if (showBatchSubjectDialog) {
        BatchSubjectDialog(
            books = books,
            selectedCount = selectedQuestionIds.size,
            onDismiss = { showBatchSubjectDialog = false },
            onConfirm = { bookId, newTitle ->
                if (bookId != null) {
                    val bookTitle = books.find { it.id == bookId }?.title ?: ""
                    viewModel.batchUpdateSubject(selectedQuestionIds.toList(), bookId, bookTitle)
                } else if (newTitle != null) {
                    viewModel.batchUpdateSubjectWithNewBook(selectedQuestionIds.toList(), newTitle)
                }
                showBatchSubjectDialog = false
                isBatchMode = false
                selectedQuestionIds = setOf()
                Toast.makeText(context, "Batch updated successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showBatchDeleteAuthDialog && selectedQuestionIds.isNotEmpty()) {
        SecurityVerificationDialog(
            title = "Authorize Batch Deletion",
            subtitle = "Please authenticate to soft-delete ${selectedQuestionIds.size} selected questions. They will be moved to the Recycle Bin.",
            settingsManager = settingsManager,
            showRecoverySnapshotOption = false,
            onSuccess = {
                val count = selectedQuestionIds.size
                selectedQuestionIds.forEach { id ->
                    viewModel.softDeleteQuestion(id, settingsManager)
                }
                showBatchDeleteAuthDialog = false
                isBatchMode = false
                selectedQuestionIds = setOf()
                Toast.makeText(context, "Soft-deleted $count questions to Recycle Bin!", Toast.LENGTH_LONG).show()
            },
            onDismiss = {
                showBatchDeleteAuthDialog = false
            }
        )
    }

    // Import / Backup Dialog
    if (showImportExportModal) {
        ImportExportDialog(
            viewModel = viewModel,
            onDismiss = { showImportExportModal = false }
        )
    }

    if (showSingleDeleteAuthDialog) {
        val qId = questionToDeleteId
        if (qId != null) {
            SecurityVerificationDialog(
                title = "Authorize Question Deletion",
                subtitle = "Please authenticate to soft-delete this question. It will be moved to the Recycle Bin.",
                settingsManager = settingsManager,
                showRecoverySnapshotOption = false,
                onSuccess = {
                    viewModel.softDeleteQuestion(qId, settingsManager)
                    Toast.makeText(context, "Moved to Recycle Bin. Recoverable in Settings!", Toast.LENGTH_SHORT).show()
                    showSingleDeleteAuthDialog = false
                    questionToDeleteId = null
                },
                onDismiss = {
                    showSingleDeleteAuthDialog = false
                    questionToDeleteId = null
                }
            )
        }
    }

    // Duplicate Detection Dialog
    if (showDuplicatesModal) {
        DuplicateDetectionDialog(
            viewModel = viewModel,
            onDismiss = { showDuplicatesModal = false }
        )
    }
}

@Composable
fun QuestionCardItem(
    question: QuestionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleBookmark: () -> Unit,
    isBatchMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: ((Boolean) -> Unit)? = null,
    onQuickEditAnswer: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var tempAnswer by remember(question.answer) { mutableStateOf(question.answer) }
    val isDirty = tempAnswer != question.answer

    val difficultyColor = when (question.difficulty.lowercase()) {
        "easy" -> Color(0xFF2E7D32)
        "medium" -> Color(0xFFEF6C00)
        "hard" -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("question_card_${question.id}")
            .let {
                if (isBatchMode) {
                    it.clickable { onSelectToggle?.invoke(!isSelected) }
                } else it
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBatchMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectToggle?.invoke(it) },
                            modifier = Modifier.size(24.dp).padding(end = 8.dp)
                        )
                    }
                    Surface(
                        color = difficultyColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = question.difficulty.uppercase(),
                            color = difficultyColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${question.marks} Marks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = question.type.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (question.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (question.isBookmarked) Color.Red else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subject & Chapter
            Text(
                text = "${question.bookTitle} • ${question.chapter}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Question Statement
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Options list
            val optionsList = remember(question.optionsJson) {
                try {
                    val arr = JSONArray(question.optionsJson)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                    list
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (question.type == "mcq" && optionsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    optionsList.forEachIndexed { idx, opt ->
                        val isCorrect = opt.trim().equals(tempAnswer.trim(), ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCorrect) Color(0xFFE8F5E9)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .clickable { tempAnswer = opt }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { tempAnswer = opt },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Mark as correct",
                                    tint = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                            Text(
                                text = "${('A' + idx)}. ",
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = opt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (question.type == "tf") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("True", "False").forEach { opt ->
                        val isCorrect = opt.equals(tempAnswer, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCorrect) Color(0xFFE8F5E9) else Color.Transparent)
                                .clickable { tempAnswer = opt }
                                .padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { tempAnswer = opt },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Mark $opt as correct",
                                    tint = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                            Text(opt, fontWeight = FontWeight.Bold, color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (question.type == "fib" || question.type == "subjective") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = tempAnswer,
                    onValueChange = { tempAnswer = it },
                    label = { Text("Correct Answer") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (question.type == "subjective") 2 else 1,
                    singleLine = question.type == "fib"
                )
            }

            if (isDirty) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onQuickEditAnswer?.invoke(tempAnswer) },
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Save Answer")
                }
            }

            if (question.explanation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Hide Explanation" else "Show Explanation",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Explanation: ${question.explanation}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeModeView(
    questions: List<QuestionEntity>,
    currentIndex: Int,
    selectedOption: String?,
    showExplanation: Boolean,
    practiceAnswers: Map<Int, String> = emptyMap(),
    allSubjects: List<Pair<String, String>> = emptyList(),
    selectedSubjectFilter: String? = null,
    selectedTypeFilter: String? = null,
    allQuestionsCount: Int = 0,
    allQuestionsList: List<QuestionEntity> = emptyList(),
    onSelectSubjectFilter: (String?) -> Unit = {},
    onSelectTypeFilter: (String?) -> Unit = {},
    onSelectOption: (String) -> Unit,
    onJumpToQuestion: (Int) -> Unit = {},
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleBookmark: (QuestionEntity) -> Unit,
    onEdit: (QuestionEntity) -> Unit = {},
    onExit: () -> Unit
) {
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(true) }
    var showQuestionGridSheet by remember { mutableStateOf(false) }
    var showSummaryModal by remember { mutableStateOf(false) }
    var forceShowExplanation by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    val correctCount = practiceAnswers.entries.count { (idx, ans) ->
        if (idx < questions.size) {
            ans.trim().equals(questions[idx].answer.trim(), ignoreCase = true)
        } else false
    }
    val incorrectCount = practiceAnswers.entries.count { (idx, ans) ->
        if (idx < questions.size) {
            !ans.trim().equals(questions[idx].answer.trim(), ignoreCase = true)
        } else false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // Full screen Top Bar
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onExit) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Quiz",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = "Full Screen Quiz",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = selectedSubjectFilter ?: "All Subjects (${questions.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Timer Pill
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable { isTimerRunning = !isTimerRunning }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isTimerRunning) Icons.Default.Timer else Icons.Default.Pause,
                                    contentDescription = "Timer",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Score Badges
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "✓ $correctCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "✗ $incorrectCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Question Palette Trigger
                        IconButton(onClick = { showQuestionGridSheet = true }) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Question Grid",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Progress Bar
            if (questions.isNotEmpty()) {
                val progress = (currentIndex + 1).toFloat() / questions.size
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }


            // Type Filter Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { onSelectTypeFilter(null) },
                        label = { Text("All Types") }
                    )
                }
                val typeMap = mapOf(
                    "mcq" to "MCQ",
                    "subjective" to "Subjective",
                    "tf" to "True / False",
                    "fib" to "Fill in Blanks"
                )
                items(typeMap.keys.toList()) { typeKey ->
                    val isSelected = selectedTypeFilter == typeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTypeFilter(if (isSelected) null else typeKey) },
                        label = { Text(typeMap[typeKey]!!) }
                    )
                }
            }
            // Subject Filter Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSubjectFilter == null,
                        onClick = { onSelectSubjectFilter(null) },
                        label = { Text("All (${allQuestionsCount})") }
                    )
                }
                items(allSubjects) { subject ->
                    val (subjectKey, subjectTitle) = subject
                    val isSelected = selectedSubjectFilter == subjectKey || selectedSubjectFilter.equals(subjectTitle, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSubjectFilter(if (isSelected) null else subjectKey) },
                        label = { Text(subjectTitle) }
                    )
                }
            }

            if (questions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No questions found in this quiz filter.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Select 'All' or pick another subject above.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onExit) {
                            Text("Exit Full Screen Quiz")
                        }
                    }
                }
                return
            }

            val currentQ = questions[currentIndex.coerceIn(0, questions.size - 1)]

            val optionsList = remember(currentQ.optionsJson) {
                try {
                    val arr = JSONArray(currentQ.optionsJson)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                    list
                } catch (e: Exception) {
                    emptyList()
                }
            }

            // Question Card & Options (Scrollable area)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Q ${currentIndex + 1} of ${questions.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        color = when (currentQ.difficulty.lowercase()) {
                                            "easy" -> Color(0xFFE8F5E9)
                                            "medium" -> Color(0xFFFFF3E0)
                                            "hard" -> Color(0xFFFFEBEE)
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = currentQ.difficulty.ifEmpty { "Medium" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (currentQ.difficulty.lowercase()) {
                                                "easy" -> Color(0xFF2E7D32)
                                                "medium" -> Color(0xFFE65100)
                                                "hard" -> Color(0xFFC62828)
                                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Text(
                                        text = "+${currentQ.marks} Marks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row {
                                    IconButton(onClick = { onEdit(currentQ) }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Question",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(onClick = { onToggleBookmark(currentQ) }) {
                                        Icon(
                                            if (currentQ.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (currentQ.isBookmarked) Color.Red else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "${currentQ.bookTitle} • ${currentQ.chapter}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = currentQ.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                itemsIndexed(optionsList) { index, option ->
                    val optionLetters = listOf("A", "B", "C", "D", "E", "F")
                    val letter = optionLetters.getOrElse(index) { "${index + 1}" }

                    val isSelected = selectedOption == option
                    val isCorrect = option.trim().equals(currentQ.answer.trim(), ignoreCase = true)
                    val showResult = showExplanation || forceShowExplanation || selectedOption != null

                    val backgroundColor = when {
                        showResult && isCorrect -> Color(0xFFE8F5E9)
                        showResult && isSelected && !isCorrect -> Color(0xFFFFEBEE)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    }

                    val borderStrokeColor = when {
                        showResult && isCorrect -> Color(0xFF2E7D32)
                        showResult && isSelected && !isCorrect -> Color(0xFFC62828)
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectOption(option) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = if (borderStrokeColor != Color.Transparent) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderStrokeColor), width = 2.dp) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    showResult && isCorrect -> Color(0xFF2E7D32)
                                    showResult && isSelected && !isCorrect -> Color(0xFFC62828)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected || (showResult && (isCorrect || isSelected))) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected || (showResult && isCorrect)) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )

                            if (showResult && isCorrect) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = Color(0xFF2E7D32))
                            } else if (showResult && isSelected && !isCorrect) {
                                Icon(Icons.Default.Cancel, contentDescription = "Incorrect", tint = Color(0xFFC62828))
                            }
                        }
                    }
                }

                if (showExplanation || forceShowExplanation || selectedOption != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Lightbulb,
                                        contentDescription = "Explanation",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Answer & Solution",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Correct Answer: ${currentQ.answer}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (currentQ.explanation.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentQ.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Navigation & Action Dock
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPrev,
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = forceShowExplanation,
                            onClick = { forceShowExplanation = !forceShowExplanation },
                            label = { Text("Solution") },
                            leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )

                        FilterChip(
                            selected = false,
                            onClick = { showSummaryModal = true },
                            label = { Text("Finish") },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Button(
                        onClick = {
                            if (currentIndex == questions.size - 1) {
                                showSummaryModal = true
                            } else {
                                onNext()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (currentIndex == questions.size - 1) "Finish" else "Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (currentIndex == questions.size - 1) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Question Grid Dialog
    if (showQuestionGridSheet) {
        AlertDialog(
            onDismissRequest = { showQuestionGridSheet = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Question Palette (${questions.size})", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showQuestionGridSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("✓ $correctCount Correct", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        Text("✗ $incorrectCount Wrong", style = MaterialTheme.typography.labelSmall, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        Text("○ ${questions.size - practiceAnswers.size} Left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(questions.size) { idx ->
                            val q = questions[idx]
                            val isCurrent = idx == currentIndex
                            val savedAns = practiceAnswers[idx]
                            val isCorrect = savedAns != null && savedAns.trim().equals(q.answer.trim(), ignoreCase = true)
                            val isWrong = savedAns != null && !isCorrect

                            val bg = when {
                                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                isCorrect -> Color(0xFFC8E6C9)
                                isWrong -> Color(0xFFFFCDD2)
                                savedAns != null -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainer
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = bg,
                                border = if (isCurrent) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary), width = 2.dp) else null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        onJumpToQuestion(idx)
                                        showQuestionGridSheet = false
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuestionGridSheet = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Quiz Summary Dialog
    if (showSummaryModal) {
        val totalQuestions = questions.size
        val answeredCount = practiceAnswers.size
        val scorePercent = if (totalQuestions > 0) (correctCount.toFloat() / totalQuestions * 100).toInt() else 0

        AlertDialog(
            onDismissRequest = { showSummaryModal = false },
            icon = {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Quiz Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Quiz Session Completed!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$scorePercent%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Overall Accuracy Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Questions:")
                                Text("$totalQuestions", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Correct Answers:")
                                Text("$correctCount", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Incorrect Answers:")
                                Text("$incorrectCount", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Unanswered:")
                                Text("${totalQuestions - answeredCount}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Time Spent:")
                                Text(formattedTime, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSummaryModal = false
                        onExit()
                    }
                ) {
                    Text("Exit Full Screen")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSummaryModal = false }
                ) {
                    Text("Review Answers")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditQuestionDialog(
    books: List<BookEntity>,
    existingQuestion: QuestionEntity?,
    defaultBookIdOrTitle: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        bookId: String,
        bookTitle: String,
        chapter: String,
        type: String,
        difficulty: String,
        question: String,
        options: List<String>,
        answer: String,
        explanation: String,
        marks: Int,
        isBookmarked: Boolean
    ) -> Unit
) {
    val displayBooks = remember(books, existingQuestion, defaultBookIdOrTitle) {
        val list = books.toMutableList()
        if (existingQuestion != null && existingQuestion.bookTitle.isNotBlank()) {
            val alreadyExists = books.any { 
                it.id == existingQuestion.bookId || 
                it.title.equals(existingQuestion.bookTitle, ignoreCase = true) 
            }
            if (!alreadyExists) {
                list.add(BookEntity(id = existingQuestion.bookId, title = existingQuestion.bookTitle, chapterCount = 5))
            }
        } else if (defaultBookIdOrTitle != null && defaultBookIdOrTitle.isNotBlank()) {
            val alreadyExists = books.any { 
                it.id == defaultBookIdOrTitle || 
                it.title.equals(defaultBookIdOrTitle, ignoreCase = true) 
            }
            if (!alreadyExists) {
                list.add(BookEntity(id = "b_" + UUID.randomUUID().toString().take(8), title = defaultBookIdOrTitle, chapterCount = 5))
            }
        }
        list
    }

    val initialBook = remember(existingQuestion, displayBooks, defaultBookIdOrTitle) {
        if (existingQuestion != null) {
            displayBooks.find { it.id == existingQuestion.bookId || it.title.equals(existingQuestion.bookTitle, ignoreCase = true) }
        } else if (defaultBookIdOrTitle != null) {
            displayBooks.find { it.id == defaultBookIdOrTitle || it.title.equals(defaultBookIdOrTitle, ignoreCase = true) } ?: displayBooks.firstOrNull()
        } else {
            displayBooks.firstOrNull()
        }
    }

    var isCreatingNewSubject by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf(initialBook) }

    var chapter by remember { mutableStateOf(existingQuestion?.chapter ?: "Chapter 1") }
    var type by remember { mutableStateOf(existingQuestion?.type ?: "mcq") } // mcq, fib, tf, subjective
    var difficulty by remember { mutableStateOf(existingQuestion?.difficulty ?: "medium") }
    var questionText by remember { mutableStateOf(existingQuestion?.question ?: "") }

    val initialOptions = remember(existingQuestion) {
        try {
            val arr = JSONArray(existingQuestion?.optionsJson ?: "[]")
            List(4) { i -> if (i < arr.length()) arr.getString(i) else "" }
        } catch (e: Exception) {
            listOf("", "", "", "")
        }
    }

    var optionA by remember { mutableStateOf(initialOptions.getOrElse(0) { "" }) }
    var optionB by remember { mutableStateOf(initialOptions.getOrElse(1) { "" }) }
    var optionC by remember { mutableStateOf(initialOptions.getOrElse(2) { "" }) }
    var optionD by remember { mutableStateOf(initialOptions.getOrElse(3) { "" }) }

    var selectedAnswer by remember { mutableStateOf(existingQuestion?.answer ?: "") }
    var tfAnswer by remember { mutableStateOf(if (existingQuestion?.answer?.trim()?.lowercase() == "false") "False" else "True") }
    var explanation by remember { mutableStateOf(existingQuestion?.explanation ?: "") }
    var marksText by remember { mutableStateOf(existingQuestion?.marks?.toString() ?: "2") }
    var isBookmarked by remember { mutableStateOf(existingQuestion?.isBookmarked ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingQuestion == null) "Add New Question" else "Edit Question", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Question Type Selection
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Question Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "mcq" to "MCQ",
                                "fib" to "Fill Blanks",
                                "tf" to "True/False",
                                "subjective" to "Subjective"
                            ).forEach { (typeKey, typeLabel) ->
                                FilterChip(
                                    selected = type == typeKey,
                                    onClick = { type = typeKey },
                                    label = { Text(typeLabel, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 2. Subject Selector / Creation with Deduplication (Dropdown style)
                item {
                    var expandedBookDropdown by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Subject / Book", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (isCreatingNewSubject) "➕ New Subject" else (selectedBook?.title ?: "Select Subject"),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { expandedBookDropdown = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Toggle Subject Dropdown")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Transparent overlay to safely capture clicks across the whole text field
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { expandedBookDropdown = true }
                            )
                            DropdownMenu(
                                expanded = expandedBookDropdown,
                                onDismissRequest = { expandedBookDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                displayBooks.forEach { book ->
                                    DropdownMenuItem(
                                        text = { Text(book.title) },
                                        onClick = {
                                            isCreatingNewSubject = false
                                            selectedBook = book
                                            expandedBookDropdown = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("➕ New Subject") },
                                    onClick = {
                                        isCreatingNewSubject = true
                                        expandedBookDropdown = false
                                    }
                                )
                            }
                        }

                        if (isCreatingNewSubject) {
                            OutlinedTextField(
                                value = newSubjectName,
                                onValueChange = { newSubjectName = it },
                                label = { Text("Enter New Subject Title") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = {
                                    val matched = books.find { it.title.equals(newSubjectName.trim(), ignoreCase = true) }
                                    if (matched != null) {
                                        Text("⚠️ Subject '${matched.title}' already exists and will be reused.", color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Text("Will create a new subject entity.")
                                    }
                                }
                            )
                        }
                    }
                }

                // 3. Chapter & Difficulty
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chapter,
                            onValueChange = { chapter = it },
                            label = { Text("Chapter") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = marksText,
                            onValueChange = { marksText = it },
                            label = { Text("Marks") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Difficulty Level", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("easy", "medium", "hard").forEach { diff ->
                                FilterChip(
                                    selected = difficulty == diff,
                                    onClick = { difficulty = diff },
                                    label = { Text(diff.uppercase(), fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // 4. Question Statement
                item {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = {
                            Text(
                                when (type) {
                                    "fib" -> "Fill in the Blanks Question (use ___ for blank)"
                                    "tf" -> "True / False Statement"
                                    "subjective" -> "Subjective Question Prompt"
                                    else -> "MCQ Question Statement"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                // 5. Dynamic Type Specific Fields
                when (type) {
                    "mcq" -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("MCQ Options (Tick correct option)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                
                                listOf(
                                    Triple(optionA, { v: String -> optionA = v }, "Option A"),
                                    Triple(optionB, { v: String -> optionB = v }, "Option B"),
                                    Triple(optionC, { v: String -> optionC = v }, "Option C"),
                                    Triple(optionD, { v: String -> optionD = v }, "Option D")
                                ).forEach { (optVal, setOpt, placeholder) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (optVal.isNotBlank()) {
                                                    selectedAnswer = if (selectedAnswer == optVal) "" else optVal
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Mark as correct",
                                                tint = if (selectedAnswer.isNotBlank() && selectedAnswer == optVal) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            )
                                        }
                                        OutlinedTextField(
                                            value = optVal,
                                            onValueChange = { newValue ->
                                                val oldVal = optVal
                                                setOpt(newValue)
                                                if (selectedAnswer == oldVal) {
                                                    selectedAnswer = newValue
                                                }
                                            },
                                            label = { Text(placeholder) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "fib" -> {
                        item {
                            OutlinedTextField(
                                value = selectedAnswer,
                                onValueChange = { selectedAnswer = it },
                                label = { Text("Correct Blank Word / Phrase") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                supportingText = { Text("Word or phrase that correctly completes the blank.") }
                            )
                        }
                    }
                    "tf" -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Correct Answer (Tick correct option)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { tfAnswer = "True" }
                                    ) {
                                        IconButton(
                                            onClick = { tfAnswer = "True" },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Mark True as correct",
                                                tint = if (tfAnswer == "True") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            )
                                        }
                                        Text("TRUE", fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { tfAnswer = "False" }
                                    ) {
                                        IconButton(
                                            onClick = { tfAnswer = "False" },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Mark False as correct",
                                                tint = if (tfAnswer == "False") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            )
                                        }
                                        Text("FALSE", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    "subjective" -> {
                        item {
                            OutlinedTextField(
                                value = selectedAnswer,
                                onValueChange = { selectedAnswer = it },
                                label = { Text("Model Answer / Key Marking Points") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                supportingText = { Text("Provide reference answer or evaluation criteria.") }
                            )
                        }
                    }
                }

                // 6. Explanation
                item {
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("Explanation / Remarks (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalBookTitle: String
                    val finalBookId: String

                    if (isCreatingNewSubject && newSubjectName.isNotBlank()) {
                        val trimmed = newSubjectName.trim()
                        val existingMatch = books.find { it.title.equals(trimmed, ignoreCase = true) }
                        if (existingMatch != null) {
                            finalBookId = existingMatch.id
                            finalBookTitle = existingMatch.title
                        } else {
                            finalBookId = "b_" + UUID.randomUUID().toString().take(8)
                            finalBookTitle = trimmed
                        }
                    } else {
                        val b = selectedBook ?: books.firstOrNull() ?: BookEntity("b1", "General Subject", 5)
                        finalBookId = b.id
                        finalBookTitle = b.title
                    }

                    val finalOptions: List<String>
                    val finalAnswer: String

                    when (type) {
                        "mcq" -> {
                            finalOptions = listOf(optionA, optionB, optionC, optionD).filter { it.isNotBlank() }
                            finalAnswer = if (selectedAnswer.isNotBlank()) selectedAnswer else "N/A"
                        }
                        "tf" -> {
                            finalOptions = listOf("True", "False")
                            finalAnswer = tfAnswer
                        }
                        else -> {
                            finalOptions = emptyList()
                            finalAnswer = selectedAnswer
                        }
                    }

                    onConfirm(
                        finalBookId,
                        finalBookTitle,
                        chapter,
                        type,
                        difficulty,
                        questionText,
                        finalOptions,
                        finalAnswer,
                        explanation,
                        marksText.toIntOrNull() ?: 2,
                        isBookmarked
                    )
                },
                enabled = questionText.isNotBlank() && (!isCreatingNewSubject || newSubjectName.isNotBlank())
            ) {
                Text(if (existingQuestion == null) "Save Question" else "Update Question")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddBookDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, chapterCount: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var chapterCountText by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subject / Book") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Subject / Book Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chapterCountText,
                    onValueChange = { chapterCountText = it },
                    label = { Text("Estimated Chapters") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, chapterCountText.toIntOrNull() ?: 5)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ImportExportDialog(
    viewModel: OtsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Format: 0 = JSON, 1 = CSV, 2 = Word (.docx), 3 = Excel (.xlsx)
    var activeFormatTab by remember { mutableIntStateOf(0) }
    var dataText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showSecurityAuthDialog by remember { mutableStateOf(false) }
    var createRecoverySnapshotChecked by remember { mutableStateOf(true) }

    var savingTemplateType by remember { mutableIntStateOf(0) } // 0 = Word, 1 = Excel
    val saveTemplateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (savingTemplateType == 0) 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" 
            else 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    if (savingTemplateType == 0) {
                        com.example.util.DocxXlsxHelper.generateDocxTemplate(stream)
                    } else {
                        com.example.util.DocxXlsxHelper.generateXlsxTemplate(stream)
                    }
                }
                Toast.makeText(context, "Template saved successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save template: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                if (activeFormatTab == 2) {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        scope.launch {
                            val (success, count) = viewModel.importQuestionsFromDocx(stream)
                            if (success && count > 0) {
                                statusMessage = "Successfully imported $count question(s) from Word (.docx)!"
                                Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                statusMessage = "Failed to import from Word. Ensure correct template tags are used."
                                Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else if (activeFormatTab == 3) {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        scope.launch {
                            val (success, count) = viewModel.importQuestionsFromXlsx(stream)
                            if (success && count > 0) {
                                statusMessage = "Successfully imported $count question(s) from Excel (.xlsx)!"
                                Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                statusMessage = "Failed to import from Excel. Ensure correct columns are present."
                                Toast.makeText(context, statusMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val content = stream.bufferedReader().use { reader -> reader.readText() }
                        dataText = content
                        statusMessage = "Loaded file contents (${content.length} characters)"
                        Toast.makeText(context, "File loaded into editor!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Save File Launcher
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(if (activeFormatTab == 0) "application/json" else "text/csv")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(dataText.toByteArray())
                }
                Toast.makeText(context, "Saved successfully to file!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Bulk Import & Export", fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Transfer question sets offline in JSON, CSV, Word, or Excel format. Import from file, or export your local database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Format Tab Selector (JSON vs CSV vs Word vs Excel)
                ScrollableTabRow(
                    selectedTabIndex = activeFormatTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Tab(
                        selected = activeFormatTab == 0,
                        onClick = {
                            activeFormatTab = 0
                            dataText = ""
                            statusMessage = null
                        },
                        text = { Text("JSON") },
                        icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = activeFormatTab == 1,
                        onClick = {
                            activeFormatTab = 1
                            dataText = ""
                            statusMessage = null
                        },
                        text = { Text("CSV") },
                        icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = activeFormatTab == 2,
                        onClick = {
                            activeFormatTab = 2
                            dataText = ""
                            statusMessage = null
                        },
                        text = { Text("Word (.docx)") },
                        icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = activeFormatTab == 3,
                        onClick = {
                            activeFormatTab = 3
                            dataText = ""
                            statusMessage = null
                        },
                        text = { Text("Excel (.xlsx)") },
                        icon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                if (activeFormatTab >= 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (activeFormatTab == 2) "Word Template Instructions:" else "Excel Template Instructions:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = if (activeFormatTab == 2) {
                                        "Define questions using bracket tags:\n" +
                                        "• [Question] Your question text\n" +
                                        "• [Subject] Subject name\n" +
                                        "• [Chapter] Chapter name\n" +
                                        "• [Type] mcq / fib / tf / subjective\n" +
                                        "• [Difficulty] easy / medium / hard\n" +
                                        "• [Options] OptionA|OptionB|OptionC (for mcq)\n" +
                                        "• [Answer] Correct answer text\n" +
                                        "• [Explanation] Explanation text\n" +
                                        "• [Marks] Marks value"
                                    } else {
                                        "Excel spreadsheet must contain the following columns in the first row:\n" +
                                        "Question, BookTitle, Chapter, Type, Difficulty, Options, Answer, Explanation, Marks"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    savingTemplateType = if (activeFormatTab == 2) 0 else 1
                                    val filename = if (activeFormatTab == 2) "questions_template.docx" else "questions_template.xlsx"
                                    saveTemplateLauncher.launch(filename)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download Template", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    filePickerLauncher.launch("*/*")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick File", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    // Action Bar 1: Export Actions & File Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                dataText = if (activeFormatTab == 0) {
                                    viewModel.exportAllDataToJson()
                                } else {
                                    viewModel.exportQuestionsToCsv()
                                }
                                statusMessage = "Exported question bank (${if (activeFormatTab == 0) "JSON" else "CSV"})"
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (activeFormatTab == 0) "Export JSON" else "Export CSV", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pick File", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                dataText = if (activeFormatTab == 0) {
                                    viewModel.getSampleQuestionsJson()
                                } else {
                                    viewModel.getSampleQuestionsCsv()
                                }
                                statusMessage = "Loaded sample batch template"
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sample", fontSize = 12.sp)
                        }
                    }

                    // Editor Text Field
                    OutlinedTextField(
                        value = dataText,
                        onValueChange = {
                            dataText = it
                            statusMessage = null
                        },
                        label = {
                            Text(if (activeFormatTab == 0) "JSON Data (Batch Array)" else "CSV Data (Comma-Separated)")
                        },
                        placeholder = {
                            Text(
                                if (activeFormatTab == 0)
                                    "[ {\"question\": \"What is X?\", \"options\": [\"A\", \"B\"], \"answer\": \"A\"} ]"
                                else
                                    "Question,BookTitle,Chapter,Type,Difficulty,Options,Answer,Explanation,Marks\n\"What is X?\",\"Subject\",\"Chap 1\",\"mcq\",\"easy\",\"A|B|C|D\",\"A\",\"Exp\",1"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                    )

                    // Quick Tools Bar: Copy, Share, Save File
                    if (dataText.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("QuestionData", dataText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("Copy") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )

                            AssistChip(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, dataText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Question Bank Batch")
                                    context.startActivity(shareIntent)
                                },
                                label = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )

                            AssistChip(
                                onClick = {
                                    val filename = if (activeFormatTab == 0) "question_bank_export.json" else "question_bank_export.csv"
                                    saveFileLauncher.launch(filename)
                                },
                                label = { Text("Save File") },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }

                statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            if (activeFormatTab < 2) {
                Button(
                    onClick = {
                        if (dataText.isNotBlank()) {
                            scope.launch {
                                val (success, count) = if (activeFormatTab == 0) {
                                    viewModel.importAllDataFromJson(dataText)
                                } else {
                                    viewModel.importQuestionsFromCsv(dataText)
                                }

                                if (success && count > 0) {
                                    Toast.makeText(context, "Successfully imported $count question(s) into database!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Failed to import. Check format syntax.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = dataText.isNotBlank()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Batch")
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        showSecurityAuthDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear DB")
                }

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )

    if (showSecurityAuthDialog) {
        val settingsManager = remember { com.example.util.SettingsManager(context) }
        SecurityVerificationDialog(
            title = "Authorize Database Clear",
            subtitle = "This will permanently clear all questions from your local database.",
            settingsManager = settingsManager,
            showRecoverySnapshotOption = true,
            recoverySnapshotChecked = createRecoverySnapshotChecked,
            onRecoverySnapshotCheckedChange = { createRecoverySnapshotChecked = it },
            lastBackupTime = settingsManager.lastBackupTime,
            onRestoreBackup = if (settingsManager.lastBackupJson.isNotEmpty()) {
                {
                    viewModel.restoreSnapshotPoint(settingsManager) { success, count ->
                        if (success) {
                            Toast.makeText(context, "Successfully restored $count questions from recovery snapshot!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed to restore snapshot point.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else null,
            onSuccess = {
                if (createRecoverySnapshotChecked) {
                    viewModel.createSnapshotPoint(settingsManager)
                }
                viewModel.clearAllQuestions()
                Toast.makeText(context, "Database cleared successfully!", Toast.LENGTH_LONG).show()
                onDismiss()
            },
            onDismiss = {
                showSecurityAuthDialog = false
            }
        )
    }
}

@Composable
fun DuplicateDetectionDialog(
    viewModel: OtsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var duplicateGroups by remember { mutableStateOf(viewModel.findDuplicateGroups()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CopyAll,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Duplicate Detection", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (duplicateGroups.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No Duplicates Found!",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "All questions in your local database are unique.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    val totalDupesCount = duplicateGroups.sumOf { it.questions.size - 1 }
                    Text(
                        text = "Found ${duplicateGroups.size} set(s) of duplicate questions ($totalDupesCount redundant entries total). You can review or clean them below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            viewModel.removeDuplicates("bookmarked") { removed ->
                                Toast.makeText(context, "Removed $removed duplicate question(s)!", Toast.LENGTH_LONG).show()
                                duplicateGroups = viewModel.findDuplicateGroups()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Clean All Duplicates ($totalDupesCount)")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(duplicateGroups) { idx, group ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Duplicate Set #${idx + 1} (${group.questions.size} copies)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = group.questions.first().question,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column {
                                        group.questions.forEachIndexed { qIdx, q ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Copy ${qIdx + 1}: [${q.bookTitle} • ${q.chapter}]",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (qIdx > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteQuestion(q.id)
                                                            duplicateGroups = viewModel.findDuplicateGroups()
                                                            Toast.makeText(context, "Deleted copy", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Delete Copy",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        "(Kept)",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun BatchSubjectDialog(
    books: List<BookEntity>,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (bookId: String?, newSubjectTitle: String?) -> Unit
) {
    var isNewSubject by remember { mutableStateOf(false) }
    var selectedBookId by remember { mutableStateOf(books.firstOrNull()?.id ?: "") }
    var newSubjectName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Subject for $selectedCount Questions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "You can move these questions to an existing subject or create a new one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !isNewSubject,
                        onClick = { isNewSubject = false }
                    )
                    Text("Existing Subject")
                }
                if (!isNewSubject) {
                    if (books.isEmpty()) {
                        Text("No existing subjects.", color = MaterialTheme.colorScheme.error)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 120.dp).fillMaxWidth()
                        ) {
                            items(books) { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBookId = book.id }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedBookId == book.id,
                                        onClick = { selectedBookId = book.id }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(book.title, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isNewSubject,
                        onClick = { isNewSubject = true }
                    )
                    Text("New Subject")
                }
                if (isNewSubject) {
                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        label = { Text("New Subject Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isNewSubject) {
                        if (newSubjectName.isNotBlank()) onConfirm(null, newSubjectName.trim())
                    } else {
                        if (selectedBookId.isNotBlank()) onConfirm(selectedBookId, null)
                    }
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

