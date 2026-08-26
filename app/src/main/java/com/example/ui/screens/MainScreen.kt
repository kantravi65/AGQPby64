package com.example.ui.screens

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.OtsViewModel
import kotlinx.coroutines.launch
import com.example.util.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: OtsViewModel) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    val scope = rememberCoroutineScope()

    // Screen states: "home", "quiz", "manage_bank", "build_bank", "assemble_paper", "saved_papers", "recycle_bin", "settings"
    var currentScreen by remember { mutableStateOf("home") }

    // Practice mode full screen check
    val isPracticeMode by viewModel.isPracticeMode.collectAsState()
    val isFullScreenQuiz = currentScreen == "quiz" || isPracticeMode

    LaunchedEffect(isPracticeMode) {
        if (!isPracticeMode && currentScreen == "quiz") {
            currentScreen = "home"
        }
    }

    // App Lock & Google Gate State
    val requiresGoogleLogin = !settingsManager.isGoogleSignedIn || settingsManager.googleAccountEmail.isBlank()
    val isAppLockedFromVm by viewModel.isAppLocked.collectAsState()
    val isAppLocked = isAppLockedFromVm ?: (requiresGoogleLogin || settingsManager.isAppLockEnabled)

    LaunchedEffect(settingsManager.isGoogleSignedIn, settingsManager.googleAccountEmail) {
        if (!settingsManager.isGoogleSignedIn || settingsManager.googleAccountEmail.isBlank()) {
            viewModel.setAppLocked(true)
        }
    }

    DisposableEffect(settingsManager.isGoogleSignedIn, settingsManager.googleAccountEmail) {
        val email = settingsManager.googleAccountEmail
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        if (settingsManager.isGoogleSignedIn && email.isNotBlank()) {
            if (!com.example.util.WhitelistManager.isOwner(email)) {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                registration = db.collection("whitelisted_users")
                    .document(email)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            error.printStackTrace()
                            return@addSnapshotListener
                        }
                        if (snapshot != null && !snapshot.exists()) {
                            scope.launch {
                                com.example.util.GoogleDriveSyncManager.signOutGoogle(context, settingsManager)
                                com.example.util.FirebaseAuthHelper.signOut()
                                viewModel.setAppLocked(true)
                                android.widget.Toast.makeText(
                                    context,
                                    "Your access has been revoked by the owner.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
            }
        }
        onDispose {
            registration?.remove()
        }
    }

    if (isAppLocked) {
        LockScreen(
            settingsManager = settingsManager,
            onUnlockSuccess = { viewModel.setAppLocked(false) }
        )
    } else {
        Scaffold(
            topBar = {
                if (!isFullScreenQuiz) {
                    TopAppBar(
                        title = {
                            if (currentScreen == "home") {
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = "OTSby64",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "developed by Ravikant",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                }
                            } else {
                                Text(
                                    text = when (currentScreen) {
                                        "manage_bank" -> "Question Bank"
                                        "build_bank" -> "Build Bank"
                                        "assemble_paper" -> "Paper Builder"
                                        "saved_papers" -> "Saved Papers"
                                        "recycle_bin" -> "Recycle Bin"
                                        "settings" -> "Settings"
                                        else -> "Question Bank"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        navigationIcon = {
                            if (currentScreen != "home") {
                                IconButton(onClick = { currentScreen = "home" }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Home"
                                    )
                                }
                            } else {
                                IconButton(onClick = { }) {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = "Home Dashboard"
                                    )
                                }
                            }
                        },
                        actions = {
                            var isSyncing by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    isSyncing = true
                                    viewModel.createSnapshotPoint(settingsManager)
                                    viewModel.triggerGlobalSync(context, settingsManager) { finalMessage ->
                                        isSyncing = false
                                        android.widget.Toast.makeText(context, "Global Sync Completed:\n$finalMessage", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = !isSyncing,
                                modifier = Modifier.testTag("global_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Global Sync",
                                    tint = if (isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            IconButton(
                                onClick = { currentScreen = "settings" },
                                modifier = Modifier.testTag("nav_settings_button")
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            },
            bottomBar = {
                if (!isFullScreenQuiz) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == "home",
                            onClick = { currentScreen = "home" },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = currentScreen == "quiz",
                            onClick = {
                                viewModel.startPracticeMode()
                                currentScreen = "quiz"
                            },
                            icon = { Icon(Icons.Default.Quiz, contentDescription = "Quiz") },
                            label = { Text("Quiz") },
                            modifier = Modifier.testTag("nav_quiz")
                        )
                        NavigationBarItem(
                            selected = currentScreen == "manage_bank" || currentScreen == "build_bank",
                            onClick = { currentScreen = "manage_bank" },
                            icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Bank") },
                            label = { Text("Bank") },
                            modifier = Modifier.testTag("nav_bank")
                        )
                        NavigationBarItem(
                            selected = currentScreen == "assemble_paper" || currentScreen == "saved_papers",
                            onClick = { currentScreen = "assemble_paper" },
                            icon = { Icon(Icons.Default.Description, contentDescription = "Papers") },
                            label = { Text("Papers") },
                            modifier = Modifier.testTag("nav_papers")
                        )
                        NavigationBarItem(
                            selected = currentScreen == "settings",
                            onClick = { currentScreen = "settings" },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullScreenQuiz) PaddingValues(0.dp) else innerPadding)
            ) {
                if (isFullScreenQuiz) {
                    androidx.activity.compose.BackHandler {
                        viewModel.stopPracticeMode()
                        currentScreen = "home"
                    }
                    LaunchedEffect(Unit) {
                        if (!isPracticeMode) {
                            viewModel.startPracticeMode()
                        }
                    }
                    QuestionBankScreen(
                        viewModel = viewModel,
                        initialMode = "quiz"
                    )
                } else {
                    when (currentScreen) {
                        "home" -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToTile = { tileId ->
                                if (tileId == "quiz") {
                                    viewModel.startPracticeMode()
                                }
                                currentScreen = tileId
                            }
                        )
                        "manage_bank" -> QuestionBankScreen(viewModel = viewModel, initialMode = "manage")
                        "build_bank" -> QuestionBankScreen(viewModel = viewModel, initialMode = "add")
                        "assemble_paper" -> AssemblePaperScreen(viewModel = viewModel, initialTab = 0)
                        "saved_papers" -> AssemblePaperScreen(viewModel = viewModel, initialTab = 1)
                        "recycle_bin" -> QuestionBankScreen(viewModel = viewModel, initialMode = "backup")
                        "settings" -> SettingsScreen(
                            viewModel = viewModel,
                            settingsManager = settingsManager,
                            onLockAppNow = { viewModel.setAppLocked(true) },
                            onGoogleSignOut = {
                                viewModel.setAppLocked(true)
                                currentScreen = "home"
                            }
                        )
                        else -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToTile = { currentScreen = it }
                        )
                    }
                }
            }
        }
    }
}

