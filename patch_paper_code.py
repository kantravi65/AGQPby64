import re

with open("app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt", "r") as f:
    content = f.read()

target = 'var paperCode by remember { mutableStateOf("QP-178566") }'
replacement = """    val initialPaperCode = remember {
        val startDate = java.time.LocalDate.of(2008, 12, 31)
        val currentDate = java.time.LocalDate.now()
        val period = java.time.Period.between(startDate, currentDate)
        val formattedDate = String.format("%02d%02d%02d", period.years, period.months, period.days)
        val randomNum = kotlin.random.Random.nextInt(10, 100)
        "RYQP-$formattedDate-$randomNum"
    }
    var paperCode by remember { mutableStateOf(initialPaperCode) }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/AssemblePaperScreen.kt", "w") as f:
        f.write(content)
    print("Patched paper code")
else:
    print("Target not found in AssemblePaperScreen")
