import re

with open('app/src/main/assets/web_livetest.html', 'r') as f:
    text = f.read()

old_capture = """function captureFrame() {
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
}"""

new_capture = """function captureFrame() {
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

text = text.replace(old_capture, new_capture)

with open('app/src/main/assets/web_livetest.html', 'w') as f:
    f.write(text)
