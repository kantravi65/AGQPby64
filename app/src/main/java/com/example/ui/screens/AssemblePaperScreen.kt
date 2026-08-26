package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.CircleShape
import com.example.data.model.BookEntity
import com.example.data.model.PaperEntity
import com.example.data.model.QuestionEntity
import com.example.ui.viewmodel.OtsViewModel
import com.example.util.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssemblePaperScreen(viewModel: OtsViewModel, initialTab: Int = 0) {
    val context = LocalContext.current
    val questions by viewModel.questions.collectAsState()
    val books by viewModel.books.collectAsState()
    val papers by viewModel.papers.collectAsState()

    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0 = Builder, 1 = Saved Papers

    // Builder Form State
    var isConfigExpanded by remember { mutableStateOf(false) }
    var paperTitle by remember { mutableStateOf("Physics & Math Combined Test") }
    var selectedBookFilter by remember { mutableStateOf<String?>(null) }
    var durationMinutes by remember { mutableStateOf("60") }
    var isAutoAssemble by remember { mutableStateOf(false) }
        var autoMcqCount by remember { mutableStateOf("5") }
    var autoTfCount by remember { mutableStateOf("5") }
    var autoFibCount by remember { mutableStateOf("5") }
    var autoSubjectiveCount by remember { mutableStateOf("5") }

    // Selected Question IDs for Manual Builder
    val selectedQuestionIds = remember { mutableStateListOf<String>() }

    // Paper Preview Modal
    var previewPaper by remember { mutableStateOf<PaperEntity?>(null) }
    var printSetupPaper by remember { mutableStateOf<PaperEntity?>(null) }

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

    // Filter questions by subject if selected
    val availableQuestions = remember(questions, selectedBookFilter, allSubjects) {
        if (selectedBookFilter == null) questions
        else {
            val filterTitle = allSubjects.find { it.first == selectedBookFilter }?.second ?: selectedBookFilter
            questions.filter { it.bookId == selectedBookFilter || it.bookTitle.equals(filterTitle, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (activeTab == 0 && selectedQuestionIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (paperTitle.isBlank()) {
                            Toast.makeText(context, "Please enter paper title", Toast.LENGTH_SHORT).show()
                            isConfigExpanded = true
                        } else {
                            val selectedQuestions = questions.filter { selectedQuestionIds.contains(it.id) }
                            val selectedBook = books.find { it.id == selectedBookFilter }
                            val subjectName = selectedBook?.title ?: "General Test"
                            viewModel.createPaper(
                                title = paperTitle,
                                subject = subjectName,
                                selectedQuestions = selectedQuestions,
                                durationMinutes = durationMinutes.toIntOrNull() ?: 60
                            )
                            Toast.makeText(context, "Question Paper Assembled & Saved!", Toast.LENGTH_LONG).show()
                            selectedQuestionIds.clear()
                            activeTab = 1
                        }
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = "Assemble & Save") },
                    text = { Text("Assemble & Save (${selectedQuestionIds.size})") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
        // Tab Navigation Header
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Assemble New Paper", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Build, contentDescription = null) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Saved Papers (${papers.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Description, contentDescription = null) }
            )
        }

        if (activeTab == 0) {
            // ASSEMBLE PAPER BUILDER
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isConfigExpanded) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { isConfigExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(
                                            text = paperTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        val selectedCount = selectedQuestionIds.size
                                        val totalMarks = questions.filter { selectedQuestionIds.contains(it.id) }.sumOf { it.marks }
                                        Text(
                                            text = "$selectedCount selected (${totalMarks}m) • ${durationMinutes}m • Filter: ${if (selectedBookFilter != null) books.find { it.id == selectedBookFilter }?.title ?: selectedBookFilter else "All"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val selectedQuestions = questions.filter { selectedQuestionIds.contains(it.id) }
                                    if (selectedQuestions.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                val selectedBook = books.find { it.id == selectedBookFilter }
                                                val subjectName = selectedBook?.title ?: "General Test"
                                                viewModel.createPaper(
                                                    title = paperTitle,
                                                    subject = subjectName,
                                                    selectedQuestions = selectedQuestions,
                                                    durationMinutes = durationMinutes.toIntOrNull() ?: 60
                                                )
                                                Toast.makeText(context, "Question Paper Assembled & Saved!", Toast.LENGTH_LONG).show()
                                                selectedQuestionIds.clear()
                                                activeTab = 1
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Assemble & Save", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(
                                        onClick = { isConfigExpanded = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand Configuration")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Section 1: Paper Details
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "1. Paper Configuration",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { isConfigExpanded = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Collapse Configuration")
                                    }
                                }

                                OutlinedTextField(
                                    value = paperTitle,
                                    onValueChange = { paperTitle = it },
                                    label = { Text("Question Paper Title") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("paper_title_input"),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = durationMinutes,
                                        onValueChange = { durationMinutes = it },
                                        label = { Text("Duration (Mins)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )

                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = "Filter by Subject",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            item {
                                                FilterChip(
                                                    selected = selectedBookFilter == null,
                                                    onClick = { selectedBookFilter = null },
                                                    label = { Text("All") }
                                                )
                                            }
                                            items(allSubjects) { subject ->
                                                val (subjectKey, subjectTitle) = subject
                                                val isSelected = selectedBookFilter == subjectKey || selectedBookFilter.equals(subjectTitle, ignoreCase = true)
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        selectedBookFilter = if (isSelected) null else subjectKey
                                                    },
                                                    label = { Text(subjectTitle) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Mode Toggle (Auto vs Manual)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "2. Selection Method",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isAutoAssemble) "Auto Random" else "Manual Picker",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = isAutoAssemble,
                                            onCheckedChange = { isAutoAssemble = it }
                                        )
                                    }
                                }

                                if (isAutoAssemble) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = autoMcqCount,
                                                onValueChange = { autoMcqCount = it },
                                                label = { Text("MCQs") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = autoTfCount,
                                                onValueChange = { autoTfCount = it },
                                                label = { Text("T/F") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = autoFibCount,
                                                onValueChange = { autoFibCount = it },
                                                label = { Text("FIB") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = autoSubjectiveCount,
                                                onValueChange = { autoSubjectiveCount = it },
                                                label = { Text("Subject") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val mcqCount = autoMcqCount.toIntOrNull() ?: 0
                                                val tfCount = autoTfCount.toIntOrNull() ?: 0
                                                val fibCount = autoFibCount.toIntOrNull() ?: 0
                                                val subjCount = autoSubjectiveCount.toIntOrNull() ?: 0
                                                
                                                val mcqs = availableQuestions.filter { it.type == "mcq" }.shuffled().take(mcqCount)
                                                val tfs = availableQuestions.filter { it.type == "tf" }.shuffled().take(tfCount)
                                                val fibs = availableQuestions.filter { it.type == "fib" }.shuffled().take(fibCount)
                                                val subjs = availableQuestions.filter { it.type == "subjective" }.shuffled().take(subjCount)
                                                
                                                val allSelected = mcqs + tfs + fibs + subjs
                                                selectedQuestionIds.clear()
                                                selectedQuestionIds.addAll(allSelected.map { it.id })
                                                Toast.makeText(context, "Auto-selected ${allSelected.size} questions!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Casino, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Generate Category-Wise Selection")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 3: Summary Bar & Save Button
                    item {
                        val selectedQuestions = questions.filter { selectedQuestionIds.contains(it.id) }
                        val totalMarks = selectedQuestions.sumOf { it.marks }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${selectedQuestions.size} Questions Selected",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Total Marks: $totalMarks • Duration: ${durationMinutes.toIntOrNull() ?: 60} Mins",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { isConfigExpanded = false },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Collapse")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Collapse", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            if (selectedQuestions.isEmpty()) {
                                                Toast.makeText(context, "Please select at least 1 question", Toast.LENGTH_SHORT).show()
                                            } else if (paperTitle.isBlank()) {
                                                Toast.makeText(context, "Please enter paper title", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val selectedBook = books.find { it.id == selectedBookFilter }
                                                val subjectName = selectedBook?.title ?: "General Test"
                                                viewModel.createPaper(
                                                    title = paperTitle,
                                                    subject = subjectName,
                                                    selectedQuestions = selectedQuestions,
                                                    durationMinutes = durationMinutes.toIntOrNull() ?: 60
                                                )
                                                Toast.makeText(context, "Question Paper Assembled & Saved!", Toast.LENGTH_LONG).show()
                                                selectedQuestionIds.clear()
                                                activeTab = 1 // Switch to saved papers tab
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("save_assembled_paper_btn")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Assemble & Save")
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Question Selector List
                item {
                    Text(
                        text = "3. Select Questions (${availableQuestions.size} Available)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(availableQuestions, key = { it.id }) { q ->
                    val isChecked = selectedQuestionIds.contains(q.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedQuestionIds.remove(q.id)
                                else selectedQuestionIds.add(q.id)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked == true) selectedQuestionIds.add(q.id)
                                    else selectedQuestionIds.remove(q.id)
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${q.bookTitle} • ${q.chapter}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "(${q.marks} Marks)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = q.question,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // SAVED PAPERS LIBRARY
            if (papers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Article,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No question papers assembled yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { activeTab = 0 }) {
                            Text("Assemble First Paper")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(papers, key = { it.id }) { paper ->
                        val questionCount = remember(paper.questionIdsJson) {
                            try {
                                JSONArray(paper.questionIdsJson).length()
                            } catch (e: Exception) {
                                0
                            }
                        }
                        val formattedDate = remember(paper.createdAt) {
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(paper.createdAt))
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("paper_card_${paper.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = paper.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${paper.subject} • Created on $formattedDate",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deletePaper(paper.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Paper",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$questionCount Questions",
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
                                            text = "${paper.totalMarks} Total Marks",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${paper.durationMinutes} Mins",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { previewPaper = paper },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Preview", fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = { printSetupPaper = paper },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Print PDF", fontSize = 13.sp)
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

    // PREVIEW FORMATTED PAPER DIALOG
    previewPaper?.let { paper ->
        val paperQuestions = remember(paper.questionIdsJson, questions) {
            try {
                val jsonArr = JSONArray(paper.questionIdsJson)
                val ids = mutableListOf<String>()
                for (i in 0 until jsonArr.length()) ids.add(jsonArr.getString(i))
                questions.filter { ids.contains(it.id) }
            } catch (e: Exception) {
                emptyList()
            }
        }

        PaperPreviewDialog(
            paper = paper,
            questions = paperQuestions,
            allQuestions = questions,
            onDismiss = { previewPaper = null },
            onCopyText = {
                val paperText = buildPaperTextString(paper, paperQuestions)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Question Paper", paperText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Question Paper copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            onPrintPdf = {
                printSetupPaper = paper
            },
            onUpdatePaper = { updatedPaper ->
                viewModel.updatePaper(updatedPaper)
                previewPaper = updatedPaper
            }
        )
    }

    // PRINT TO PDF SETUP DIALOG
    printSetupPaper?.let { paper ->
        val paperQuestions = remember(paper.questionIdsJson, questions) {
            try {
                val jsonArr = JSONArray(paper.questionIdsJson)
                val ids = mutableListOf<String>()
                for (i in 0 until jsonArr.length()) ids.add(jsonArr.getString(i))
                questions.filter { ids.contains(it.id) }
            } catch (e: Exception) {
                emptyList()
            }
        }

        PdfPageSetupDialog(
            paper = paper,
            questions = paperQuestions,
            onDismiss = { printSetupPaper = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperPreviewDialog(
    paper: PaperEntity,
    questions: List<QuestionEntity>,
    allQuestions: List<QuestionEntity>,
    onDismiss: () -> Unit,
    onCopyText: () -> Unit,
    onPrintPdf: () -> Unit,
    onUpdatePaper: (PaperEntity) -> Unit
) {
    val context = LocalContext.current
    var showAnswerKey by remember { mutableStateOf(false) }
    var swapTargetQuestion by remember { mutableStateOf<QuestionEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Assembled Question Paper", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(Color.White, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, shape = RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    // Header of the Exam Paper
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ACADEMIC EXAMINATION 2026",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = paper.title.uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "Subject: ${paper.subject}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Time Allowed: ${paper.durationMinutes} Mins",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Maximum Marks: ${paper.totalMarks}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.5.dp,
                            color = Color.Black
                        )
                    }

                    // Questions List inside paper

                    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
                    val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
                    
                    val displayItems = remember(questions) {
                        val grouped = questions.groupBy { it.type }.toSortedMap(compareBy { typeOrder[it] ?: 5 })
                        val list = mutableListOf<Any>()
                        var counter = 0
                        for ((type, typeQuestions) in grouped) {
                            list.add(typeNames[type] ?: "OTHER")
                            for (q in typeQuestions) {
                                list.add(Pair(counter++, q))
                            }
                        }
                        list
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayItems) { item ->
                            if (item is String) {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp).fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            } else if (item is Pair<*, *>) {
                                val idx = item.first as Int
                                val q = item.second as com.example.data.model.QuestionEntity

                            val optionsList = remember(q.optionsJson) {
                                try {
                                    val arr = JSONArray(q.optionsJson)
                                    val list = mutableListOf<String>()
                                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                                    list
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }

                            val content = @Composable {
                                Column(modifier = Modifier.fillMaxWidth().padding(if (q.type == "mcq") 8.dp else 0.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Q${idx + 1}. ${q.question}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            if (q.type == "mcq" && optionsList.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Column(
                                                    modifier = Modifier.padding(start = 12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    optionsList.forEachIndexed { optIdx, opt ->
                                                        Text(
                                                            text = "(${('A' + optIdx)}) $opt",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color.Black,
                                                            fontFamily = FontFamily.Serif
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = "[${q.marks} Marks]",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.DarkGray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        // Auto Swap
                                                        val validReplacements = allQuestions.filter { 
                                                            it.id != q.id && 
                                                            it.type == q.type && 
                                                            it.bookId == q.bookId &&
                                                            !questions.any { currentQ -> currentQ.id == it.id }
                                                        }
                                                        if (validReplacements.isNotEmpty()) {
                                                            val replacement = validReplacements.random()
                                                            val newQuestions = questions.map { if (it.id == q.id) replacement else it }
                                                            val newIdsJson = JSONArray(newQuestions.map { it.id }).toString()
                                                            val newMarks = newQuestions.sumOf { it.marks }
                                                            onUpdatePaper(paper.copy(questionIdsJson = newIdsJson, totalMarks = newMarks))
                                                        } else {
                                                            Toast.makeText(context, "No suitable replacement found for auto-swap.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Autorenew, contentDescription = "Auto Swap", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        // Manual Swap
                                                        swapTargetQuestion = q
                                                    },
                                                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                                ) {
                                                    Icon(Icons.Default.FindReplace, contentDescription = "Manual Swap", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        // Delete
                                                        val newQuestions = questions.filter { it.id != q.id }
                                                        val newIdsJson = JSONArray(newQuestions.map { it.id }).toString()
                                                        val newMarks = newQuestions.sumOf { it.marks }
                                                        onUpdatePaper(paper.copy(questionIdsJson = newIdsJson, totalMarks = newMarks))
                                                    },
                                                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Question", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (q.type == "mcq") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                ) {
                                    content()
                                }
                            } else {
                                content()
                            }
                            } // end of else if
                        }

                        if (showAnswerKey) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "--- PAGE BREAK ---",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = "ANSWER KEY & SOLUTIONS (SEPARATE PAGE)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                }
                            }

                            val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
                            val sortedQuestions = questions.sortedBy { typeOrder[it.type] ?: 5 }
                            itemsIndexed(sortedQuestions) { idx, q ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Q${idx + 1}. Answer: ${q.answer}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                        if (q.explanation.isNotBlank()) {
                                            Text(
                                                text = "Explanation: ${q.explanation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.DarkGray,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showAnswerKey = !showAnswerKey }
                        ) {
                            Checkbox(
                                checked = showAnswerKey,
                                onCheckedChange = { showAnswerKey = it }
                            )
                            Text(
                                text = "Show Answer Key",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onPrintPdf,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Print PDF", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onCopyText,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    swapTargetQuestion?.let { target ->
        var searchQuery by remember { mutableStateOf("") }
        var selectedBookFilter by remember { mutableStateOf<String?>(null) }
        var selectedTypeFilter by remember { mutableStateOf<String?>(null) }

        val availableBooks = allQuestions.map { it.bookTitle }.distinct().sorted()
        val availableTypes = allQuestions.map { it.type }.distinct().sorted()

        val filteredQuestions = allQuestions.filter { q ->
            q.id != target.id &&
            !questions.any { currentQ -> currentQ.id == q.id } &&
            (searchQuery.isBlank() || q.question.contains(searchQuery, ignoreCase = true)) &&
            (selectedBookFilter == null || q.bookTitle == selectedBookFilter) &&
            (selectedTypeFilter == null || q.type == selectedTypeFilter)
        }

        AlertDialog(
            onDismissRequest = { swapTargetQuestion = null },
            modifier = Modifier.fillMaxHeight(0.8f),
            title = { Text("Manual Swap") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Questions") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var expandedBook by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedBook = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedBookFilter ?: "All Subjects", maxLines = 1)
                            }
                            DropdownMenu(expanded = expandedBook, onDismissRequest = { expandedBook = false }) {
                                DropdownMenuItem(text = { Text("All Subjects") }, onClick = { selectedBookFilter = null; expandedBook = false })
                                availableBooks.forEach { book ->
                                    DropdownMenuItem(text = { Text(book) }, onClick = { selectedBookFilter = book; expandedBook = false })
                                }
                            }
                        }

                        var expandedType by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expandedType = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedTypeFilter ?: "All Types", maxLines = 1)
                            }
                            DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                                DropdownMenuItem(text = { Text("All Types") }, onClick = { selectedTypeFilter = null; expandedType = false })
                                availableTypes.forEach { type ->
                                    DropdownMenuItem(text = { Text(type) }, onClick = { selectedTypeFilter = type; expandedType = false })
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredQuestions) { q ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val newQuestions = questions.map { if (it.id == target.id) q else it }
                                        val newIdsJson = JSONArray(newQuestions.map { it.id }).toString()
                                        val newMarks = newQuestions.sumOf { it.marks }
                                        onUpdatePaper(paper.copy(questionIdsJson = newIdsJson, totalMarks = newMarks))
                                        swapTargetQuestion = null
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(q.question, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${q.bookTitle} • ${q.type}", style = MaterialTheme.typography.labelSmall)
                                        Text("${q.marks} Marks", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { swapTargetQuestion = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PdfPageSetupDialog(
    paper: PaperEntity,
    questions: List<QuestionEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPaperSize by remember { mutableStateOf(PaperSize.A4) }
    var marginPt by remember { mutableFloatStateOf(36f) }
    var fontBodySp by remember { mutableFloatStateOf(11f) }
    var fontTitleSp by remember { mutableFloatStateOf(15f) }
    var lineSpacingExtra by remember { mutableFloatStateOf(4f) }

    var mainTitle by remember { mutableStateOf(paper.title.ifBlank { "GEN TEST: FLT ENG" }) }
    var subTitle by remember { mutableStateOf("TECH II") }
    var paperCode by remember { mutableStateOf("QP-178566") }
    var dateStr by remember { mutableStateOf("2026-08-05") }
    var sectionHeading by remember { mutableStateOf("MULTIPLE CHOICE QUESTIONS (MCQ)") }
    var totalMarksText by remember {
        mutableStateOf("TOTAL MARKS: ${questions.size}X${questions.firstOrNull()?.marks ?: 1}=${paper.totalMarks}")
    }

    var showCandidateBox by remember { mutableStateOf(true) }
    var showGridBorders by remember { mutableStateOf(true) }
    var twoColumnOptions by remember { mutableStateOf(true) }
    var showAnswerKey by remember { mutableStateOf(false) }
    var showExplanations by remember { mutableStateOf(false) }

    val settingsManager = remember { com.example.util.SettingsManager(context) }
    var watermarkEnabled by remember { mutableStateOf(settingsManager.watermarkEnabled) }
    var watermarkText by remember { mutableStateOf(settingsManager.watermarkText) }
    var watermarkIsCursive by remember { mutableStateOf(settingsManager.watermarkIsCursive) }
    var watermarkSizeSp by remember { mutableStateOf(settingsManager.watermarkSize) }
    var watermarkOpacity by remember { mutableStateOf(settingsManager.watermarkOpacity) }
    var watermarkPattern by remember {
        mutableStateOf(
            try {
                com.example.util.WatermarkPattern.valueOf(settingsManager.watermarkPattern)
            } catch (_: Exception) {
                com.example.util.WatermarkPattern.MULTIPLE_GRID
            }
        )
    }
    var watermarkAngle by remember { mutableStateOf(settingsManager.watermarkAngle) }

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
                        Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Print / Save PDF Setup", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Paper Headings & Paper Code
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Paper Titles & Headings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = mainTitle,
                            onValueChange = { mainTitle = it },
                            label = { Text("Main Exam Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = subTitle,
                                onValueChange = { subTitle = it },
                                label = { Text("Subtitle / Branch") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = paperCode,
                                onValueChange = { paperCode = it },
                                label = { Text("Paper Code") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = { dateStr = it },
                                label = { Text("Exam Date") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = totalMarksText,
                                onValueChange = { totalMarksText = it },
                                label = { Text("Total Marks Text") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = sectionHeading,
                            onValueChange = { sectionHeading = it },
                            label = { Text("Section Heading Banner") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // 2. Paper Size & Margins
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("2. Page Size & Margin (${marginPt.toInt()} pt)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(PaperSize.values()) { size ->
                                FilterChip(
                                    selected = selectedPaperSize == size,
                                    onClick = { selectedPaperSize = size },
                                    label = { Text(size.displayName, fontSize = 11.sp) }
                                )
                            }
                        }
                        Slider(
                            value = marginPt,
                            onValueChange = { marginPt = it },
                            valueRange = 12f..72f,
                            steps = 5
                        )
                    }
                }

                // 3. Font Size & Spacing
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("3. Font Size (${fontBodySp.toInt()} pt) & Spacing (${lineSpacingExtra.toInt()} pt)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Body Font:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
                            Slider(
                                value = fontBodySp,
                                onValueChange = { fontBodySp = it },
                                valueRange = 8f..16f,
                                steps = 7,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Title Font:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
                            Slider(
                                value = fontTitleSp,
                                onValueChange = { fontTitleSp = it },
                                valueRange = 12f..24f,
                                steps = 5,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Line Extra:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
                            Slider(
                                value = lineSpacingExtra,
                                onValueChange = { lineSpacingExtra = it },
                                valueRange = 0f..12f,
                                steps = 5,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. Boxed Format & Layout Settings
                item {
                    Column {
                        Text("4. Grid Format & Layout Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showCandidateBox, onCheckedChange = { showCandidateBox = it })
                            Text("Include Candidate Details Box (Ser No / Rank / Name)", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showGridBorders, onCheckedChange = { showGridBorders = it })
                            Text("Draw Table Grid Borders around Questions", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = twoColumnOptions, onCheckedChange = { twoColumnOptions = it })
                            Text("Format MCQ Options in 2-Column Side-by-Side Grid", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showAnswerKey, onCheckedChange = { showAnswerKey = it })
                            Text("Append Answer Key & Solutions at End", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = showExplanations, onCheckedChange = { showExplanations = it })
                            Text("Include Answer Explanations in Questions", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 5. Permanent Watermark & Security Settings
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("5. Watermark & Anti-Tamper Security", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = watermarkEnabled, onCheckedChange = { watermarkEnabled = it })
                            Text("Enable Permanent Watermark in PDF", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }

                        if (watermarkEnabled) {
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                label = { Text("Watermark Text (e.g. Ravikant)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Font Style:", style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = watermarkIsCursive,
                                        onClick = { watermarkIsCursive = true },
                                        label = { Text("Cursive / Script", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = !watermarkIsCursive,
                                        onClick = { watermarkIsCursive = false },
                                        label = { Text("Standard Bold", fontSize = 11.sp) }
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Size (${watermarkSizeSp.toInt()} pt):", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(90.dp))
                                Slider(
                                    value = watermarkSizeSp,
                                    onValueChange = { watermarkSizeSp = it },
                                    valueRange = 14f..60f,
                                    steps = 23,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Opacity (${(watermarkOpacity * 100).toInt()}%):", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(90.dp))
                                Slider(
                                    value = watermarkOpacity,
                                    onValueChange = { watermarkOpacity = it },
                                    valueRange = 0.05f..0.60f,
                                    steps = 11,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text("Watermark Pattern:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(com.example.util.WatermarkPattern.values()) { pat ->
                                    FilterChip(
                                        selected = watermarkPattern == pat,
                                        onClick = { watermarkPattern = pat },
                                        label = { Text(pat.displayName, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        val settings = PdfPrintSettings(
                            paperSize = selectedPaperSize,
                            marginPt = marginPt.toInt(),
                            fontBodySp = fontBodySp,
                            fontTitleSp = fontTitleSp,
                            lineSpacingExtra = lineSpacingExtra,
                            mainTitle = mainTitle,
                            subTitle = subTitle,
                            paperCode = paperCode,
                            dateStr = dateStr,
                            totalMarksText = totalMarksText,
                            sectionHeading = sectionHeading,
                            showCandidateBox = showCandidateBox,
                            showGridBorders = showGridBorders,
                            twoColumnOptions = twoColumnOptions,
                            showAnswerKey = showAnswerKey,
                            showExplanations = showExplanations,
                            watermarkEnabled = watermarkEnabled,
                            watermarkText = watermarkText,
                            watermarkIsCursive = watermarkIsCursive,
                            watermarkSizeSp = watermarkSizeSp,
                            watermarkOpacity = watermarkOpacity,
                            watermarkPattern = watermarkPattern,
                            watermarkAngle = watermarkAngle
                        )
                        val pdfFile = PdfPrintUtils.generatePdfFile(context, paper, questions, settings)
                        if (pdfFile != null) {
                            PdfPrintUtils.printPdf(context, pdfFile, paper.title)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print Document / Save PDF")
                }

                OutlinedButton(
                    onClick = {
                        val settings = PdfPrintSettings(
                            paperSize = selectedPaperSize,
                            marginPt = marginPt.toInt(),
                            fontBodySp = fontBodySp,
                            fontTitleSp = fontTitleSp,
                            lineSpacingExtra = lineSpacingExtra,
                            mainTitle = mainTitle,
                            subTitle = subTitle,
                            paperCode = paperCode,
                            dateStr = dateStr,
                            totalMarksText = totalMarksText,
                            sectionHeading = sectionHeading,
                            showCandidateBox = showCandidateBox,
                            showGridBorders = showGridBorders,
                            twoColumnOptions = twoColumnOptions,
                            showAnswerKey = showAnswerKey,
                            showExplanations = showExplanations,
                            watermarkEnabled = watermarkEnabled,
                            watermarkText = watermarkText,
                            watermarkIsCursive = watermarkIsCursive,
                            watermarkSizeSp = watermarkSizeSp,
                            watermarkOpacity = watermarkOpacity,
                            watermarkPattern = watermarkPattern,
                            watermarkAngle = watermarkAngle
                        )
                        val pdfFile = PdfPrintUtils.generatePdfFile(context, paper, questions, settings)
                        if (pdfFile != null) {
                            PdfPrintUtils.sharePdf(context, pdfFile, paper.title)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share PDF File")
                }

                OutlinedButton(
                    onClick = {
                        val settings = PdfPrintSettings(
                            paperSize = selectedPaperSize,
                            marginPt = marginPt.toInt(),
                            fontBodySp = fontBodySp,
                            fontTitleSp = fontTitleSp,
                            lineSpacingExtra = lineSpacingExtra,
                            mainTitle = mainTitle,
                            subTitle = subTitle,
                            paperCode = paperCode,
                            dateStr = dateStr,
                            totalMarksText = totalMarksText,
                            sectionHeading = sectionHeading,
                            showCandidateBox = showCandidateBox,
                            showGridBorders = showGridBorders,
                            twoColumnOptions = twoColumnOptions,
                            showAnswerKey = showAnswerKey,
                            showExplanations = showExplanations,
                            watermarkEnabled = watermarkEnabled,
                            watermarkText = watermarkText,
                            watermarkIsCursive = watermarkIsCursive,
                            watermarkSizeSp = watermarkSizeSp,
                            watermarkOpacity = watermarkOpacity,
                            watermarkPattern = watermarkPattern,
                            watermarkAngle = watermarkAngle
                        )
                        val pdfFile = PdfPrintUtils.generatePdfFile(context, paper, questions, settings)
                        if (pdfFile != null) {
                            PdfPrintUtils.sharePdfViaEmail(context, pdfFile, paper.title)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send via Email")
                }

                OutlinedButton(
                    onClick = {
                        val settings = PdfPrintSettings(
                            paperSize = selectedPaperSize,
                            marginPt = marginPt.toInt(),
                            fontBodySp = fontBodySp,
                            fontTitleSp = fontTitleSp,
                            lineSpacingExtra = lineSpacingExtra,
                            mainTitle = mainTitle,
                            subTitle = subTitle,
                            paperCode = paperCode,
                            dateStr = dateStr,
                            totalMarksText = totalMarksText,
                            sectionHeading = sectionHeading,
                            showCandidateBox = showCandidateBox,
                            showGridBorders = showGridBorders,
                            twoColumnOptions = twoColumnOptions,
                            showAnswerKey = showAnswerKey,
                            showExplanations = showExplanations,
                            watermarkEnabled = watermarkEnabled,
                            watermarkText = watermarkText,
                            watermarkIsCursive = watermarkIsCursive,
                            watermarkSizeSp = watermarkSizeSp,
                            watermarkOpacity = watermarkOpacity,
                            watermarkPattern = watermarkPattern,
                            watermarkAngle = watermarkAngle
                        )
                        val pdfFile = PdfPrintUtils.generatePdfFile(context, paper, questions, settings)
                        if (pdfFile != null) {
                            PdfPrintUtils.sharePdfViaWhatsApp(context, pdfFile, paper.title)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send via WhatsApp")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun buildPaperTextString(paper: PaperEntity, questions: List<QuestionEntity>): String {
    val sb = StringBuilder()
    sb.appendLine("==========================================")
    sb.appendLine("          ${paper.title.uppercase()}")
    sb.appendLine("Subject: ${paper.subject}")
    sb.appendLine("Time Allowed: ${paper.durationMinutes} Mins | Max Marks: ${paper.totalMarks}")
    sb.appendLine("==========================================")
    sb.appendLine()

    val typeOrder = mapOf("mcq" to 1, "tf" to 2, "fib" to 3, "subjective" to 4)
    val typeNames = mapOf("mcq" to "MULTIPLE CHOICE QUESTIONS (MCQ)", "tf" to "TRUE / FALSE", "fib" to "FILL IN THE BLANKS", "subjective" to "SUBJECTIVE QUESTIONS")
    
    val grouped = questions.groupBy { it.type }.toSortedMap(compareBy { typeOrder[it] ?: 5 })
    
    var questionCounter = 1
    for ((type, list) in grouped) {
        sb.appendLine(typeNames[type] ?: "OTHER")
        sb.appendLine("------------------------------------------")
        list.forEach { q ->
            sb.appendLine("Q${questionCounter}. ${q.question}  [${q.marks} Marks]")
            try {
                val arr = JSONArray(q.optionsJson)
                for (i in 0 until arr.length()) {
                    val optChar = ('A' + i)
                    sb.appendLine("   ($optChar) ${arr.getString(i)}")
                }
            } catch (e: Exception) {
                // Ignore options formatting error
            }
            sb.appendLine()
            questionCounter++
        }
        sb.appendLine()
    }

    sb.appendLine("\n\n------------------------------------------")
    sb.appendLine(" PAGE BREAK: ANSWER KEY (SEPARATE PAGE)   ")
    sb.appendLine("------------------------------------------")
    sb.appendLine("==========================================")
    sb.appendLine("               ANSWER KEY                 ")
    sb.appendLine("==========================================")
    val sortedQuestionsForAns = questions.sortedBy { typeOrder[it.type] ?: 5 }
    sortedQuestionsForAns.forEachIndexed { idx, q ->
        sb.appendLine("Q${idx + 1}: ${q.answer}")
    }

    return sb.toString()
}
