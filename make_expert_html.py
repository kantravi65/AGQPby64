import re

with open("app/src/main/assets/web_dashboard.html", "r") as f:
    content = f.read()

# I will replace the entire body inside <div class="container"> with just the Expert UI
start = content.find('<div class="container">')
end = content.find('</body>')

expert_body = """<div class="container" style="max-width: 1000px; margin: 40px auto; padding: 20px; background: white; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
    <h1 style="text-align: center; color: #1976d2;">Expert Review System</h1>
    
    <div style="background: #f8f9fa; padding: 15px; border-radius: 8px; margin-bottom: 20px; border: 1px solid #ddd;">
        <h3 style="margin-top:0;">1. Filter Questions for Review</h3>
        <div style="display: flex; gap: 15px; flex-wrap: wrap;">
            <div style="flex:1;"><label>Subject</label><select id="ex_subject" class="search-box" style="width:100%;"><option value="">All Subjects</option></select></div>
            <div style="flex:1;"><label>Filter by Answer</label><input type="text" id="ex_answer" class="search-box" style="width:100%;" placeholder="e.g. True, 42, etc."></div>
            <div style="flex:1;"><label>Number of Questions</label><input type="number" id="ex_count" class="search-box" style="width:100%;" value="10"></div>
            <div style="display:flex; align-items:flex-end;"><button class="btn btn-primary" onclick="loadQuestions()">Load Questions</button></div>
        </div>
    </div>

    <div id="expertQuestionsContainer"></div>
</div>

<script>
let allQuestions = [];
let loadedQuestions = [];

async function fetchData() {
    const r = await fetch('/api/questions');
    allQuestions = await r.json();
    
    const subjs = [...new Set(allQuestions.map(q => q.bookTitle))].sort();
    document.getElementById('ex_subject').innerHTML = '<option value="">All Subjects</option>' + subjs.map(s => `<option value="${s.replace(/"/g, '&quot;')}">${s}</option>`).join('');
}

function loadQuestions() {
    const subj = document.getElementById('ex_subject').value;
    const ans = document.getElementById('ex_answer').value.toLowerCase();
    const count = parseInt(document.getElementById('ex_count').value) || 10;
    
    let pool = allQuestions;
    if (subj) pool = pool.filter(q => q.bookTitle === subj);
    if (ans) pool = pool.filter(q => q.answer.toLowerCase().includes(ans));
    
    loadedQuestions = pool.sort(() => 0.5 - Math.random()).slice(0, count);
    renderQuestions();
}

function renderQuestions() {
    const container = document.getElementById('expertQuestionsContainer');
    container.innerHTML = '';
    
    if (loadedQuestions.length === 0) {
        container.innerHTML = '<p>No questions found matching criteria.</p>';
        return;
    }
    
    loadedQuestions.forEach((q, i) => {
        let opts = []; try{ opts = JSON.parse(q.optionsJson); }catch(e){}
        const div = document.createElement('div');
        div.style.cssText = 'background: #fff; padding: 15px; margin-bottom: 15px; border-radius: 6px; border: 1px solid #ccc; position: relative;';
        
        div.innerHTML = `
            <div style="font-weight:bold; color:#555; margin-bottom: 10px;">Q${i+1} [${q.bookTitle}] - Type: ${q.type.toUpperCase()}</div>
            <div style="display:flex; gap: 15px;">
                <div style="flex:2;">
                    <label>Question Text</label>
                    <textarea id="eq_${q.id}" class="search-box" style="width:100%;" rows="3">${q.question}</textarea>
                </div>
                <div style="flex:1;">
                    <label>Correct Answer</label>
                    <input type="text" id="ea_${q.id}" class="search-box" style="width:100%;" value="${q.answer.replace(/"/g, '&quot;')}">
                    <label style="margin-top:10px; display:block;">Marks</label>
                    <input type="number" id="em_${q.id}" class="search-box" style="width:100%;" value="${q.marks}">
                </div>
            </div>
            ${q.type === 'mcq' ? `
            <div style="margin-top:10px;">
                <label>Options</label>
                <div style="display:flex; gap:10px;">
                    <input type="text" id="eo1_${q.id}" class="search-box" style="width:25%;" value="${(opts[0]||'').replace(/"/g, '&quot;')}">
                    <input type="text" id="eo2_${q.id}" class="search-box" style="width:25%;" value="${(opts[1]||'').replace(/"/g, '&quot;')}">
                    <input type="text" id="eo3_${q.id}" class="search-box" style="width:25%;" value="${(opts[2]||'').replace(/"/g, '&quot;')}">
                    <input type="text" id="eo4_${q.id}" class="search-box" style="width:25%;" value="${(opts[3]||'').replace(/"/g, '&quot;')}">
                </div>
            </div>
            ` : ''}
            <button class="btn btn-success" style="margin-top:15px;" onclick="saveQuestion('${q.id}')">Save & Approve</button>
        `;
        container.appendChild(div);
    });
}

async function saveQuestion(id) {
    const q = loadedQuestions.find(x => x.id === id);
    if (!q) return;
    
    q.question = document.getElementById(`eq_${id}`).value;
    q.answer = document.getElementById(`ea_${id}`).value;
    q.marks = parseInt(document.getElementById(`em_${id}`).value) || 0;
    
    if (q.type === 'mcq') {
        const o1 = document.getElementById(`eo1_${id}`).value;
        const o2 = document.getElementById(`eo2_${id}`).value;
        const o3 = document.getElementById(`eo3_${id}`).value;
        const o4 = document.getElementById(`eo4_${id}`).value;
        q.optionsJson = JSON.stringify([o1, o2, o3, o4].filter(x => x));
    }
    
    const res = await fetch('/api/questions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(q)
    });
    
    if (res.ok) {
        alert("Question updated successfully!");
    } else {
        alert("Failed to save.");
    }
}

fetchData();
</script>
"""

new_html = content[:start] + expert_body + "</body></html>"
with open("app/src/main/assets/web_expert.html", "w") as f:
    f.write(new_html)
