import re

with open('web_dashboard_extracted.txt', 'r') as f:
    web_dashboard = f.read()

# Strip "        // --- WEB DASHBOARD MANAGER ---"
web_dashboard = web_dashboard.replace("        // --- WEB DASHBOARD MANAGER ---", "")
# Strip leading "        item {" and the final matching "        }"
if "item {" in web_dashboard:
    web_dashboard = web_dashboard[web_dashboard.find("item {") + 6 : web_dashboard.rfind("}")]

# We need to add adminUser and adminPass state variables, and pass them to startWebServer
web_dashboard = web_dashboard.replace(
    'val webServerUrl by viewModel.webServerUrl.collectAsState()',
    'val webServerUrl by viewModel.webServerUrl.collectAsState()\n            var adminUser by remember { mutableStateOf("admin") }\n            var adminPass by remember { mutableStateOf("1234") }'
)

web_dashboard = web_dashboard.replace(
    'Button(\n                        onClick = { viewModel.startWebServer("admin") }',
    'Button(\n                        onClick = { viewModel.startWebServer("admin", adminUser, adminPass) }'
)
web_dashboard = web_dashboard.replace(
    'Button(\n                        onClick = { viewModel.startWebServer("expert") }',
    'Button(\n                        onClick = { viewModel.startWebServer("expert", adminUser, adminPass) }'
)
web_dashboard = web_dashboard.replace(
    'Button(\n                        onClick = { viewModel.startWebServer("livetest") }',
    'Button(\n                        onClick = { viewModel.startWebServer("livetest", adminUser, adminPass) }'
)

# Add username and password fields to the UI before the Start Server buttons
credentials_ui = """
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
"""

web_dashboard = web_dashboard.replace(
    'Button(\n                        onClick = { viewModel.startWebServer("admin", adminUser, adminPass) }',
    credentials_ui + '\n                    Button(\n                        onClick = { viewModel.startWebServer("admin", adminUser, adminPass) }'
)


# Now write the full ArchivesScreen.kt
archives_kt = f"""package com.example.ui.screens

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
import com.example.ui.viewmodel.OtsViewModel
import com.example.util.SettingsManager
import com.example.util.LiveTestState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivesScreen(viewModel: OtsViewModel, settingsManager: SettingsManager) {{
    val context = LocalContext.current
    var selectedTab by remember {{ mutableStateOf(0) }}

    Scaffold(
        topBar = {{
            Column {{
                TopAppBar(
                    title = {{ Text("Live Exam & Archives") }},
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                )
                TabRow(selectedTabIndex = selectedTab) {{
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {{ selectedTab = 0 }},
                        text = {{ Text("Live Monitor Server") }}
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {{ selectedTab = 1 }},
                        text = {{ Text("Post-Exam Archives") }}
                    )
                }}
            }}
        }}
    ) {{ padding ->
        if (selectedTab == 0) {{
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {{
                item {{
                    {web_dashboard}
                }}
            }}
        }} else {{
            ArchivesTabContent(padding)
        }}
    }}
}}

@Composable
fun ArchivesTabContent(padding: PaddingValues) {{
    val context = LocalContext.current
    var pdfFiles by remember {{ mutableStateOf(emptyList<File>()) }}

    LaunchedEffect(Unit) {{
        val dir = File(context.filesDir, "ExamArchives")
        if (dir.exists()) {{
            pdfFiles = dir.listFiles {{ file -> file.name.endsWith(".pdf") }}?.toList()?.sortedByDescending {{ it.lastModified() }} ?: emptyList()
        }}
    }}
    
    if (pdfFiles.isEmpty()) {{
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {{
            Text("No archived exam reports found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }}
    }} else {{
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {{
            items(pdfFiles) {{ file ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {{
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {{
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {{
                            Text(file.nameWithoutExtension.replace("_", " "), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
                            Text("Archived: $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }}
                        Button(onClick = {{ com.example.util.PdfPrintUtils.printPdf(context, file, "Archived_Report") }}) {{
                            Text("View/Print")
                        }}
                    }}
                }}
            }}
        }}
    }}
}}
"""

with open('app/src/main/java/com/example/ui/screens/ArchivesScreen.kt', 'w') as f:
    f.write(archives_kt)

