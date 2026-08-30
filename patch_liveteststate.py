import re

with open('app/src/main/java/com/example/util/LiveTestState.kt', 'r') as f:
    text = f.read()

old_config = """data class LiveTestConfig(
    val subject: String = "",
    val mcqCount: Int = 10,
    val fibCount: Int = 0,
    val tfCount: Int = 0,
    val durationMinutes: Int = 30
)"""

new_config = """data class LiveTestConfig(
    val examName: String = "Online Secured Exam",
    val subject: String = "",
    val mcqCount: Int = 10,
    val fibCount: Int = 0,
    val tfCount: Int = 0,
    val durationMinutes: Int = 30
)"""

text = text.replace(old_config, new_config)

with open('app/src/main/java/com/example/util/LiveTestState.kt', 'w') as f:
    f.write(text)

