import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Restore the 9 braces removed before sections
content = re.sub(
    r'    \}\n        \}\n        (?=(// --- (3|3\.5|4|5|6|7|8|9|10)\.))',
    r'                }\n            }\n        }\n\n        ',
    content
)

# Restore the 1 brace removed before section 10's end
content = re.sub(
    r'    \}\n        \}\n    \}\n\n    (?=(if \(showChangePinDialog\)))',
    r'                }\n            }\n        }\n    }\n\n    ',
    content
)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)

