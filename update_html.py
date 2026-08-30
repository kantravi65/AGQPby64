import re

with open('app/src/main/assets/web_livetest.html', 'r') as f:
    content = f.read()

# 1. Update form
form_addition = """        <div class="form-group">
            <label for="lt_email">Email Address</label>
            <input type="email" id="lt_email" class="form-input" placeholder="e.g. john@example.com">
        </div>
        <div class="form-group">
            <label for="lt_mobile">Mobile Number (SMS Enabled)</label>
            <input type="tel" id="lt_mobile" class="form-input" placeholder="e.g. +1234567890">
        </div>
        <div class="form-group">
            <label>Live Camera Registration</label>
            <video id="cameraPreview" autoplay playsinline muted style="width:100%; border-radius:8px; background:#000; height: 180px;"></video>
            <p style="font-size:12px; color:gray; text-align:center;">Camera required for proctoring. Please allow permissions.</p>
        </div>"""
        
content = content.replace('        <div style="background-color: #f0f7ff', form_addition + '\n        <div style="background-color: #f0f7ff')

# 2. Add camera init and heartbeat logic
js_additions = """
let cameraStream = null;
let currentPortraitBase64 = "";

async function setupCamera() {
    try {
        cameraStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
        document.getElementById('cameraPreview').srcObject = cameraStream;
    } catch(e) {
        console.error("Camera error:", e);
    }
}
window.addEventListener('load', setupCamera);

function captureFrame() {
    const video = document.getElementById('cameraPreview');
    if (!video || !cameraStream) return "";
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', 0.5);
}

// Heartbeat function to send periodic camera frames and receive warnings
setInterval(async () => {
    if (examActive && !examSubmitted && cNumber) {
        const frame = captureFrame();
        try {
            const resp = await fetch('/api/livetest/heartbeat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rollNumber: cNumber, frameBase64: frame })
            });
            if (resp.ok) {
                const data = await resp.json();
                if (data.warningMessage) {
                    alert("SUPERVISOR WARNING:\\n" + data.warningMessage);
                }
            }
        } catch(e) {}
    }
}, 5000);
"""
content = content.replace('let cheated = false;', js_additions + '\nlet cheated = false;')

# 3. Update initiateRegistration to include email, mobile, portrait
reg_old = """    cName = document.getElementById('lt_name').value.trim();
    cNumber = document.getElementById('lt_number').value.trim();
    if (!cName || !cNumber) {
        alert("Please enter both your Full Name and Candidate Number.");
        return;
    }

    // Call server to login and get questions according to admin settings
    try {
        const response = await fetch('/api/livetest/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: cName, rollNumber: cNumber })
        });"""

reg_new = """    cName = document.getElementById('lt_name').value.trim();
    cNumber = document.getElementById('lt_number').value.trim();
    const email = document.getElementById('lt_email').value.trim();
    const mobile = document.getElementById('lt_mobile').value.trim();
    
    if (!cName || !cNumber || !email || !mobile) {
        alert("Please fill in all details (Name, Roll, Email, Mobile).");
        return;
    }
    
    currentPortraitBase64 = captureFrame();
    if (!currentPortraitBase64) {
        alert("Please allow camera permissions to proceed.");
        return;
    }

    try {
        const response = await fetch('/api/livetest/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: cName, rollNumber: cNumber, email: email, mobile: mobile, portraitBase64: currentPortraitBase64 })
        });"""
content = content.replace(reg_old, reg_new)

with open('app/src/main/assets/web_livetest.html', 'w') as f:
    f.write(content)

