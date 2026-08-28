import re

with open("app/src/main/assets/web_dashboard.html", "r") as f:
    content = f.read()

# Replace simple p_count with type-wise counts
old_count = r'<div class="form-group"><label>No\. of Qs \(Random\)</label><input type="number" id="p_count" value="10" required></div>'
new_count = """<div class="form-group">
                <label>No. of Qs (Random, Type-wise)</label>
                <div style="display:flex; gap:10px;">
                    <div><small>MCQ</small><input type="number" id="p_count_mcq" value="5" min="0" style="width:60px;" class="search-box"></div>
                    <div><small>FIB</small><input type="number" id="p_count_fib" value="0" min="0" style="width:60px;" class="search-box"></div>
                    <div><small>T/F</small><input type="number" id="p_count_tf" value="0" min="0" style="width:60px;" class="search-box"></div>
                    <div><small>Subj</small><input type="number" id="p_count_subj" value="5" min="0" style="width:60px;" class="search-box"></div>
                </div>
            </div>"""

content = re.sub(old_count, new_count, content, count=1)

# Now fix `function savePaper(e)` to use these counts.
# We need to pick questions based on type and count!
save_paper_start = r'const count = parseInt\(document\.getElementById\(\'p_count\'\)\.value\);[\s\S]*?const selectedQs = shuffled\.slice\(0, count\);'
save_paper_new = """const cMcq = parseInt(document.getElementById('p_count_mcq').value) || 0;
        const cFib = parseInt(document.getElementById('p_count_fib').value) || 0;
        const cTf = parseInt(document.getElementById('p_count_tf').value) || 0;
        const cSubj = parseInt(document.getElementById('p_count_subj').value) || 0;
        
        let pool = globalQuestions;
        if(subj) pool = pool.filter(q => q.bookTitle === subj);
        
        const mcqPool = pool.filter(q => q.type === 'mcq').sort(() => 0.5 - Math.random());
        const fibPool = pool.filter(q => q.type === 'fib').sort(() => 0.5 - Math.random());
        const tfPool = pool.filter(q => q.type === 'tf').sort(() => 0.5 - Math.random());
        const subjPool = pool.filter(q => q.type === 'subjective').sort(() => 0.5 - Math.random());
        
        const selectedQs = [
            ...mcqPool.slice(0, cMcq),
            ...fibPool.slice(0, cFib),
            ...tfPool.slice(0, cTf),
            ...subjPool.slice(0, cSubj)
        ];"""

content = re.sub(save_paper_start, save_paper_new, content, count=1)

with open("app/src/main/assets/web_dashboard.html", "w") as f:
    f.write(content)
