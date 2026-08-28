import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# I need to add state for selected mode and show a dialog/dropdown before starting.
# Or just replace the button with 3 buttons? "Start Admin Server", "Start Expert Server", "Start Live Test Server"?
# Yes! 3 buttons is much easier UI-wise.
