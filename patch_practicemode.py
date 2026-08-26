import re

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'r') as f:
    content = f.read()

# Let's insert type filter in PracticeModeView
lazy_row_idx = content.find("            // Subject Filter Bar\n            LazyRow(")
if lazy_row_idx != -1:
    type_filter_str = """
            // Type Filter Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { onSelectTypeFilter(null) },
                        label = { Text("All Types") }
                    )
                }
                val typeMap = mapOf(
                    "mcq" to "MCQ",
                    "subjective" to "Subjective",
                    "tf" to "True / False",
                    "fib" to "Fill in Blanks"
                )
                items(typeMap.keys.toList()) { typeKey ->
                    val isSelected = selectedTypeFilter == typeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTypeFilter(if (isSelected) null else typeKey) },
                        label = { Text(typeMap[typeKey]!!) }
                    )
                }
            }
"""
    content = content[:lazy_row_idx] + type_filter_str + content[lazy_row_idx:]

with open('app/src/main/java/com/example/ui/screens/QuestionBankScreen.kt', 'w') as f:
    f.write(content)
