// server.js
const path = require("path");
const express = require("express");
const http = require("http");
const WebSocket = require("ws");

const app = express();
const PORT = 8080;

// Serve static files (index.html, viewer.js, etc.)
app.use(express.static(__dirname));

// Serve index
app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "index.html"));
});

// Raw body for upload endpoint (binary frames)
app.use("/upload-frame", express.raw({ type: "application/octet-stream", limit: "50mb" }));

const server = http.createServer(app);

// WebSocket server on path /ws
const wss = new WebSocket.Server({ server, path: "/ws" });

wss.on("connection", (ws) => {
  console.log("✅ WebSocket client connected (total:", wss.clients.size, ")");
  ws.on("close", () => {
    console.log("❌ WebSocket client disconnected (total:", wss.clients.size, ")");
  });
});

// POST endpoint - receives binary frame and broadcasts it
app.post("/upload-frame", (req, res) => {
  try {
    const buf = req.body; // Buffer
    if (!Buffer.isBuffer(buf) || buf.length < 8) {
      res.status(400).send("invalid frame");
      return;
    }

    // Validate header and length roughly (optional)
    const width = buf.readUInt32LE(0);
    const height = buf.readUInt32LE(4);
    const expected = width * height * 4;
    if (buf.length !== 8 + expected) {
      console.warn(`Warning: frame size mismatch: got ${buf.length - 8}, expected ${expected}`);
      // we still broadcast, to let viewer do size checks
    }

    // Broadcast raw buffer to all connected WS clients
    wss.clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(buf);
      }
    });

    res.status(200).send("ok");
  } catch (err) {
    console.error("Error in /upload-frame:", err);
    res.status(500).send("server error");
  }
});

server.listen(PORT, () => {
  console.log(`🚀 Web viewer server running at http://localhost:${PORT}`);
  console.log(`🔌 WebSocket endpoint: ws://localhost:${PORT}/ws`);
});

