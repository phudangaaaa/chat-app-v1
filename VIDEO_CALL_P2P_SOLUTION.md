# Video Call Architecture - Current Issues & P2P Solution

## 🔴 Vấn đề hiện tại

### Kiến trúc Client-Server (SAI)

```
Caller Client → Server → Receiver Client
     |                        |
     |← Video/Audio Relay ←---|
```

**Problems:**
1. ❌ **High Server Load**: Server phải relay tất cả video/audio frames
2. ❌ **High Latency**: Data phải đi qua server (double network trip)
3. ❌ **Bandwidth**: Server cần bandwidth lớn cho mỗi call
4. ❌ **Không scale**: 10 calls = server phải xử lý 20 video streams
5. ❌ **Quality**: Compression/decompression 2 lần làm giảm chất lượng

**Current Implementation:**
```java
// ClientHandler.java - Line 618
if (signalType.equals("VIDEO_FRAME") || signalType.equals("AUDIO_CHUNK")) {
    // Server đang relay video/audio data ← WRONG!
}
```

---

## ✅ Giải pháp đúng: P2P với WebRTC

### Kiến trúc P2P (ĐÚNG)

```
         Signaling Server
              /    \
            /        \
          /            \
Caller Client ←P2P→ Receiver Client
   (Direct video/audio connection)
```

**Benefits:**
1. ✅ **Low Latency**: Direct connection giữa 2 clients
2. ✅ **No Server Load**: Server chỉ làm signaling, không relay media
3. ✅ **Better Quality**: Không có compression trung gian
4. ✅ **Scales Well**: 1000 calls = server chỉ xử lý 1000 signaling messages
5. ✅ **NAT Traversal**: WebRTC tự động xử lý firewall/NAT

---

## 🛠️ Implementation Options

### Option 1: WebRTC với JavaScript (RECOMMENDED)

**Pros:**
- WebRTC được support native trong browser
- Dễ implement, nhiều thư viện
- Standards-compliant

**Cons:**
- Cần JavaFX WebView
- App cần embedded browser engine

**Implementation:**
```java
// Embed WebRTC trong JavaFX WebView
WebView webView = new WebView();
WebEngine engine = webView.getEngine();
engine.load("webrtc-call.html"); // HTML với WebRTC JS
```

**WebRTC Flow:**
1. Caller tạo Offer (SDP)
2. Server relay Offer → Receiver
3. Receiver tạo Answer (SDP)
4. Server relay Answer → Caller
5. Exchange ICE candidates qua server
6. **P2P connection established** ← Direct video/audio

---

### Option 2: Jitsi Meet Integration

**Pros:**
- Complete video conferencing solution
- Production-ready
- Open source

**Cons:**
- Very heavy (100+ MB dependencies)
- Overkill cho 1-1 calls

**Implementation:**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.jitsi</groupId>
    <artifactId>libjitsi</artifactId>
    <version>1.1</version>
</dependency>
```

---

### Option 3: Keep Client-Server but Optimize (QUICK FIX)

**Pros:**
- Minimal code changes
- Works with current architecture

**Cons:**
- Vẫn có vấn đề scalability
- High server bandwidth

**Optimizations:**
- Giảm video resolution (640x480 → 320x240)
- Giảm framerate (30fps → 15fps)
- Tăng compression
- Add TURN server để relay khi P2P không được

---

## 📋 Recommended Implementation Plan

### Phase 1: Add WebRTC Signaling (Server Side)

**Update server để handle WebRTC signaling:**

```java
// Protocol.java - Add new actions
public static final String ACTION_WEBRTC_OFFER = "WEBRTC_OFFER";
public static final String ACTION_WEBRTC_ANSWER = "WEBRTC_ANSWER";
public static final String ACTION_WEBRTC_ICE_CANDIDATE = "WEBRTC_ICE_CANDIDATE";

// ClientHandler.java - Add signaling handlers
private void handleWebRTCOffer(JsonObject data) {
    int receiverId = data.get("receiverId").getAsInt();
    String sdp = data.get("sdp").getAsString();

    // Relay offer to receiver (don't process media!)
    notifyUser(receiverId, Protocol.ACTION_WEBRTC_OFFER, data);
}

private void handleWebRTCAnswer(JsonObject data) {
    int callerId = data.get("callerId").getAsInt();
    String sdp = data.get("sdp").getAsString();

    // Relay answer to caller
    notifyUser(callerId, Protocol.ACTION_WEBRTC_ANSWER, data);
}

private void handleWebRTCIceCandidate(JsonObject data) {
    int targetUserId = data.get("targetUserId").getAsInt();
    String candidate = data.get("candidate").getAsString();

    // Relay ICE candidate
    notifyUser(targetUserId, Protocol.ACTION_WEBRTC_ICE_CANDIDATE, data);
}
```

---

### Phase 2: Client Side WebRTC (JavaFX WebView)

**Create WebRTC HTML/JS:**

```html
<!-- resources/webrtc-call.html -->
<!DOCTYPE html>
<html>
<head>
    <title>WebRTC Call</title>
</head>
<body>
    <video id="localVideo" autoplay muted></video>
    <video id="remoteVideo" autoplay></video>

    <script>
    let peerConnection;
    let localStream;

    // Initialize WebRTC
    async function startCall() {
        // Get local media
        localStream = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: true
        });
        document.getElementById('localVideo').srcObject = localStream;

        // Create peer connection
        peerConnection = new RTCPeerConnection({
            iceServers: [
                { urls: 'stun:stun.l.google.com:19302' }
            ]
        });

        // Add local stream
        localStream.getTracks().forEach(track => {
            peerConnection.addTrack(track, localStream);
        });

        // Handle remote stream
        peerConnection.ontrack = (event) => {
            document.getElementById('remoteVideo').srcObject = event.streams[0];
        };

        // Handle ICE candidates
        peerConnection.onicecandidate = (event) => {
            if (event.candidate) {
                // Send to server via Java bridge
                window.javaApp.sendIceCandidate(JSON.stringify(event.candidate));
            }
        };

        // Create offer
        const offer = await peerConnection.createOffer();
        await peerConnection.setLocalDescription(offer);

        // Send offer to server via Java bridge
        window.javaApp.sendOffer(JSON.stringify(offer));
    }

    // Receive answer from remote peer
    async function receiveAnswer(answerJson) {
        const answer = JSON.parse(answerJson);
        await peerConnection.setRemoteDescription(answer);
    }

    // Receive ICE candidate from remote peer
    async function receiveIceCandidate(candidateJson) {
        const candidate = JSON.parse(candidateJson);
        await peerConnection.addIceCandidate(candidate);
    }
    </script>
</body>
</html>
```

**Java Bridge:**

```java
// CallController.java
public class CallController {
    private WebView webView;
    private WebEngine webEngine;

    @FXML
    private void initialize() {
        // Initialize WebView for WebRTC
        webView = new WebView();
        webEngine = webView.getEngine();

        // Enable JavaScript bridge
        webEngine.setJavaScriptEnabled(true);

        // Set Java bridge object
        JSObject window = (JSObject) webEngine.executeScript("window");
        window.setMember("javaApp", new WebRTCBridge());

        // Load WebRTC page
        webEngine.load(getClass().getResource("/webrtc-call.html").toExternalForm());
    }

    // JavaScript bridge
    public class WebRTCBridge {
        public void sendOffer(String offerJson) {
            // Send offer to server
            JsonObject data = new JsonObject();
            data.addProperty("receiverId", otherUser.getUserId());
            data.addProperty("sdp", offerJson);
            networkManager.sendRequest(Protocol.ACTION_WEBRTC_OFFER, data, null);
        }

        public void sendAnswer(String answerJson) {
            JsonObject data = new JsonObject();
            data.addProperty("callerId", otherUser.getUserId());
            data.addProperty("sdp", answerJson);
            networkManager.sendRequest(Protocol.ACTION_WEBRTC_ANSWER, data, null);
        }

        public void sendIceCandidate(String candidateJson) {
            JsonObject data = new JsonObject();
            data.addProperty("targetUserId", otherUser.getUserId());
            data.addProperty("candidate", candidateJson);
            networkManager.sendRequest(Protocol.ACTION_WEBRTC_ICE_CANDIDATE, data, null);
        }
    }
}
```

---

### Phase 3: Handle NAT/Firewall (TURN Server)

**If P2P fails, use TURN server as fallback:**

```javascript
const peerConnection = new RTCPeerConnection({
    iceServers: [
        { urls: 'stun:stun.l.google.com:19302' },
        {
            urls: 'turn:your-turn-server.com:3478',
            username: 'user',
            credential: 'pass'
        }
    ]
});
```

**TURN Server Options:**
1. **CoTURN** (Open source, self-hosted)
2. **Twilio TURN** (Cloud service, pay-as-you-go)
3. **Xirsys** (Cloud TURN service)

---

## 🔧 Quick Fix (Temporary Solution)

Nếu không muốn implement WebRTC ngay:

### Optimize Current Implementation

```java
// ClientHandler.java
private void handleMediaSignal(JsonObject data) {
    String signalType = data.get("signalType").getAsString();

    if (signalType.equals("VIDEO_FRAME")) {
        // Optimize: Compress before relay
        String frameData = data.get("data").getAsString();

        // Check if we can skip frames (reduce FPS)
        if (shouldSkipFrame()) {
            return; // Skip this frame
        }

        // Compress frame data
        String compressedData = compressFrame(frameData);
        data.addProperty("data", compressedData);
    }

    // Relay to receiver
    int receiverId = data.get("receiverId").getAsInt();
    notifyUser(receiverId, Protocol.NOTIFY_MEDIA_SIGNAL, data);
}

private boolean shouldSkipFrame() {
    // Reduce to 15 FPS instead of 30
    frameCounter++;
    return frameCounter % 2 != 0;
}
```

---

## 📊 Comparison Table

| Feature | Current (Client-Server) | WebRTC (P2P) |
|---------|------------------------|--------------|
| Latency | 100-500ms | 20-100ms |
| Server Bandwidth | Very High | Very Low (signaling only) |
| Video Quality | Medium (double compression) | High (direct stream) |
| Max Users | ~50 concurrent calls | 1000+ concurrent calls |
| Implementation Complexity | Low | Medium |
| NAT Traversal | Not needed | Built-in (STUN/TURN) |
| Cost | High (server bandwidth) | Low (minimal server) |

---

## 🚀 Recommendation

### For Production App:
**Implement WebRTC with JavaFX WebView** (Option 1)
- Best quality
- Scalable
- Industry standard

### For Learning/Demo:
**Keep current but optimize** (Option 3)
- Simpler to understand
- Quick to implement
- Good enough for demo

### For Enterprise:
**Use Jitsi Meet Integration** (Option 2)
- Production-ready
- Full features
- Support & documentation

---

## 📝 Next Steps

1. **Decide on approach** (WebRTC vs Optimize current)
2. **If WebRTC:**
   - Add signaling actions to Protocol
   - Implement server-side relay handlers
   - Create WebRTC HTML/JS client
   - Add JavaFX WebView integration
   - Test with STUN server
   - Add TURN server for NAT traversal

3. **If Optimize:**
   - Reduce video resolution
   - Implement frame skipping
   - Add better compression
   - Limit concurrent calls
   - Monitor server resources

---

## 🔗 Resources

**WebRTC:**
- https://webrtc.org/
- https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API

**STUN/TURN Servers:**
- CoTURN: https://github.com/coturn/coturn
- Free STUN servers: stun.l.google.com:19302

**JavaFX WebView:**
- https://docs.oracle.com/javase/8/javafx/embedded-browser-tutorial/overview.htm

**Jitsi:**
- https://jitsi.org/
- https://github.com/jitsi/jitsi

---

**Made with ❤️ for building better video call architecture**
