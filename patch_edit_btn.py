import re

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'r') as f:
    content = f.read()

target = """                                IconButton(onClick = { onToggleBookmark(currentQ) }) {
                                    Icon(
                                        if (currentQ.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (currentQ.isBookmarked) Color.Red else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }"""

replacement = """                                Row {
                                    IconButton(onClick = { onEdit(currentQ) }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Question",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    IconButton(onClick = { onToggleBookmark(currentQ) }) {
                                        Icon(
                                            if (currentQ.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (currentQ.isBookmarked) Color.Red else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }"""

content = content.replace(target, replacement)
with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'w') as f:
    f.write(content)
