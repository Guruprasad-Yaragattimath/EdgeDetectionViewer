📸 EdgeDetectionViewer

A real-time Android + OpenCV(C++) + OpenGL ES + WebSocket/WebServer project that performs live edge detection on camera frames using JNI/C++ and streams the processed RGBA frames to a Web Viewer using a Node.js server.

🚀 Features
Android App

Captures frames using Camera2 API (YUV_420_888).

Converts Y-plane → Edges → RGBA using JNI + C++ + OpenCV.

Renders processed frames using OpenGL ES (GLSurfaceView).

Streams RGBA frames to PC using:

⚡ HTTP POST (/upload-frame)

📡 WebSocket broadcast to the browser.

Efficient memory usage (buffer reuse, FPS throttling).

Web Viewer

Built using HTML + TypeScript.

Displays streaming RGBA frames in real-time.

Shows resolution + FPS.

Reconnects automatically on WebSocket disconnect.

Node.js Server

Serves the viewer UI.

Accepts raw binary frames via POST /upload-frame.

Broadcasts each frame to all connected WebSocket clients.

📱 Android Processing Pipeline

Camera2 captures Y-plane (YUV_420_888).

JNI receives Y bytes.

C++ / OpenCV converts:

Y → grayscale matrix

Canny edge detection

Edge map → RGBA buffer

Kotlin sends RGBA to OpenGL to display.

RGBA sent to Node.js server:

POST /upload-frame

Server broadcasts frames over WebSocket.

Web viewer displays the frame.


🖥️ How to Run the Web Viewer

1. Open terminal inside /webviewer folder
   cd webviewer
2. Install dependencies
   npm install
3. Start server
   node server.js

🚀 Web viewer server running at http://localhost:8080
🔌 WebSocket endpoint: ws://localhost:8080/ws

4. Open browser
   http://localhost:8080

📡 Android → PC Networking

Ensure both devices are connected:
1.via same WiFi OR
2.USB tethering OR
3.Ethernet sharing/hotspot

  ipconfig

  MainActivity.kt
   private val BASE_URL = "http://10.166.225.136:8080"
  viewer.ts
   const WS_URL = "ws://10.166.225.136:8080/ws";

   🧩 Technologies Used
   Android

Kotlin

Camera2 API

OpenGL ES 2.0

JNI + C++

OpenCV 4.x

OkHttp

Web

TypeScript / JavaScript

Canvas API

WebSocket

Backend

Node.js

Express.js

ws (WebSocket Library)

🧪 Testing

Viewer works in Chrome/Edge/Safari.

Works on any Android device with Camera2 support.

Tested with USB tethering, LAN, WiFi.

📝 Future Enhancements

Add color modes (Sobel, Laplacian).

Add compression (JPEG/WebP) for lower bandwidth use.

Add authentication.

Add mobile web viewer.


<img width="620" height="666" alt="Screenshot 2025-11-23 192457" src="https://github.com/user-attachments/assets/33a0b040-e7c8-43da-baac-735c72052681" />

OUTPUT:

![result!](https://github.com/user-attachments/assets/69463e15-ce5f-4bf1-8f30-e6d439e01b0a)

![output](https://github.com/user-attachments/assets/b191bd0a-96a3-47ba-9179-8d8b95b56fa1)





