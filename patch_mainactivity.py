import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

import_insert = '''
import android.os.Build
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
'''

if "android.Manifest" not in content:
    content = content.replace('import android.os.Bundle', 'import android.os.Bundle\n' + import_insert)

request_insert = '''
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
'''

if "POST_NOTIFICATIONS" not in content:
    content = content.replace('enableEdgeToEdge()', 'enableEdgeToEdge()\n' + request_insert)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
