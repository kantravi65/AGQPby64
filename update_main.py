import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    home_code = f.read()

home_code = home_code.replace('import androidx.compose.material.icons.filled.*', 'import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.outlined.Archive')

new_tile = """        HomeTileItem(
            id = "archives",
            title = "Exam Archives",
            description = "Post-exam marksheet & result storage",
            icon = Icons.Outlined.Archive,
            countBadge = "Results",
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),"""

home_code = home_code.replace('        HomeTileItem(\n            id = "settings",', new_tile + '\n        HomeTileItem(\n            id = "settings",')

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(home_code)


with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    main_code = f.read()

main_code = main_code.replace('                                        "settings" -> "Settings"', '                                        "settings" -> "Settings"\n                                        "archives" -> "Exam Archives"')

main_code = main_code.replace('                    "settings" -> SettingsScreen(viewModel, settingsManager, context = context)', '                    "settings" -> SettingsScreen(viewModel, settingsManager, context = context)\n                    "archives" -> ArchivesScreen()')

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(main_code)
