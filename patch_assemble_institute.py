import re

with open("app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt", "r") as f:
    content = f.read()

old_vars = """    var mainTitle by remember { mutableStateOf(paper.title.ifBlank { "GEN TEST: FLT ENG" }) }
    var subTitle by remember { mutableStateOf("TECH II") }"""

new_vars = """    val settingsManager = remember { com.example.util.SettingsManager(context) }
    var mainTitle by remember { mutableStateOf(settingsManager.defaultInstitute.ifBlank { "GEN TEST: FLT ENG" }) }
    var subTitle by remember { mutableStateOf(paper.title.ifBlank { "TECH II" }) }"""

content = content.replace(old_vars, new_vars)

old_sm = """    var showExplanations by remember { mutableStateOf(false) }

    val settingsManager = remember { com.example.util.SettingsManager(context) }
    var watermarkEnabled"""

new_sm = """    var showExplanations by remember { mutableStateOf(false) }

    var watermarkEnabled"""

content = content.replace(old_sm, new_sm)

with open("app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt", "w") as f:
    f.write(content)

