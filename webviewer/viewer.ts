// CHANGE THIS TO YOUR PC LAN IP
// Example: ws://192.168.1.23:8080/ws
const WS_URL = "ws://10.166.225.136:8080/ws";

const canvas = document.getElementById("frameCanvas") as HTMLCanvasElement;
const ctx = canvas.getContext("2d");

const statusText = document.getElementById("statusText") as HTMLElement;
const resText = document.getElementById("resText") as HTMLElement;
const fpsText = document.getElementById("fpsText") as HTMLElement;

let lastFpsTime = performance.now();
let frameCount = 0;

function updateFps() {
  const now = performance.now();
  const delta = now - lastFpsTime;
  if (delta >= 1000) {
    const fps = (frameCount * 1000) / delta;
    fpsText.textContent = fps.toFixed(1);
    frameCount = 0;
    lastFpsTime = now;
  }
}

function applyFrame(buffer: ArrayBuffer) {
  if (!ctx) return;

  const view = new DataView(buffer);

  const width = view.getUint32(0, true);  // Little-endian width
  const height = view.getUint32(4, true); // Little-endian height

  const rgba = new Uint8ClampedArray(buffer, 8); // Skip header

  if (rgba.length !== width * height * 4) {
    console.warn("Invalid frame size", rgba.length, width, height);
    return;
  }

  // Resize canvas if needed
  if (canvas.width !== width || canvas.height !== height) {
    canvas.width = width;
    canvas.height = height;
  }

  const imgData = new ImageData(rgba, width, height);
  ctx.putImageData(imgData, 0, 0);

  // Update UI
  resText.textContent = `${width} x ${height}`;
  frameCount++;
  updateFps();
}

function setupWebSocket() {
  const ws = new WebSocket(WS_URL);
  ws.binaryType = "arraybuffer";

  ws.onopen = () => {
    statusText.textContent = "Connected";
    statusText.style.color = "#4caf50";
  };

  ws.onerror = () => {
    statusText.textContent = "WebSocket error";
    statusText.style.color = "#f44336";
  };

  ws.onclose = () => {
    statusText.textContent = "Disconnected (retrying...)";
    statusText.style.color = "#ff9800";

    // reconnect after 2 seconds
    setTimeout(() => {
      setupWebSocket();
    }, 2000);
  };

  ws.onmessage = (event) => {
    if (event.data instanceof ArrayBuffer) {
      applyFrame(event.data);
    } else if (typeof event.data === "string") {
      console.log("Server text:", event.data);
    }
  };
}

document.addEventListener("DOMContentLoaded", () => {
  if (!ctx) {
    console.error("2D context not available");
    return;
  }

  // Initial blank screen
  canvas.width = 320;
  canvas.height = 240;
  ctx.fillStyle = "black";
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  resText.textContent = "320 x 240";
  fpsText.textContent = "0";

  setupWebSocket();
});
