package com.example.ui.screens

import android.telephony.SmsManager
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.viewmodel.OtsViewModel
import com.example.util.SettingsManager
import com.example.util.LiveTestState
import com.example.data.model.PaperEntity
import com.example.data.model.QuestionEntity
import androidx.compose.foundation.lazy.itemsIndexed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivesScreen(
    viewModel: OtsViewModel,
    settingsManager: SettingsManager,
    screenMode: String = "livetest", // "livetest", "admin", "expert"
    onOpenMonitor: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    val screenTitle = when (screenMode) {
        "admin" -> "Admin Mode Server"
        "expert" -> "Expert Review Server"
        else -> "Live Test Portal"
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(screenTitle) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                )
                if (screenMode == "livetest") {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Live Server") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Submissions & Results") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("PDF Archives") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (selectedTab == 0 || screenMode != "livetest") {
            val context = androidx.compose.ui.platform.LocalContext.current
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    


            val webServerUrl by viewModel.webServerUrl.collectAsState()
            val webServerHttpUrl by viewModel.webServerHttpUrl.collectAsState()
            val webServerPublicUrl by viewModel.webServerPublicUrl.collectAsState()
            var publicTunnelInput by remember { mutableStateOf(viewModel.publicTunnelUrl) }
            LaunchedEffect(webServerPublicUrl) {
                if (!webServerPublicUrl.isNullOrBlank()) {
                    publicTunnelInput = webServerPublicUrl ?: ""
                }
            }
            val serverError by com.example.util.WebServerState.error.collectAsState()
            var adminUser by remember { mutableStateOf(settingsManager.webAdminUser) }
            var adminPass by remember { mutableStateOf(settingsManager.webAdminPass) }
            val serverMode by com.example.util.WebServerState.mode.collectAsState()
            val candidates by viewModel.liveCandidates.collectAsState()
            
            val booksList by viewModel.books.collectAsState()
            val uniqueSubjects = remember(booksList) {
                listOf("") + booksList.map { it.title }.distinct().sorted()
            }
            
            val papersList by viewModel.papers.collectAsState()
            val selectedLivePaperId by viewModel.selectedLivePaperId.collectAsState()
            val selectedPaper = remember(papersList, selectedLivePaperId) {
                papersList.find { it.id == selectedLivePaperId }
            }
            
            // Expert Review States
            val allQuestions by viewModel.questions.collectAsState()
            var selectedExpertPaperId by remember { mutableStateOf<String?>(null) }
            val selectedExpertPaper = remember(papersList, selectedExpertPaperId) {
                papersList.find { it.id == selectedExpertPaperId }
            }
            var showExpertReviewDialog by remember { mutableStateOf(false) }
            
            LaunchedEffect(selectedExpertPaperId) {
                com.example.util.LiveTestState.selectedExpertPaperId = selectedExpertPaperId
            }
            
            val liveExamName by viewModel.liveTestExamName.collectAsState()
            val liveSubject by viewModel.liveTestSubject.collectAsState()
            val liveMcqCount by viewModel.liveTestMcqCount.collectAsState()
            val liveFibCount by viewModel.liveTestFibCount.collectAsState()
            val liveTfCount by viewModel.liveTestTfCount.collectAsState()
            val liveDuration by viewModel.liveTestDuration.collectAsState()
            
            var reviewCandidate by remember { mutableStateOf<com.example.util.CandidateSession?>(null) }
            var marksheetCandidate by remember { mutableStateOf<com.example.util.CandidateSession?>(null) }

            // Dialog: Candidate Review Answers
            if (reviewCandidate != null) {
                val candidate = reviewCandidate!!
                val assignedQuestions = remember(candidate) {
                    try {
                        kotlinx.serialization.json.Json.decodeFromString<List<com.example.util.QuestionDto>>(candidate.questionsJson)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val submittedAnswers = remember(candidate) {
                    try {
                        kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(candidate.answersJson)
                    } catch (e: Exception) {
                        emptyMap()
                    }
                }
                
                AlertDialog(
                    onDismissRequest = { reviewCandidate = null },
                    title = { Text("Review Answers: ${candidate.name}") },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            Text("Roll Number: ${candidate.rollNumber}", style = MaterialTheme.typography.titleSmall)
                            Text("Security Warnings: ${candidate.warningCount} / 3", color = if (candidate.warningCount > 0) Color.Red else Color.Unspecified)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(assignedQuestions) { q ->
                                    val studentAns = submittedAnswers[q.id]?.trim() ?: ""
                                    val isCorrect = studentAns.isNotEmpty() && studentAns.equals(q.answer.trim(), ignoreCase = true)
                                    
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCorrect) Color(0xFFE8F5E9) else if (studentAns.isEmpty()) Color(0xFFF5F5F5) else Color(0xFFFFEBEE)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text(text = "Type: ${q.type.uppercase()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                                Text(text = "[${q.marks} Marks]", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = q.question, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Correct Answer: ${q.answer}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                                            Text(
                                                text = "Candidate Answer: ${if (studentAns.isEmpty()) "(Not Answered)" else studentAns}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { reviewCandidate = null }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Dialog: Candidate Marksheet
            if (marksheetCandidate != null) {
                val candidate = marksheetCandidate!!
                val assignedQuestions = remember(candidate) {
                    try {
                        kotlinx.serialization.json.Json.decodeFromString<List<com.example.util.QuestionDto>>(candidate.questionsJson)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val submittedAnswers = remember(candidate) {
                    try {
                        kotlinx.serialization.json.Json.decodeFromString<Map<String, String>>(candidate.answersJson)
                    } catch (e: Exception) {
                        emptyMap()
                    }
                }
                
                val totalQ = assignedQuestions.size
                var correctCount = 0
                var wrongCount = 0
                assignedQuestions.forEach { q ->
                    val studentAns = submittedAnswers[q.id]?.trim() ?: ""
                    if (studentAns.isNotEmpty() && studentAns.equals(q.answer.trim(), ignoreCase = true)) {
                        correctCount++
                    } else if (studentAns.isNotEmpty()) {
                        wrongCount++
                    }
                }
                val unansweredCount = totalQ - correctCount - wrongCount
                
                AlertDialog(
                    onDismissRequest = { marksheetCandidate = null },
                    title = {
                        Text(
                            "Official Academic Scorecard",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 450.dp)
                                .background(Color.White, shape = RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "QUESTIONBANK INSTITUTE OF EXAMINATIONS",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "VERIFIED TRANSCRIPT OF PERFORMANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Candidate info block
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Candidate Name:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(candidate.name, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Roll Number:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(candidate.rollNumber, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Session Date:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    val formattedDate = remember(candidate.loginTime) {
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                        sdf.format(java.util.Date(candidate.loginTime))
                                    }
                                    Text(formattedDate, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Security Status:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = if (candidate.status == "Disqualified") "DISQUALIFIED" else if (candidate.warningCount > 0) "SECURED (With Warnings)" else "FULLY SECURED",
                                        color = if (candidate.status == "Disqualified") Color.Red else if (candidate.warningCount > 0) Color(0xFFD97706) else Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Performance metrics
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Correct", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("$correctCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF2E7D32))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Incorrect", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("$wrongCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFFC62828))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Unanswered", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    Text("$unansweredCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Core Score Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val percent = if (candidate.totalMarks > 0) {
                                        (candidate.score.toFloat() / candidate.totalMarks.toFloat() * 100f)
                                    } else 0f
                                    val formattedPercent = String.format(java.util.Locale.US, "%.1f", percent)
                                    
                                    Text("GRAND TOTAL SCORE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${candidate.score} / ${candidate.totalMarks} Marks",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "Percentage Score: $formattedPercent%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Result Verdict: " + if (candidate.status == "Disqualified") "DISQUALIFIED / FAIL" else if (candidate.score >= candidate.totalMarks * 0.4) "PASS" else "FAIL",
                                fontWeight = FontWeight.Bold,
                                color = if (candidate.status != "Disqualified" && candidate.score >= candidate.totalMarks * 0.4) Color(0xFF2E7D32) else Color(0xFFC62828),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            Toast.makeText(context, "Marksheet PDF Saved to /Downloads successfully!", Toast.LENGTH_LONG).show()
                            marksheetCandidate = null
                        }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download PDF")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { marksheetCandidate = null }) {
                            Text("Close")
                        }
                    }
                )
            }
            
            // Expert Paper Review Dialog Trigger
            if (showExpertReviewDialog && selectedExpertPaper != null) {
                val paperQuestions = remember(selectedExpertPaper, allQuestions) {
                    try {
                        val arr = org.json.JSONArray(selectedExpertPaper.questionIdsJson)
                        val ids = mutableListOf<String>()
                        for (i in 0 until arr.length()) ids.add(arr.getString(i))
                        ids.mapNotNull { id -> allQuestions.find { it.id == id } }
                    } catch (e: Exception) {
                        emptyList<QuestionEntity>()
                    }
                }
                
                ExpertPaperReviewDialog(
                    paper = selectedExpertPaper,
                    questions = paperQuestions,
                    allQuestions = allQuestions,
                    onDismiss = { showExpertReviewDialog = false },
                    onUpdatePaper = { updatedPaper ->
                        viewModel.updatePaper(updatedPaper)
                        showExpertReviewDialog = false
                    }
                )
            }
            
            if (screenMode == "expert") {
                SettingsCategory(
                    title = "Expert Paper Review & Edit",
                    icon = { Icon(Icons.Default.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                    defaultExpanded = true
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select any saved exam paper from the database to perform expert peer evaluation, review questions, swap, delete, or append new questions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Option to select a saved paper
                        var expandedPaper by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedExpertPaper?.title ?: "Select a saved paper...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Choose Paper for Expert Review") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (selectedExpertPaper != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { expandedPaper = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { expandedPaper = true }
                            )
                            DropdownMenu(
                                expanded = expandedPaper,
                                onDismissRequest = { expandedPaper = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("None") },
                                    onClick = {
                                        selectedExpertPaperId = null
                                        expandedPaper = false
                                    }
                                )
                                papersList.forEach { paper ->
                                    DropdownMenuItem(
                                        text = { Text("${paper.title} (${paper.subject})") },
                                        onClick = {
                                            selectedExpertPaperId = paper.id
                                            expandedPaper = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        if (selectedExpertPaper != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Subject:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(selectedExpertPaper.subject, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Duration:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${selectedExpertPaper.durationMinutes} minutes", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Questions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        val count = try {
                                            org.json.JSONArray(selectedExpertPaper.questionIdsJson).length()
                                        } catch (e: Exception) {
                                            0
                                        }
                                        Text("$count questions", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Marks:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${selectedExpertPaper.totalMarks} Marks", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { showExpertReviewDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit & Review Questions", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            SettingsCategory(
                title = "Remote Web Dashboard",
                icon = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {
                Text(
                    text = "Manage your database or run live secured test halls directly from your PC browser.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (serverError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Server Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(serverError ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if (webServerUrl != null) {
                    if (serverMode != screenMode && serverMode != "all") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Another Server is Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("A server is already running in ${serverMode?.uppercase()} mode.", style = MaterialTheme.typography.bodyMedium)
                                Text("Please stop the active server before starting ${screenTitle}.", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.stopWebServer() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Stop Running Server")
                                }
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val modeLabel = when(serverMode) {
                                    "all" -> "Unified Server (All 3 Portals Live)"
                                    "livetest" -> "Live Test Portal"
                                    "expert" -> "Expert Review"
                                    else -> "Desktop Admin"
                                }
                                Text("Server Active [$modeLabel]", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // 1. Public Internet Section (if tunnel configured)
                                if (webServerPublicUrl != null) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        border = BorderStroke(1.dp, Color(0xFF81C784))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("🌐 Public Internet Link (Candidates Anywhere):", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                            val publicCandidateUrl = webServerPublicUrl ?: ""
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                Text(publicCandidateUrl, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), modifier = Modifier.weight(1f))
                                                IconButton(onClick = {
                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Exam Link", publicCandidateUrl))
                                                    Toast.makeText(context, "Copied Exam Link!", Toast.LENGTH_SHORT).show()
                                                }) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF1B5E20))
                                                }
                                                IconButton(onClick = {
                                                    val sendIntent = android.content.Intent().apply {
                                                        action = android.content.Intent.ACTION_SEND
                                                        putExtra(android.content.Intent.EXTRA_TEXT, "Online Examination Link: $publicCandidateUrl")
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Exam Link"))
                                                }) {
                                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF1B5E20))
                                                }
                                            }
                                            Text("Supervisors Monitor: $publicCandidateUrl/admin", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                            if (serverMode == "all") {
                                                Text("Admin: $publicCandidateUrl/dashboard | Expert: $publicCandidateUrl/expert", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                                            }
                                        }
                                    }
                                }

                                // 2. Local LAN Section
                                if (serverMode == "livetest" || serverMode == "all") {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("📱 Local LAN Candidate Exam URL:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                            Text(webServerUrl ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("Share this URL with students on the same Wi-Fi", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("💻 Supervisor AV Monitor URL:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                            Text("${webServerUrl}/admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                            Text("Login: admin / 1234 (For desktop proctoring)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = onOpenMonitor,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open Live A/V Monitor (In-App)", fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                if (serverMode == "admin" || serverMode == "all") {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("🖥️ Admin Dashboard URL:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                            val dashUrl = if (serverMode == "all") "${webServerUrl}/dashboard" else (webServerUrl ?: "")
                                            Text(dashUrl, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("Login credentials: admin / 1234", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }

                                if (serverMode == "expert" || serverMode == "all") {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("📝 Expert Review URL:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                            val expertUrl = if (serverMode == "all") "${webServerUrl}/expert" else (webServerUrl ?: "")
                                            Text(expertUrl, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("Login credentials: admin / 1234", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                }

                                if (webServerHttpUrl != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Tunnel Target Port: $webServerHttpUrl (HTTP)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { viewModel.stopWebServer() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Stop Running Server")
                                }
                            }
                        }
                    }
                    
                    // Live Test Session Supervisor Panel
                    if (serverMode == "livetest" || serverMode == "all") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Live Test Session Supervisor",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(onClick = { viewModel.clearLiveTestSessions() }) {
                                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Active Registered Candidates: ${candidates.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val submittedCount = candidates.count { it.status == "Submitted" }
                                    Text(
                                        "Submitted: $submittedCount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))

                                // Show Active Live Test Paper and Timing Setup under Live Test Server Mode
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Active Exam Configuration",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Subject: " + if (liveSubject.isEmpty()) "All Subjects" else liveSubject,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text("MCQs: $liveMcqCount", style = MaterialTheme.typography.bodySmall)
                                            Text("FIBs: $liveFibCount", style = MaterialTheme.typography.bodySmall)
                                            Text("T/F: $liveTfCount", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Duration: $liveDuration Minutes",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                 val isExamCompleted = candidates.isNotEmpty() && candidates.none { it.status == "Testing" }
                                 val testingCount = candidates.count { it.status == "Testing" }

                                 // Printable Merit List & Scorecards Button
                                 Button(
                                     onClick = {
                                         val reportFile = com.example.util.PdfPrintUtils.generateLiveTestReportPdf(
                                             context,
                                             candidates,
                                             liveSubject,
                                             liveDuration
                                         )
                                         if (reportFile != null) {
                                             com.example.util.PdfPrintUtils.printPdf(context, reportFile, "Live_Test_Report_Merit_List")
                                         } else {
                                             Toast.makeText(context, "Failed to generate report PDF", Toast.LENGTH_SHORT).show()
                                         }
                                     },
                                     modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                     colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                     enabled = isExamCompleted
                                 ) {
                                     Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                     Spacer(modifier = Modifier.width(8.dp))
                                     Text(if (testingCount > 0) "Compile Merit List ($testingCount Testing...)" else "Print Merit List & Scorecards (PDF)")
                                 }

                                 // Compile & Dispatch All Results Button
                                 Button(
                                     onClick = {
                                         viewModel.dispatchAllCompletedCandidates(context)
                                     },
                                     modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                                     enabled = isExamCompleted
                                 ) {
                                     Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                     Spacer(modifier = Modifier.width(8.dp))
                                     Text(if (testingCount > 0) "Compile & Dispatch ($testingCount In Progress)" else "Compile & Dispatch All Results")
                                 }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (candidates.isEmpty()) {
                                    Text(
                                        "Waiting for candidates to login...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        candidates.forEach { session ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                modifier = Modifier.fillMaxWidth(),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Row {
                                                            val portraitBmp = decodeBase64Image(session.latestFrameBase64.ifEmpty { session.portraitBase64 })
                                                            if (portraitBmp != null) {
                                                                Image(
                                                                    bitmap = portraitBmp,
                                                                    contentDescription = "Candidate Portrait",
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                                                )
                                                                Spacer(modifier = Modifier.width(12.dp))
                                                            }
                                                            Column {
                                                                Text(session.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                                Text("Roll: ${session.rollNumber}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                                if (session.mobile.isNotEmpty()) {
                                                                    Text("Mob: ${session.mobile}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                                }
                                                            }
                                                        }
                                                        
                                                        val badgeBg = if (session.status == "Testing") Color(0xFFFEF3C7) else if (session.status == "Disqualified") Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
                                                        val badgeText = if (session.status == "Testing") Color(0xFFD97706) else if (session.status == "Disqualified") Color(0xFFB91C1C) else Color(0xFF047857)
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(badgeBg)
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                session.status,
                                                                color = badgeText,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    if (session.warningCount > 0) {
                                                        Text(
                                                            text = "⚠️ Warnings: ${session.warningCount} / 3",
                                                            color = Color(0xFFB91C1C),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(bottom = 8.dp)
                                                        )
                                                    }
                                                    
                                                    if (session.status == "Testing") {
                                                        Text("Current Q: ${session.currentQuestionId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Button(
                                                                onClick = { com.example.util.LiveTestState.requestForceSubmit(session.rollNumber) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                                modifier = Modifier.weight(1f).height(32.dp),
                                                                contentPadding = PaddingValues(0.dp)
                                                            ) { Text("Force Submit", style = MaterialTheme.typography.labelSmall) }
                                                            Button(
                                                                onClick = { com.example.util.LiveTestState.setWarning(session.rollNumber, "Suspicious Activity Detected") },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                                                modifier = Modifier.weight(1f).height(32.dp),
                                                                contentPadding = PaddingValues(0.dp)
                                                            ) { Text("Warn", style = MaterialTheme.typography.labelSmall) }
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Button(
                                                            onClick = { marksheetCandidate = session },
                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                                            modifier = Modifier.fillMaxWidth().height(32.dp),
                                                            contentPadding = PaddingValues(0.dp)
                                                        ) { Text("View Live Paper", style = MaterialTheme.typography.labelSmall) }
                                                    }
                                                    if (session.status != "Testing") {
                                                        Text(
                                                            text = "Score: ${session.score} / ${session.totalMarks} Marks",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(bottom = 8.dp)
                                                        )
                                                    }
                                                    
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        OutlinedButton(
                                                            onClick = { reviewCandidate = session },
                                                            modifier = Modifier.weight(1.1f).height(36.dp),
                                                            contentPadding = PaddingValues(0.dp)
                                                        ) {
                                                            Text("Review", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        OutlinedButton(
                                                            onClick = { marksheetCandidate = session },
                                                            modifier = Modifier.weight(1.3f).height(36.dp),
                                                            contentPadding = PaddingValues(0.dp)
                                                        ) {
                                                            Text("Marksheet", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        
                                                        if (session.isDispatched) {
                                                            Button(
                                                                onClick = {},
                                                                enabled = false,
                                                                modifier = Modifier.weight(1.5f).height(36.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                                contentPadding = PaddingValues(0.dp)
                                                            ) {
                                                                Text("Dispatched ✓", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                                            }
                                                        } else if (!isExamCompleted) {
                                                            Button(
                                                                onClick = {},
                                                                enabled = false,
                                                                modifier = Modifier.weight(1.5f).height(36.dp),
                                                                contentPadding = PaddingValues(0.dp),
                                                                colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFCBD5E1), disabledContentColor = Color(0xFF64748B))
                                                            ) {
                                                                Text(if (session.status == "Testing") "Testing..." else "Wait for End", style = MaterialTheme.typography.bodySmall)
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = {
                                                                    viewModel.dispatchCandidateMarksheet(session.rollNumber)
                                                                    Toast.makeText(context, "Marksheet successfully dispatched to candidate ${session.name}!", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.weight(1.5f).height(36.dp),
                                                                contentPadding = PaddingValues(0.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                                            ) {
                                                                Text("Dispatch & SMS", style = MaterialTheme.typography.bodySmall)
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    if (screenMode == "livetest") {
                        Button(
                            onClick = {
                                viewModel.stopWebServer()
                                viewModel.generateMeritListPdf(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("End Exam & Generate Merit List")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Button(onClick = { viewModel.stopWebServer() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Web Server")
                    }
                } else {
                    if (screenMode == "livetest") {
                        // --- Live Test Configuration Card (Only displayed before server starts) ---
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Live Test Paper & Timing Setup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Option to select a saved paper
                            var expandedPaper by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedPaper?.title ?: "Generate dynamically from question pools",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Use Saved Paper (Optional)") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (selectedPaper != null) Icons.Default.Description else Icons.Default.Casino,
                                            contentDescription = null,
                                            tint = if (selectedPaper != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { expandedPaper = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { expandedPaper = true }
                                )
                                DropdownMenu(
                                    expanded = expandedPaper,
                                    onDismissRequest = { expandedPaper = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Generate dynamically from question pools (None)") },
                                        leadingIcon = { Icon(Icons.Default.Casino, contentDescription = null) },
                                        onClick = {
                                            viewModel.selectPaperForLiveTest(null)
                                            expandedPaper = false
                                        }
                                    )
                                    papersList.forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text("${p.title} (${p.subject} - ${p.durationMinutes}m)") },
                                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                            onClick = {
                                                viewModel.selectPaperForLiveTest(p)
                                                expandedPaper = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (selectedPaper != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Using saved paper. MCQ/FIB/TF counts are fixed from the paper structure.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            OutlinedTextField(
                                value = liveExamName,
                                onValueChange = { viewModel.updateLiveTestConfig(it, liveSubject, liveMcqCount, liveFibCount, liveTfCount, liveDuration, viewModel.liveStartTimeInput.value, keepPaper = selectedPaper != null) },
                                label = { Text("Exam Name (For Candidate Portal)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // 1. Subject Select Dropdown
                            var expandedSubj by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = if (liveSubject.isEmpty()) "All Subjects" else liveSubject,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = selectedPaper == null,
                                    label = { Text("Select Exam Subject") },
                                    trailingIcon = {
                                        if (selectedPaper == null) {
                                            IconButton(onClick = { expandedSubj = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().then(
                                        if (selectedPaper == null) Modifier.clickable { expandedSubj = true } else Modifier
                                    )
                                )
                                if (selectedPaper == null) {
                                    DropdownMenu(
                                        expanded = expandedSubj,
                                        onDismissRequest = { expandedSubj = false },
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        uniqueSubjects.forEach { s ->
                                            DropdownMenuItem(
                                                text = { Text(if (s.isEmpty()) "All Subjects" else s) },
                                                onClick = {
                                                    viewModel.updateLiveTestConfig(liveExamName, s, liveMcqCount, liveFibCount, liveTfCount, liveDuration, viewModel.liveStartTimeInput.value)
                                                    expandedSubj = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 2. Type-wise total counts
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = if (selectedPaper != null) "Fixed" else liveMcqCount.toString(),
                                    onValueChange = {
                                        val count = it.toIntOrNull() ?: 0
                                        viewModel.updateLiveTestConfig(liveExamName, liveSubject, count, liveFibCount, liveTfCount, liveDuration, viewModel.liveStartTimeInput.value)
                                    },
                                    enabled = selectedPaper == null,
                                    label = { Text("MCQs") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = if (selectedPaper != null) "Fixed" else liveFibCount.toString(),
                                    onValueChange = {
                                        val count = it.toIntOrNull() ?: 0
                                        viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, count, liveTfCount, liveDuration, viewModel.liveStartTimeInput.value)
                                    },
                                    enabled = selectedPaper == null,
                                    label = { Text("FIBs") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = if (selectedPaper != null) "Fixed" else liveTfCount.toString(),
                                    onValueChange = {
                                        val count = it.toIntOrNull() ?: 0
                                        viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, liveFibCount, count, liveDuration, viewModel.liveStartTimeInput.value)
                                    },
                                    enabled = selectedPaper == null,
                                    label = { Text("T/F") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 3. Duration input
                            OutlinedTextField(
                                value = liveDuration.toString(),
                                onValueChange = {
                                    val mins = it.toIntOrNull() ?: 30
                                    viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, liveFibCount, liveTfCount, mins, viewModel.liveStartTimeInput.value)
                                },
                                enabled = selectedPaper == null,
                                label = { Text("Exam Duration (Minutes)") },
                                trailingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
            var showDatePicker by remember { mutableStateOf(false) }
            var showTimePicker by remember { mutableStateOf(false) }
            val calendar = remember { java.util.Calendar.getInstance() }

            if (showDatePicker) {
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(java.util.Calendar.YEAR, year)
                        calendar.set(java.util.Calendar.MONTH, month)
                        calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                        showDatePicker = false
                        showTimePicker = true
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                ).apply { 
                    setOnCancelListener { showDatePicker = false }
                }.show()
            }
            
            if (showTimePicker) {
                android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(java.util.Calendar.MINUTE, minute)
                        calendar.set(java.util.Calendar.SECOND, 0)
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, liveFibCount, liveTfCount, liveDuration, sdf.format(calendar.time), keepPaper = selectedPaper != null)
                        showTimePicker = false
                    },
                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE),
                    true
                ).apply {
                    setOnCancelListener { showTimePicker = false }
                }.show()
            }
                            // Start Time input
                            OutlinedTextField(
                                value = viewModel.liveStartTimeInput.value,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Scheduled Start Date & Time") },
                                placeholder = { Text("Leave empty to start immediately") },
                                trailingIcon = { 
                                    Row {
                                        if (viewModel.liveStartTimeInput.value.isNotBlank()) {
                                            IconButton(onClick = { viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, liveFibCount, liveTfCount, liveDuration, "", keepPaper = selectedPaper != null) }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                        IconButton(onClick = { showDatePicker = true }) {
                                            Icon(Icons.Default.Schedule, contentDescription = "Pick Time")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                            )

                        }
                    }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Web Monitor Credentials (For Browser)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adminUser,
                                onValueChange = { 
                                    adminUser = it
                                    settingsManager.webAdminUser = it
                                },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adminPass,
                                onValueChange = { 
                                    adminPass = it
                                    settingsManager.webAdminPass = it
                                },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("🌐 Public Internet Tunnel (Optional)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Enter Cloudflare Tunnel or Ngrok URL for candidate tests outside local Wi-Fi:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = publicTunnelInput,
                                onValueChange = { 
                                    publicTunnelInput = it
                                    viewModel.publicTunnelUrl = it
                                },
                                label = { Text("Public URL (e.g. https://xxx.trycloudflare.com)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    if (publicTunnelInput.isNotBlank()) {
                                        IconButton(onClick = { 
                                            publicTunnelInput = ""
                                            viewModel.publicTunnelUrl = ""
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.startWebServer("all", adminUser, adminPass) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Unified Server (All 3 Portals)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (screenMode) {
                        "admin" -> {
                            OutlinedButton(
                                onClick = { viewModel.startWebServer("admin", adminUser, adminPass) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Admin Server Only")
                            }
                        }
                        "expert" -> {
                            OutlinedButton(
                                onClick = { viewModel.startWebServer("expert", adminUser, adminPass) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Expert Review Server Only")
                            }
                        }
                        else -> {
                            OutlinedButton(
                                onClick = { viewModel.startWebServer("livetest", adminUser, adminPass) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Live Test Server Only")
                            }
                        }
                    }
                }
            }
        
                }
            }
        } else if (selectedTab == 1) {
            SubmissionsEvaluationTabContent(padding, viewModel, settingsManager)
        } else {
            ArchivesTabContent(padding)
        }
    }
}

@Composable
fun ArchivesTabContent(padding: PaddingValues) {
    val context = LocalContext.current
    var pdfFiles by remember { mutableStateOf(emptyList<File>()) }

    LaunchedEffect(Unit) {
        val dir = File(context.filesDir, "ExamArchives")
        if (dir.exists()) {
            pdfFiles = dir.listFiles { file -> file.name.endsWith(".pdf") }?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
        }
    }
    
    if (pdfFiles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("No archived exam reports found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pdfFiles) { file ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.nameWithoutExtension.replace("_", " "), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
                            Text("Archived: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = { com.example.util.PdfPrintUtils.printPdf(context, file, "Archived_Report") }) {
                            Text("View/Print")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpertPaperReviewDialog(
    paper: com.example.data.model.PaperEntity,
    questions: List<com.example.data.model.QuestionEntity>,
    allQuestions: List<com.example.data.model.QuestionEntity>,
    onDismiss: () -> Unit,
    onUpdatePaper: (com.example.data.model.PaperEntity) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf(paper.title) }
    var durationMinutes by remember { mutableStateOf(paper.durationMinutes.toString()) }
    var subject by remember { mutableStateOf(paper.subject) }
    
    var currentQuestions by remember { mutableStateOf(questions) }
    
    // Pickers states
    var swapTargetIndex by remember { mutableStateOf<Int?>(null) }
    var showQuestionPicker by remember { mutableStateOf(false) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Expert Review & Edit: ${paper.title}", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "Paper title cannot be blank.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val duration = durationMinutes.toIntOrNull() ?: 0
                                if (duration <= 0) {
                                    Toast.makeText(context, "Please enter a valid duration.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val questionIds = currentQuestions.map { it.id }
                                val updatedPaper = paper.copy(
                                    title = title,
                                    durationMinutes = duration,
                                    subject = subject,
                                    questionIdsJson = org.json.JSONArray(questionIds).toString(),
                                    totalMarks = currentQuestions.sumOf { it.marks }
                                )
                                onUpdatePaper(updatedPaper)
                                Toast.makeText(context, "Paper updated successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Paper")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Paper Details Setup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Exam Paper Title") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = subject,
                                        onValueChange = { subject = it },
                                        label = { Text("Subject") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = durationMinutes,
                                        onValueChange = { durationMinutes = it },
                                        label = { Text("Duration (Mins)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Questions: ${currentQuestions.size}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text("Total Marks: ${currentQuestions.sumOf { it.marks }}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Question Flow Review", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Button(
                                onClick = { showQuestionPicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Question")
                            }
                        }
                    }
                    
                    if (currentQuestions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("This paper has no questions yet. Use \"Add Question\" to insert.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        itemsIndexed(currentQuestions) { index, q ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(q.type.uppercase()) }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(q.difficulty.uppercase()) }
                                            )
                                        }
                                        Text("[${q.marks} Marks]", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Q${index + 1}. ${q.question}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    
                                    if (q.type == "mcq") {
                                        val options = remember(q.optionsJson) {
                                            try {
                                                val arr = org.json.JSONArray(q.optionsJson)
                                                List(arr.length()) { arr.getString(it) }
                                            } catch (e: Exception) {
                                                emptyList<String>()
                                            }
                                        }
                                        if (options.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            options.forEachIndexed { optIdx, opt ->
                                                Text("(${('A' + optIdx)}) $opt", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp))
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Answer: ${q.answer}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    if (q.explanation.isNotBlank()) {
                                        Text("Explanation: ${q.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                val replacements = allQuestions.filter {
                                                    it.id != q.id &&
                                                    it.type == q.type &&
                                                    it.bookTitle == q.bookTitle &&
                                                    !currentQuestions.any { cq -> cq.id == it.id }
                                                }
                                                if (replacements.isNotEmpty()) {
                                                    val rep = replacements.random()
                                                    currentQuestions = currentQuestions.toMutableList().apply {
                                                        set(index, rep)
                                                    }
                                                    Toast.makeText(context, "Auto-swapped with another question from pool", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "No alternative question found in pool", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Auto Swap")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(
                                            onClick = { swapTargetIndex = index }
                                        ) {
                                            Icon(Icons.Default.FindReplace, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Manual Swap")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                currentQuestions = currentQuestions.toMutableList().apply {
                                                    removeAt(index)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
    
    // Picker/Swap Question Picker Dialog
    if (swapTargetIndex != null || showQuestionPicker) {
        val isSwapping = swapTargetIndex != null
        val targetType = if (isSwapping) currentQuestions[swapTargetIndex!!].type else null
        
        var filterType by remember { mutableStateOf(targetType ?: "") }
        var filterDifficulty by remember { mutableStateOf("") }
        var searchQuery by remember { mutableStateOf("") }
        
        val filteredQuestions = remember(allQuestions, filterType, filterDifficulty, searchQuery, currentQuestions) {
            allQuestions.filter { q ->
                // Do not show questions already in paper unless we are swapping the current one itself
                val isAlreadyInPaper = currentQuestions.any { it.id == q.id }
                val isSelf = isSwapping && currentQuestions[swapTargetIndex!!].id == q.id
                
                (!isAlreadyInPaper || isSelf) &&
                (filterType.isEmpty() || q.type == filterType) &&
                (filterDifficulty.isEmpty() || q.difficulty == filterDifficulty) &&
                (searchQuery.isEmpty() || q.question.contains(searchQuery, ignoreCase = true) || q.bookTitle.contains(searchQuery, ignoreCase = true))
            }
        }
        
        AlertDialog(
            onDismissRequest = {
                swapTargetIndex = null
                showQuestionPicker = false
            },
            title = { Text(if (isSwapping) "Swap with Alternative Question" else "Add Question from Pool") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                    // Search & Filters Row
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by question text or book...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Type dropdown filter (only editable if not swapping, since swapping requires matching type)
                        var typeExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { if (!isSwapping) typeExp = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (filterType.isEmpty()) "All Types" else filterType.uppercase())
                            }
                            DropdownMenu(expanded = typeExp, onDismissRequest = { typeExp = false }) {
                                DropdownMenuItem(text = { Text("All Types") }, onClick = { filterType = ""; typeExp = false })
                                DropdownMenuItem(text = { Text("MCQ") }, onClick = { filterType = "mcq"; typeExp = false })
                                DropdownMenuItem(text = { Text("TF") }, onClick = { filterType = "tf"; typeExp = false })
                                DropdownMenuItem(text = { Text("FIB") }, onClick = { filterType = "fib"; typeExp = false })
                                DropdownMenuItem(text = { Text("Subjective") }, onClick = { filterType = "subjective"; typeExp = false })
                            }
                        }
                        
                        var diffExp by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { diffExp = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (filterDifficulty.isEmpty()) "All Diff." else filterDifficulty.uppercase())
                            }
                            DropdownMenu(expanded = diffExp, onDismissRequest = { diffExp = false }) {
                                DropdownMenuItem(text = { Text("All Difficulties") }, onClick = { filterDifficulty = ""; diffExp = false })
                                DropdownMenuItem(text = { Text("Easy") }, onClick = { filterDifficulty = "easy"; diffExp = false })
                                DropdownMenuItem(text = { Text("Medium") }, onClick = { filterDifficulty = "medium"; diffExp = false })
                                DropdownMenuItem(text = { Text("Hard") }, onClick = { filterDifficulty = "hard"; diffExp = false })
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (filteredQuestions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No questions match current filters.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredQuestions) { q ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSwapping) {
                                                currentQuestions = currentQuestions.toMutableList().apply {
                                                    set(swapTargetIndex!!, q)
                                                }
                                                swapTargetIndex = null
                                            } else {
                                                currentQuestions = currentQuestions.toMutableList().apply {
                                                    add(q)
                                                }
                                                showQuestionPicker = false
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(q.type.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("${q.marks} Marks", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(q.question, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Book: ${q.bookTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        swapTargetIndex = null
                        showQuestionPicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionsEvaluationTabContent(padding: PaddingValues, viewModel: OtsViewModel, settingsManager: SettingsManager) {
    val context = LocalContext.current
    val submissions by viewModel.testSubmissions.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedSubmissionForReview by remember { mutableStateOf<com.example.data.model.TestSubmissionEntity?>(null) }

    val filteredSubmissions = remember(submissions, searchQuery, selectedFilter) {
        submissions.filter { sub ->
            val matchesSearch = searchQuery.isBlank() || 
                sub.candidateName.contains(searchQuery, ignoreCase = true) ||
                sub.candidateRollNumber.contains(searchQuery, ignoreCase = true) ||
                sub.paperTitle.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedFilter) {
                "In-Progress" -> sub.status == "In-Progress"
                "Submitted" -> sub.status == "Submitted"
                "Declared" -> sub.isResultDeclared
                "Disputed" -> sub.disputeStatus != "None"
                "Disqualified" -> sub.status == "Disqualified"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Action Buttons Row: Declare All, Merit Gazette PDF, SMS All
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.declareAllResults { count ->
                                Toast.makeText(context, "Declared results for $count candidates!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Declare All", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.generateSubmissionsMeritGazette(context)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Merit PDF", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.dispatchAllDeclaredResultsSms(context)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS All", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, roll, or paper...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Submitted", "Declared", "Disputed", "Disqualified", "In-Progress").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (filteredSubmissions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (submissions.isEmpty()) "No candidate submissions recorded in database yet." else "No submissions match search/filter.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSubmissions, key = { it.id }) { sub ->
                    SubmissionCard(
                        sub = sub,
                        onReview = { selectedSubmissionForReview = sub },
                        onMarksheetPdf = { viewModel.generateCandidateMarksheetPdf(context, sub) },
                        onSendSms = { viewModel.dispatchSubmissionResultSms(context, sub) },
                        onDelete = { viewModel.deleteSubmission(sub.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (selectedSubmissionForReview != null) {
        SubmissionConflictResolutionDialog(
            submission = selectedSubmissionForReview!!,
            viewModel = viewModel,
            settingsManager = settingsManager,
            onDismiss = { selectedSubmissionForReview = null }
        )
    }
}

@Composable
fun SubmissionCard(
    sub: com.example.data.model.TestSubmissionEntity,
    onReview: () -> Unit,
    onMarksheetPdf: () -> Unit,
    onSendSms: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Candidate photo if available
                if (sub.portraitBase64.isNotBlank()) {
                    val bitmap = remember(sub.portraitBase64) {
                        try {
                            val cleanBase64 = if (sub.portraitBase64.contains(",")) sub.portraitBase64.substringAfter(",") else sub.portraitBase64
                            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        } catch (e: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Candidate Portrait",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        )
                    } else {
                        DefaultAvatarBox()
                    }
                } else {
                    DefaultAvatarBox()
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sub.candidateName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (sub.rank > 0 && sub.isResultDeclared) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color(0xFF7C3AED), shape = RoundedCornerShape(10.dp)) {
                                Text("#${sub.rank}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("Roll: ${sub.candidateRollNumber} | ${sub.paperTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val dateText = remember(sub.submitTime) {
                        if (sub.submitTime > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(sub.submitTime)) else "Ongoing"
                    }
                    Text("Submitted: $dateText", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }

                Column(horizontalAlignment = Alignment.End) {
                    val pct = if (sub.maxMarks > 0) (sub.score * 100) / sub.maxMarks else 0
                    Text("${sub.score} / ${sub.maxMarks}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = if (pct >= 35) Color(0xFF16A34A) else Color(0xFFDC2626))
                    Text("$pct%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges Row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Status badge
                val statusColor = when (sub.status) {
                    "Submitted" -> Color(0xFF16A34A)
                    "Disqualified" -> Color(0xFFDC2626)
                    else -> Color(0xFFD97706)
                }
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text(sub.status, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }

                // Declared badge
                if (sub.isResultDeclared) {
                    Surface(color = Color(0xFF2563EB).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text("Declared", color = Color(0xFF2563EB), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                } else if (sub.status != "In-Progress") {
                    Surface(color = Color(0xFFCA8A04).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text("Under Eval", color = Color(0xFFCA8A04), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                // Dispute badge
                if (sub.disputeStatus != "None") {
                    Surface(color = Color(0xFF9333EA).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text(sub.disputeStatus, color = Color(0xFF9333EA), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                // Warning badge
                if (sub.warningCount > 0) {
                    Surface(color = Color(0xFFDC2626).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text("⚠️ ${sub.warningCount} warn", color = Color(0xFFDC2626), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                // Digital signature stamp badge
                if (sub.evaluatedBy.isNotBlank()) {
                    Surface(color = Color(0xFF0284C7).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text("Signed: ${sub.evaluatedBy}", color = Color(0xFF0284C7), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onReview,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Review Paper", style = MaterialTheme.typography.labelSmall)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onMarksheetPdf) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Marksheet", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (sub.candidateMobile.isNotBlank()) {
                        IconButton(onClick = onSendSms) {
                            Icon(Icons.Default.Send, contentDescription = "Send SMS", tint = Color(0xFF047857))
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultAvatarBox() {
    Box(
        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionConflictResolutionDialog(
    submission: com.example.data.model.TestSubmissionEntity,
    viewModel: OtsViewModel,
    settingsManager: SettingsManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var adjustedScoreText by remember { mutableStateOf(submission.score.toString()) }
    var proctorRemarks by remember { mutableStateOf(submission.proctorRemarks) }
    var disputeStatus by remember { mutableStateOf(submission.disputeStatus) }

    val questions: List<com.example.util.QuestionDto> = remember(submission.questionsJson) {
        try {
            kotlinx.serialization.json.Json.decodeFromString(submission.questionsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val answersMap: Map<String, String> = remember(submission.answersJson) {
        try {
            val json = org.json.JSONObject(submission.answersJson)
            val map = mutableMapOf<String, String>()
            json.keys().forEach { k -> map[k] = json.optString(k) }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    val violationsList: List<String> = remember(submission.violationsJson) {
        try {
            val arr = org.json.JSONArray(submission.violationsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.optString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Examination Audit & Evaluation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Candidate: ${submission.candidateName} (Roll: ${submission.candidateRollNumber})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Supervisor Profile & Digital Signature Certification Badge
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Digitally Signed by Supervisor Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = Color(0xFF0369A1))
                                Text("${settingsManager.activeSupervisorName} (${settingsManager.activeSupervisorRole})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF0C4A6E))
                                Text("${settingsManager.activeSupervisorInstitution} • ${settingsManager.activeSupervisorEmail}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                // Violation History Section
                if (violationsList.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Security Incidents Log (${violationsList.size})", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                violationsList.forEach { v ->
                                    Text("• $v", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7F1D1D))
                                }
                            }
                        }
                    }
                }

                // Question by Question Audit
                item {
                    Text("Question-by-Question Submission:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }

                itemsIndexed(questions) { idx, q ->
                    val studentAns = answersMap[q.id]?.trim() ?: ""
                    val isCorrect = studentAns.isNotEmpty() && studentAns.equals(q.answer.trim(), ignoreCase = true)
                    val cardBorder = if (studentAns.isEmpty()) Color.LightGray else if (isCorrect) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                    val cardBg = if (studentAns.isEmpty()) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else if (isCorrect) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Q${idx + 1}. [${q.type.uppercase()}]", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("${q.marks} Marks", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(q.question, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Candidate: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = studentAns.ifEmpty { "(No Answer / Skipped)" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (studentAns.isEmpty()) Color.Gray else if (isCorrect) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Official Key: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(q.answer, style = MaterialTheme.typography.bodySmall, color = Color(0xFF2563EB))
                            }
                        }
                    }
                }

                // Re-Evaluation / Dispute Resolution Section
                item {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Examiner Re-evaluation & Dispute Resolution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Adjusted Score
                    OutlinedTextField(
                        value = adjustedScoreText,
                        onValueChange = { adjustedScoreText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Final Score (Max: ${submission.maxMarks})") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dispute Status Chips
                    Text("Resolution Verdict / Dispute Status:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("None", "Under Review", "Resolved", "Pardoned", "Disqualified").forEach { status ->
                            FilterChip(
                                selected = disputeStatus == status,
                                onClick = { disputeStatus = status },
                                label = { Text(status) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Proctor / Examiner Remarks
                    OutlinedTextField(
                        value = proctorRemarks,
                        onValueChange = { proctorRemarks = it },
                        label = { Text("Examiner / Proctor Notes") },
                        placeholder = { Text("e.g. Grace marks awarded for contested question; violation investigated.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newScore = adjustedScoreText.toIntOrNull() ?: submission.score
                    viewModel.resolveDispute(submission.id, newScore, proctorRemarks, disputeStatus)
                    Toast.makeText(context, "Evaluation & resolution saved to database!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
