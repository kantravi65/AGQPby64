import re

with open('app/src/main/assets/web_livetest.html', 'r') as f:
    html = f.read()

# Find the start of the script block
script_start = html.find('<script>') + 8

injection = """
let currentPortraitBase64 = null;
let videoStream = null;
let heartbeatInterval = null;

async function initCamera() {
    try {
        videoStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
        const videoElement = document.getElementById('cameraPreview');
        if (videoElement) {
            videoElement.srcObject = videoStream;
        }
    } catch (err) {
        console.error("Camera error:", err);
        alert("Camera permission is required. Please enable it to proceed.");
    }
}
window.addEventListener('load', initCamera);

function captureFrame() {
    const videoElement = document.getElementById('cameraPreview');
    if (!videoStream || !videoElement || !videoElement.videoWidth) {
        return null;
    }
    const canvas = document.createElement('canvas');
    canvas.width = videoElement.videoWidth;
    canvas.height = videoElement.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', 0.5);
}

async function sendHeartbeat() {
    if (!cNumber || isSubmitted) return;
    const frame = captureFrame();
    try {
        const response = await fetch('/api/livetest/heartbeat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ rollNumber: cNumber, frameBase64: frame || "" })
        });
        if (response.ok) {
            const data = await response.json();
            if (data.warningMessage && data.warningMessage.trim() !== "") {
                alert("SUPERVISOR WARNING: " + data.warningMessage);
            }
        }
    } catch (e) {
        console.error("Heartbeat error", e);
    }
}
"""

html = html[:script_start] + injection + html[script_start:]

# also need to start heartbeat in `requestFullScreenAndStart` or similar.
# Let's find requestFullScreenAndStart()

req_fs_old = """    startCheatMonitoring();
    startTimer();
    renderQuestions();"""
req_fs_new = """    startCheatMonitoring();
    startTimer();
    renderQuestions();
    
    // Start heartbeat
    heartbeatInterval = setInterval(sendHeartbeat, 5000);"""

html = html.replace(req_fs_old, req_fs_new)

# Stop heartbeat when submitted
submit_old = """    isSubmitted = true;
    document.exitFullscreen().catch(()=>{});"""
submit_new = """    isSubmitted = true;
    if (heartbeatInterval) clearInterval(heartbeatInterval);
    if (videoStream) videoStream.getTracks().forEach(t => t.stop());
    document.exitFullscreen().catch(()=>{});"""

html = html.replace(submit_old, submit_new)

with open('app/src/main/assets/web_livetest.html', 'w') as f:
    f.write(html)
