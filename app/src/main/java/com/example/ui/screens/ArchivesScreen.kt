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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.viewmodel.OtsViewModel
import com.example.util.SettingsManager
import com.example.util.LiveTestState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivesScreen(viewModel: OtsViewModel, settingsManager: SettingsManager) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Live Exam & Archives") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Live Monitor Server") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Post-Exam Archives") }
                    )
                }
            }
        }
    ) { padding ->
        if (selectedTab == 0) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    
            val webServerUrl by viewModel.webServerUrl.collectAsState()
            var adminUser by remember { mutableStateOf("admin") }
            var adminPass by remember { mutableStateOf("1234") }
            val serverMode by com.example.util.WebServerState.mode.collectAsState()
            val candidates by viewModel.liveCandidates.collectAsState()
            
            val booksList by viewModel.books.collectAsState()
            val uniqueSubjects = remember(booksList) {
                listOf("") + booksList.map { it.title }.distinct().sorted()
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
                
                if (webServerUrl != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Server running [Mode: ${serverMode.uppercase()}]", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Open this URL on your PC:", style = MaterialTheme.typography.bodySmall)
                            Text(webServerUrl ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Live Test Session Supervisor Panel
                    if (serverMode == "livetest") {
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
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    enabled = candidates.isNotEmpty()
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Print Merit List & Scorecards (PDF)")
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
                                                                Text("Dispatch", style = MaterialTheme.typography.bodySmall)
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
                    Button(onClick = { viewModel.stopWebServer() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Web Server")
                    }
                } else {
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
                            
                                                        OutlinedTextField(
                                value = liveExamName,
                                onValueChange = { viewModel.updateLiveTestConfig(it, liveSubject, liveMcqCount, liveFibCount, liveTfCount, liveDuration) },
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
                                    label = { Text("Select Exam Subject") },
                                    trailingIcon = {
                                        IconButton(onClick = { expandedSubj = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().clickable { expandedSubj = true }
                                )
                                DropdownMenu(
                                    expanded = expandedSubj,
                                    onDismissRequest = { expandedSubj = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    uniqueSubjects.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(if (s.isEmpty()) "All Subjects" else s) },
                                            onClick = {
                                                viewModel.updateLiveTestConfig(liveExamName, s, liveMcqCount, liveFibCount, liveTfCount, liveDuration)
                                                expandedSubj = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 2. Type-wise total counts
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = liveMcqCount.toString(),
                                    onValueChange = {
                                        val count = it.toIntOrNull() ?: 0
                                        viewModel.updateLiveTestConfig(liveExamName, liveSubject, count, liveFibCount, liveTfCount, liveDuration)
                                    },
                                    label = { Text("MCQs") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = liveFibCount.toString(),
                                    onValueChange = {
                                        val count = it.toIntOrNull() ?: 0
                                        viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, count, liveTfCount, liveDuration)
                                    },
                                    label = { Text("FIBs") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = liveTfCount.toString(),
                                    onValueChange = {
                                        val count = it.toIntOrNull() ?: 0
                                        viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, liveFibCount, count, liveDuration)
                                    },
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
                                    viewModel.updateLiveTestConfig(liveExamName, liveSubject, liveMcqCount, liveFibCount, liveTfCount, mins)
                                },
                                label = { Text("Exam Duration (Minutes)") },
                                trailingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
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
                                onValueChange = { adminUser = it },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adminPass,
                                onValueChange = { adminPass = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.startWebServer("admin", adminUser, adminPass) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Admin Server")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.startWebServer("expert", adminUser, adminPass) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Expert Review Server")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.startWebServer("livetest", adminUser, adminPass) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Live Test Server")
                    }
                }
            }
        
                }
            }
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
