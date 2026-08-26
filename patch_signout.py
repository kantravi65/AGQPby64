import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

target = """                            onGoogleSignOut = {
                                viewModel.setAppLocked(true)
                                currentScreen = "home"
                            }"""

replacement = """                            onGoogleSignOut = {
                                scope.launch {
                                    com.example.util.GoogleDriveSyncManager.signOutGoogle(context, settingsManager)
                                    com.example.util.FirebaseAuthHelper.signOut()
                                    viewModel.setAppLocked(true)
                                    currentScreen = "home"
                                }
                            }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
        f.write(content)
    print("Sign out patched")
else:
    print("Target not found")
