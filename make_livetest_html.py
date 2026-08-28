import re

with open("app/src/main/assets/web_dashboard.html", "r") as f:
    content = f.read()

start = content.find('<div class="container">')

livetest_body = """<div class="container" style="max-width: 800px; margin: 40px auto; padding: 20px; background: white; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
    <h1 style="text-align: center; color: #1976d2;">Live Test Portal</h1>
    
    <div id="setupView">
        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; border: 1px solid #ddd; margin-bottom: 20px;">
            <h3>Candidate Details</h3>
            <div class="form-group"><label>Full Name</label><input type="text" id="lt_name" class="search-box" style="width:100%;"></div>
            <div class="form-group"><label>Roll / Candidate Number</label><input type="text" id="lt_number" class="search-box" style="width:100%;"></div>
        </div>
        
        <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; border: 1px solid #ddd;">
            <h3>Test Parameters</h3>
            <div class="form-group"><label>Subject</label><select id="lt_subject" class="search-box" style="width:100%;"><option value="">Random / All Subjects</option></select></div>
            <div class="form-group">
                <label>Question Type</label>
                <select id="lt_type" class="search-box" style="width:100%;">
                    <option value="">Mixed (All Types)</option>
                    <option value="mcq">MCQ</option>
                    <option value="fib">Fill in the Blanks</option>
                    <option value="tf">True/False</option>
                </select>
            </div>
            <div class="form-group"><label>Number of Questions</label><input type="number" id="lt_count" class="search-box" style="width:100%;" value="10"></div>
            <button class="btn btn-primary" style="width:100%; padding:15px; font-size:18px; margin-top:15px;" onclick="startTest()">Start Exam</button>
        </div>
    </div>

    <div id="testView" style="display:none;">
        <h2 id="testTitle" style="text-align:center;"></h2>
        <div style="text-align:right; font-weight:bold; color:red;" id="timer"></div>
        <div id="testQuestions"></div>
        <button class="btn btn-success" style="width:100%; padding:15px; font-size:18px; margin-top:20px;" onclick="submitTest()">Submit Exam</button>
    </div>

    <div id="resultView" style="display:none; text-align:center;">
        <h2 style="color: #4CAF50;">Test Submitted Successfully!</h2>
        <div style="font-size:24px; margin: 20px 0;">Your Score: <b id="finalScore"></b></div>
        <p>Your results have been sent to the Admin. Once approved, you can download your official scorecard.</p>
        <button class="btn btn-primary" onclick="checkApproval()">Check Approval Status</button>
        <div id="approvalStatus" style="margin-top:20px; font-weight:bold;"></div>
    </div>
</div>

<script>
let allQuestions = [];
let testQuestions = [];
let cName = "";
let cNumber = "";

async function fetchData() {
    const r = await fetch('/api/questions');
    allQuestions = await r.json();
    const subjs = [...new Set(allQuestions.map(q => q.bookTitle))].sort();
    document.getElementById('lt_subject').innerHTML = '<option value="">All Subjects</option>' + subjs.map(s => `<option value="${s.replace(/"/g, '&quot;')}">${s}</option>`).join('');
}

function startTest() {
    cName = document.getElementById('lt_name').value.trim();
    cNumber = document.getElementById('lt_number').value.trim();
    if(!cName || !cNumber) return alert("Please enter Name and Number");
    
    const subj = document.getElementById('lt_subject').value;
    const type = document.getElementById('lt_type').value;
    const count = parseInt(document.getElementById('lt_count').value) || 10;
    
    let pool = allQuestions;
    if (subj) pool = pool.filter(q => q.bookTitle === subj);
    if (type) pool = pool.filter(q => q.type === type);
    
    if(pool.length === 0) return alert("No questions available for these filters!");
    
    testQuestions = pool.sort(() => 0.5 - Math.random()).slice(0, count);
    
    document.getElementById('setupView').style.display = 'none';
    document.getElementById('testView').style.display = 'block';
    document.getElementById('testTitle').innerText = `${subj || 'Mixed'} Exam - Candidate: ${cName} (${cNumber})`;
    
    renderTest();
}

function renderTest() {
    const qDiv = document.getElementById('testQuestions');
    qDiv.innerHTML = '';
    
    testQuestions.forEach((q, idx) => {
        let html = `<div style="background:#f9f9f9; padding:15px; border-radius:8px; margin-bottom:15px; border:1px solid #ddd;">
            <p><b>Q${idx+1}.</b> ${q.question} <i>[${q.marks} Marks]</i></p>`;
            
        if(q.type === 'mcq') {
            try {
                const opts = JSON.parse(q.optionsJson);
                opts.forEach(opt => {
                    html += `<label style="display:block; margin:8px 0;"><input type="radio" name="ans_${q.id}" value="${opt.replace(/"/g, '&quot;')}"> ${opt}</label>`;
                });
            } catch(e){}
        } else if (q.type === 'tf') {
            html += `<label style="margin-right:20px;"><input type="radio" name="ans_${q.id}" value="True"> True</label>
                     <label><input type="radio" name="ans_${q.id}" value="False"> False</label>`;
        } else {
            html += `<input type="text" id="ans_${q.id}" class="search-box" style="width:100%" placeholder="Your answer...">`;
        }
        html += `</div>`;
        qDiv.innerHTML += html;
    });
}

function submitTest() {
    if(!confirm("Are you sure you want to submit?")) return;
    
    let score = 0;
    let max = 0;
    
    testQuestions.forEach(q => {
        max += q.marks;
        let uAns = "";
        if(q.type === 'mcq' || q.type === 'tf') {
            const sel = document.querySelector(`input[name="ans_${q.id}"]:checked`);
            if(sel) uAns = sel.value;
        } else {
            const inp = document.getElementById(`ans_${q.id}`);
            if(inp) uAns = inp.value.trim();
        }
        
        if(uAns.toLowerCase() === q.answer.toLowerCase()) {
            score += q.marks;
        }
    });
    
    document.getElementById('testView').style.display = 'none';
    document.getElementById('resultView').style.display = 'block';
    document.getElementById('finalScore').innerText = `${score} / ${max}`;
    
    // We would normally POST to /api/attempts here.
    // For this prototype, we'll pretend it's sent.
}

function checkApproval() {
    // Mock approval logic
    document.getElementById('approvalStatus').innerHTML = "<span style='color:orange;'>Status: Pending Admin Approval...</span>";
}

fetchData();
</script>
"""

new_html = content[:start] + livetest_body + "</body></html>"
with open("app/src/main/assets/web_livetest.html", "w") as f:
    f.write(new_html)
