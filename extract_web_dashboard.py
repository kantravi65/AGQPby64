import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    settings = f.read()

# Find the start
start_marker = "        // --- WEB DASHBOARD MANAGER ---"
end_marker = "        // --- 10. ABOUT DEVELOPER ---"

start_idx = settings.find(start_marker)
end_idx = settings.find(end_marker)

if start_idx != -1 and end_idx != -1:
    web_dashboard_code = settings[start_idx:end_idx]
    
    # Remove from SettingsScreen
    new_settings = settings[:start_idx] + settings[end_idx:]
    with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
        f.write(new_settings)
        
    with open('web_dashboard_extracted.txt', 'w') as f:
        f.write(web_dashboard_code)
    
    print("Extraction successful.")
else:
    print("Could not find markers.")

