import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

target = """    val requiresGoogleLogin = !settingsManager.isGoogleSignedIn || settingsManager.googleAccountEmail.isBlank()
    val isAppLockedFromVm by viewModel.isAppLocked.collectAsState()
    val isAppLocked = isAppLockedFromVm ?: (requiresGoogleLogin || settingsManager.isAppLockEnabled)"""

replacement = """    val requiresGoogleLogin = !settingsManager.isGoogleSignedIn || settingsManager.googleAccountEmail.isBlank()
    val isAppLockedFromVm by viewModel.isAppLocked.collectAsState()
    val isAppLocked = isAppLockedFromVm ?: (requiresGoogleLogin || settingsManager.isAppLockEnabled)

    // Automatic Google Drive Sync
    LaunchedEffect(settingsManager.isGoogleDriveSyncEnabled, settingsManager.autoSyncIntervalMins) {
        if (settingsManager.isGoogleDriveSyncEnabled) {
            while (true) {
                kotlinx.coroutines.delay(settingsManager.autoSyncIntervalMins * 60 * 1000L)
                com.example.util.GoogleDriveSyncManager.backupToDrive(
                    context = context,
                    viewModel = viewModel,
                    settingsManager = settingsManager,
                    onComplete = { success, msg -> 
                        android.util.Log.d("AutoSync", "Auto sync complete: $success - $msg")
                    }
                )
            }
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
        f.write(content)
    print("Auto sync patched")
else:
    print("Target not found")
