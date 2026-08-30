import re

with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'r') as f:
    vm = f.read()

# Add _liveTestExamName stateflow
insert_state = """    private val _liveTestSubject = kotlinx.coroutines.flow.MutableStateFlow("")
    val liveTestSubject: kotlinx.coroutines.flow.StateFlow<String> = _liveTestSubject
    
    private val _liveTestExamName = kotlinx.coroutines.flow.MutableStateFlow("Online Secured Exam")
    val liveTestExamName: kotlinx.coroutines.flow.StateFlow<String> = _liveTestExamName"""

vm = vm.replace("""    private val _liveTestSubject = kotlinx.coroutines.flow.MutableStateFlow("")
    val liveTestSubject: kotlinx.coroutines.flow.StateFlow<String> = _liveTestSubject""", insert_state)

old_config = """    fun updateLiveTestConfig(subject: String, mcqs: Int, fibs: Int, tfs: Int, duration: Int) {
        _liveTestSubject.value = subject
        _liveTestMcqCount.value = mcqs
        _liveTestFibCount.value = fibs
        _liveTestTfCount.value = tfs
        _liveTestDuration.value = duration
        
        com.example.util.LiveTestState.config = com.example.util.LiveTestConfig(
            subject = subject,
            mcqCount = mcqs,
            fibCount = fibs,
            tfCount = tfs,
            durationMinutes = duration
        )
    }"""
new_config = """    fun updateLiveTestConfig(examName: String, subject: String, mcqs: Int, fibs: Int, tfs: Int, duration: Int) {
        _liveTestExamName.value = examName
        _liveTestSubject.value = subject
        _liveTestMcqCount.value = mcqs
        _liveTestFibCount.value = fibs
        _liveTestTfCount.value = tfs
        _liveTestDuration.value = duration
        
        com.example.util.LiveTestState.config = com.example.util.LiveTestConfig(
            examName = examName,
            subject = subject,
            mcqCount = mcqs,
            fibCount = fibs,
            tfCount = tfs,
            durationMinutes = duration
        )
    }
    
    fun generateMeritListPdf(context: android.content.Context) {
        val candidates = com.example.util.LiveTestState.candidates.value
        val config = com.example.util.LiveTestState.config
        val pdf = com.example.util.PdfPrintUtils.generateLiveTestMeritList(context, config.subject, config.durationMinutes, candidates)
        if (pdf != null) {
            android.widget.Toast.makeText(context, "Merit List Generated!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Failed to generate Merit List.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }"""

vm = vm.replace(old_config, new_config)

with open('app/src/main/java/com/example/ui/viewmodel/OtsViewModel.kt', 'w') as f:
    f.write(vm)

