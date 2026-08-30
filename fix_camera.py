import re

with open('app/src/main/assets/web_livetest.html', 'r') as f:
    text = f.read()

old_init = """async function initCamera() {
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
}"""

new_init = """async function initCamera() {
    try {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            console.warn("Media devices API not available. Usually happens on non-HTTPS origins.");
            const preview = document.getElementById('cameraPreview');
            if (preview) {
                preview.outerHTML = '<div style="width:100%; height:180px; background:#333; color:white; display:flex; align-items:center; justify-content:center; border-radius:8px; text-align:center; padding: 20px;">Camera Disabled<br><small>(Browser blocks camera on non-HTTPS IP addresses)</small></div>';
            }
            return;
        }
        videoStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
        const videoElement = document.getElementById('cameraPreview');
        if (videoElement) {
            videoElement.srcObject = videoStream;
        }
    } catch (err) {
        console.error("Camera error:", err);
        alert("Camera permission is blocked or unavailable. You may proceed without camera monitoring.");
    }
}"""

text = text.replace(old_init, new_init)

old_capture = """function captureFrame() {
    const videoElement = document.getElementById('cameraPreview');
    if (!videoStream || !videoElement) {
        return null;
    }
    const width = videoElement.videoWidth || 320;
    const height = videoElement.videoHeight || 240;
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', 0.5);
}"""

new_capture = """function captureFrame() {
    const fallback = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";
    const videoElement = document.getElementById('cameraPreview');
    if (!videoStream || !videoElement) {
        return fallback;
    }
    const width = videoElement.videoWidth || 320;
    const height = videoElement.videoHeight || 240;
    if (width === 0 || height === 0) return fallback;
    
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(videoElement, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', 0.5);
}"""

text = text.replace(old_capture, new_capture)

with open('app/src/main/assets/web_livetest.html', 'w') as f:
    f.write(text)

