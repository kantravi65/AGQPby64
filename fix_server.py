import re

with open("app/src/main/java/com/example/util/WebServerManager.kt", "r") as f:
    content = f.read()

# Fix getPaper and getAllQuestions
bad_paper = 'val paper = id?.let { repository.getPaper(it) }'
good_paper = 'val paper = id?.let { repository.allPapers.first().find { p -> p.id == it } }'
content = content.replace(bad_paper, good_paper)

bad_qs = 'val questions = repository.getAllQuestions().first().filter { qIds.contains(it.id) }'
good_qs = 'val questions = repository.allQuestions.first().filter { qIds.contains(it.id) }'
content = content.replace(bad_qs, good_qs)

with open("app/src/main/java/com/example/util/WebServerManager.kt", "w") as f:
    f.write(content)
