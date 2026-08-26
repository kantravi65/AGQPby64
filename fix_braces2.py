import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Replace any sequence of 3 closing braces followed by a section comment
content = re.sub(
    r'\}\s*\}\s*\}\s*(?=(// --- (3|3\.5|4|5|6|7|8|9|10)\.))',
    r'    }\n        }\n        ',
    content
)

# And for section 10, the end is followed by if (showChangePinDialog)
# Wait, section 10 ends with:
#                 }
#             }
#         }
#     }
# 
#     if (showChangePinDialog) {

content = re.sub(
    r'\}\s*\}\s*\}\s*\}\s*(?=(if \(showChangePinDialog\)))',
    r'    }\n        }\n    }\n\n    ',
    content
)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)

