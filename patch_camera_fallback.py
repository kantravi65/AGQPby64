import re

with open('app/src/main/assets/web_livetest.html', 'r') as f:
    text = f.read()

# Replace the camera initialization script
old_camera = """async function initCamera() {
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

new_camera = """async function initCamera() {
    try {
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            enableCameraFallback();
            return;
        }
        videoStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
        const videoElement = document.getElementById('cameraPreview');
        if (videoElement) {
            videoElement.srcObject = videoStream;
        }
    } catch (err) {
        console.error("Camera error:", err);
        enableCameraFallback();
    }
}

let fallbackImageBase64 = null;

function enableCameraFallback() {
    const preview = document.getElementById('cameraPreview');
    if (preview) {
        preview.outerHTML = `
            <div style="width:100%; height:180px; background:#333; color:white; display:flex; flex-direction:column; align-items:center; justify-content:center; border-radius:8px; text-align:center; padding: 20px;" id="fallbackContainer">
                <span style="margin-bottom:10px;">Live Camera Blocked (HTTP).<br>Take a photo manually:</span>
                <input type="file" accept="image/*" capture="user" id="manualPhotoInput" style="max-width: 90%;">
                <img id="manualPhotoPreview" style="display:none; max-height:100px; margin-top:10px; border-radius:4px;"/>
            </div>
        `;
        document.getElementById('manualPhotoInput').addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(evt) {
                    fallbackImageBase64 = evt.target.result;
                    const imgPreview = document.getElementById('manualPhotoPreview');
                    imgPreview.src = fallbackImageBase64;
                    imgPreview.style.display = 'block';
                };
                reader.readAsDataURL(file);
            }
        });
    }
}
"""

text = text.replace(old_camera, new_camera)

old_capture = """function captureFrame() {
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

new_capture = """function captureFrame() {
    if (fallbackImageBase64) return fallbackImageBase64;
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

