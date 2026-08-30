import re

with open('app/src/main/java/com/example/ui/screens/ArchivesScreen.kt', 'r') as f:
    text = f.read()

imports = """import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
"""

text = text.replace("import com.example.ui.viewmodel.OtsViewModel", imports + "import com.example.ui.viewmodel.OtsViewModel")

with open('app/src/main/java/com/example/ui/screens/ArchivesScreen.kt', 'w') as f:
    f.write(text)
