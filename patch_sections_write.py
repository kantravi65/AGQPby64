import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Same replacements as before
sec2_target = """        // --- 2. APP LOCK & BIOMETRIC SECURITY ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "App Lock & Security Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()"""

sec2_replacement = """        // --- 2. APP LOCK & BIOMETRIC SECURITY ---
        item {
            SettingsCategory(
                title = "App Lock & Security Settings",
                icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {"""

sec3_target = """        // --- 3. GOOGLE DRIVE SYNC & BACKUP ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Google Drive Cloud Backup & Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()"""

sec3_replacement = """        // --- 3. GOOGLE DRIVE SYNC & BACKUP ---
        item {
            SettingsCategory(
                title = "Google Drive Cloud Backup & Sync",
                icon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {"""

sec35_target = """        // --- 3.5. OWNER WHITELIST MANAGEMENT ---
        if (com.example.util.WhitelistManager.isOwner(settingsManager.googleAccountEmail)) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Owner Whitelist Management",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider()"""

sec35_replacement = """        // --- 3.5. OWNER WHITELIST MANAGEMENT ---
        if (com.example.util.WhitelistManager.isOwner(settingsManager.googleAccountEmail)) {
            item {
                SettingsCategory(
                    title = "Owner Whitelist Management",
                    icon = { Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                ) {"""

sec4_target = """        // --- 4. EXAM & PRINT DEFAULTS ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Exam Paper & Watermark Defaults",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()"""

sec4_replacement = """        // --- 4. EXAM & PRINT DEFAULTS ---
        item {
            SettingsCategory(
                title = "Exam Paper & Watermark Defaults",
                icon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {"""

sec5_target = """        // --- 5. FTP REMOTE STORAGE INTEGRATION ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var isFtpConnectionValid by remember { mutableStateOf(settingsManager.isFtpConnectionValid) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "FTP Remote Storage Integration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (isFtpConnectionValid) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid Connection",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider()"""

sec5_replacement = """        // --- 5. FTP REMOTE STORAGE INTEGRATION ---
        item {
            var isFtpConnectionValid by remember { mutableStateOf(settingsManager.isFtpConnectionValid) }
            SettingsCategory(
                title = "FTP Remote Storage Integration",
                icon = { Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                titleRightContent = {
                    if (isFtpConnectionValid) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Valid Connection",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp).padding(end = 8.dp)
                        )
                    }
                }
            ) {"""

sec6_target = """        // --- 6. LOCAL DEVICE STORAGE EXPORT / IMPORT ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Local Device Export & Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()"""

sec6_replacement = """        // --- 6. LOCAL DEVICE STORAGE EXPORT / IMPORT ---
        item {
            SettingsCategory(
                title = "Local Device Export & Backup",
                icon = { Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {"""

sec7_target = """        // --- 7. DATA RECOVERY & TRASH / RECYCLE BIN ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Data Recovery & Undo Trash",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()"""

sec7_replacement = """        // --- 7. DATA RECOVERY & TRASH / RECYCLE BIN ---
        item {
            SettingsCategory(
                title = "Data Recovery & Undo Trash",
                icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {"""

sec8_target = """        // --- 8. CLEAN UNINSTALL & REINSTALL DATA PRESERVATION ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Clean Uninstall & Cloud Preservation Policy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()"""

sec8_replacement = """        // --- 8. CLEAN UNINSTALL & REINSTALL DATA PRESERVATION ---
        item {
            SettingsCategory(
                title = "Clean Uninstall & Cloud Preservation Policy",
                icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {"""

sec9_target = """        // --- 9. APP UPDATE ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "App Update",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()"""

sec9_replacement = """        // --- 9. APP UPDATE ---
        item {
            SettingsCategory(
                title = "App Update",
                icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            ) {"""

sec10_target = """        // --- 10. ABOUT DEVELOPER ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()"""

sec10_replacement = """        // --- 10. ABOUT DEVELOPER ---
        item {
            SettingsCategory(
                title = "About",
                icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {"""

replacements = [
    (sec2_target, sec2_replacement, "Section 2"),
    (sec3_target, sec3_replacement, "Section 3"),
    (sec35_target, sec35_replacement, "Section 3.5"),
    (sec4_target, sec4_replacement, "Section 4"),
    (sec5_target, sec5_replacement, "Section 5"),
    (sec6_target, sec6_replacement, "Section 6"),
    (sec7_target, sec7_replacement, "Section 7"),
    (sec8_target, sec8_replacement, "Section 8"),
    (sec9_target, sec9_replacement, "Section 9"),
    (sec10_target, sec10_replacement, "Section 10"),
]

for t, r, name in replacements:
    if t in content:
        content = content.replace(t, r)
        print(f"Replaced {name}")
    else:
        print(f"{name} target not found")

# NOW WE ACTUALLY WRITE THE FILE
with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)

