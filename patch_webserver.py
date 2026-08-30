import re

with open('app/src/main/java/com/example/util/WebServerManager.kt', 'r') as f:
    text = f.read()

old_resp = "data class LoginResponse(val success: Boolean, val questions: List<QuestionDto>, val durationMinutes: Int)"
new_resp = "data class LoginResponse(val success: Boolean, val questions: List<QuestionDto>, val durationMinutes: Int, val examName: String = \"\", val subjectName: String = \"\")"

text = text.replace(old_resp, new_resp)

old_call = "call.respond(LoginResponse(true, questionDtos, LiveTestState.config.durationMinutes))"
new_call = "call.respond(LoginResponse(true, questionDtos, LiveTestState.config.durationMinutes, LiveTestState.config.examName, LiveTestState.config.subject))"

text = text.replace(old_call, new_call)

with open('app/src/main/java/com/example/util/WebServerManager.kt', 'w') as f:
    f.write(text)

