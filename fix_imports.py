import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    code = f.read()

imports = """import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
"""

code = code.replace("import androidx.compose.ui.unit.dp\n", "import androidx.compose.ui.unit.dp\n" + imports)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(code)

