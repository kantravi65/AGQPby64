import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# I will check if it already has auto-sync
print("autoSyncIntervalMins" in content)
