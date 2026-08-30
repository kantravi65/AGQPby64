import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    ms = f.read()

old_block = """                        "settings" -> SettingsScreen("""
new_block = """                        "archives" -> ArchivesScreen(viewModel = viewModel, settingsManager = settingsManager)
                        "settings" -> SettingsScreen("""
ms = ms.replace(old_block, new_block)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(ms)

