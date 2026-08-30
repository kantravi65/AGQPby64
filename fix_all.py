import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Let's see how many times the warn_buttons block was inserted
warn_buttons = """                                                    if (session.status == "Testing") {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                            Button(
                                                                onClick = {
                                                                    LiveTestState.setWarning(session.rollNumber, "Please keep your eyes on the screen and do not talk.")
                                                                    Toast.makeText(context, "Text Warning Sent to Client Screen", Toast.LENGTH_SHORT).show()
                                                                },
                                                                modifier = Modifier.weight(1f).height(32.dp),
                                                                contentPadding = PaddingValues(0.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                            ) {
                                                                Text("Issue Warning", style = MaterialTheme.typography.bodySmall)
                                                            }
                                                        }
                                                    }"""

# Remove ALL instances of warn_buttons and the preceding Spacer
content = content.replace("Spacer(modifier = Modifier.height(12.dp))\n" + warn_buttons, "Spacer(modifier = Modifier.height(12.dp))")

# Now re-insert it ONLY inside the candidates.forEach loop (around line 1700)
# Let's find a unique anchor in the supervisor card.
# "val badgeBg = if (session.status == \"Testing\")"
# Wait, let's just insert it uniquely.
# I will use multi_edit_file or python replace with a specific count.

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
