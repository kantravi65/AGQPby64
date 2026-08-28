import re

with open("app/src/main/assets/web_dashboard.html", "r") as f:
    content = f.read()

# Replace Quiz UI
quiz_ui_pattern = r'<div class="toolbar" id="quizSetup">\s*<select id="quizSelectPaper"[^>]+><option value="">Select Paper to Start\.\.\.</option></select>\s*<button class="btn btn-success" onclick="startQuiz\(\)">Start Quiz</button>\s*</div>'
new_quiz_ui = '''<div id="quizSetup" style="display: flex; flex-direction: column; gap: 15px; margin-bottom: 20px;">
            <div class="toolbar" style="margin-bottom: 0;">
                <select id="quizSelectPaper" class="search-box" style="width:300px;"><option value="">Select Paper to Start...</option></select>
                <button class="btn btn-success" onclick="startQuiz('paper')">Start from Paper</button>
            </div>
            <div style="text-align: center; font-weight: bold; color: #666;">- OR -</div>
            <div class="toolbar" style="margin-bottom: 0;">
                <select id="quizSelectSubject" class="search-box" style="width:200px;"><option value="">All Subjects</option></select>
                <input type="number" id="quizQuestionCount" class="search-box" style="width:100px;" placeholder="No. of Qs">
                <button class="btn btn-primary" onclick="startQuiz('subject')">Start from Subject</button>
            </div>
        </div>'''
content = re.sub(quiz_ui_pattern, new_quiz_ui, content, count=1)


# Replace startQuiz() function
start_quiz_pattern = r'function startQuiz\(\) \{[\s\S]*?currentQuizQs\.forEach\(\(q, idx\) => \{'
new_start_quiz = '''function startQuiz(mode) {
        currentQuizQs = [];
        let title = "Quiz";
        
        if (mode === 'paper') {
            const pid = document.getElementById('quizSelectPaper').value;
            if(!pid) return alert("Select a paper first");
            const p = globalPapers.find(x => x.id === pid);
            if(!p) return;
            
            let qIds = [];
            try { qIds = JSON.parse(p.questionIdsJson); } catch(e){}
            currentQuizQs = globalQuestions.filter(q => qIds.includes(q.id));
            if(currentQuizQs.length === 0) return alert("This paper has no questions!");
            title = p.title;
        } else if (mode === 'subject') {
            const subj = document.getElementById('quizSelectSubject').value;
            const countStr = document.getElementById('quizQuestionCount').value;
            const count = parseInt(countStr) || 10;
            
            let pool = globalQuestions;
            if(subj) pool = pool.filter(q => q.bookTitle === subj);
            if (pool.length === 0) return alert("No questions available for this subject!");
            
            let shuffled = [...pool].sort(() => 0.5 - Math.random());
            currentQuizQs = shuffled.slice(0, count);
            title = subj ? `${subj} Quiz` : `Random Quiz`;
        }
        
        document.getElementById('quizSetup').style.display = 'none';
        document.getElementById('quizContainer').style.display = 'block';
        document.getElementById('quizTitle').innerText = title;
        
        const qDiv = document.getElementById('quizQuestions');
        qDiv.innerHTML = '';
        currentQuizQs.forEach((q, idx) => {'''
content = re.sub(start_quiz_pattern, new_start_quiz, content, count=1)


# Replace populateFilters to also update quizSelectSubject
populate_filters_pattern = r'subjSel\.value = curSubj; ansSel\.value = curAns; markSel\.value = curMark;\s*\}'
new_populate_filters = '''subjSel.value = curSubj; ansSel.value = curAns; markSel.value = curMark;
        
        const quizSubj = document.getElementById('quizSelectSubject');
        if (quizSubj) {
            const curQuizSubj = quizSubj.value;
            quizSubj.innerHTML = '<option value="">All Subjects</option>' + subjs.map(s => `<option value="${s.replace(/"/g, '&quot;')}">${s}</option>`).join('');
            quizSubj.value = curQuizSubj;
        }
    }'''
content = re.sub(populate_filters_pattern, new_populate_filters, content, count=1)

with open("app/src/main/assets/web_dashboard.html", "w") as f:
    f.write(content)

