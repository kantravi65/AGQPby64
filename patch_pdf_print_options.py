import re

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'r') as f:
    content = f.read()

target = """                val optionsList = mutableListOf<String>()
                try {
                    val arr = JSONArray(q.optionsJson)
                    for (i in 0 until arr.length()) optionsList.add(arr.getString(i))
                } catch (e: Exception) {}"""

content = content.replace(target, "")

with open('app/src/main/java/com/example/util/PdfPrintUtils.kt', 'w') as f:
    f.write(content)
