const info = document.getElementById("info") as HTMLDivElement;
const img = document.getElementById("frame") as HTMLImageElement;

let last = performance.now();

function updateStats() {
    let now = performance.now();
    let fps = Math.round(1000 / (now - last));
    last = now;
    info.innerText = "FPS: " + fps + "   Resolution: 1280 x 720";
    requestAnimationFrame(updateStats);
}

updateStats();

// Refresh image every 2 seconds
setInterval(() => {
    img.src = "assets/processed.png?rand=" + Math.random();
}, 2000);
