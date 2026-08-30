import re

with open('app/src/main/assets/web_livetest.html', 'r') as f:
    text = f.read()

# 1. Update the UI upon login success
old_login = """        const data = await response.json();
        testQuestions = data.questions;
        timeLimitSeconds = data.durationMinutes * 60;"""
new_login = """        const data = await response.json();
        testQuestions = data.questions;
        timeLimitSeconds = data.durationMinutes * 60;
        
        document.getElementById('portalHeaderTitle').innerText = data.examName || 'Examination Portal';
        document.getElementById('portalHeaderSub').innerText = (data.subjectName && data.subjectName !== "") ? 'Subject: ' + data.subjectName : 'High Security Environment';
"""

text = text.replace(old_login, new_login)

with open('app/src/main/assets/web_livetest.html', 'w') as f:
    f.write(text)

