with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("private val _selectedDifficultyFilter", "private val _selectedTypeFilter = MutableStateFlow<String?>(null)\n    val selectedTypeFilter: StateFlow<String?> = _selectedTypeFilter.asStateFlow()\n\n    private val _selectedDifficultyFilter")

content = content.replace("fun setDifficultyFilter", "fun setTypeFilter(type: String?) {\n        _selectedTypeFilter.value = type\n    }\n\n    fun setDifficultyFilter")

with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'w') as f:
    f.write(content)
