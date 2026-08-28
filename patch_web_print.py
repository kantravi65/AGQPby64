import re

with open("app/src/main/assets/web_dashboard.html", "r") as f:
    content = f.read()

# I need to change `function executePrint(e)` in web_dashboard.html
# Find the function executePrint(e) block and replace it
print_start = r'function executePrint\(e\) \{[\s\S]*?printWin\.document\.close\(\);\s*\}'

new_print = """function executePrint(e) {
        e.preventDefault();
        const id = document.getElementById('pr_id').value;
        const p = globalPapers.find(x => x.id === id);
        if(!p) return;
        
        const watermark = document.getElementById('pr_watermark').value;
        const wmPattern = document.getElementById('pr_watermarkPattern').value;
        const wmStyle = document.getElementById('pr_watermarkStyle').value;
        
        const showAns = document.getElementById('pr_ans').value;
        const showExp = document.getElementById('pr_explanations').value;
        const showCandidate = document.getElementById('pr_candidateBox').value;
        
        const fontSize = document.getElementById('pr_fontSize').value;
        const margin = document.getElementById('pr_margin').value;
        const lineSpacing = document.getElementById('pr_lineSpacing').value;

        // Call our backend API to generate exactly matching PDF!
        const url = `/api/papers/${id}/pdf?watermarkText=${encodeURIComponent(watermark)}&wmPattern=${wmPattern}&wmStyle=${wmStyle}&showAns=${showAns}&showExp=${showExp}&showCandidate=${showCandidate}&fontSize=${fontSize}&margin=${margin}&lineSpacing=${lineSpacing}`;
        
        window.open(url, '_blank');
        closeModal('printModal');
    }"""

content = re.sub(print_start, new_print, content, count=1)

with open("app/src/main/assets/web_dashboard.html", "w") as f:
    f.write(content)
