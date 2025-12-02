package com.chatapp.client.controller;

import com.chatapp.client.model.CallInfo;
import com.chatapp.client.model.Protocol;
import com.chatapp.client.model.User;
import com.chatapp.client.service.NetworkManager;
import com.chatapp.client.util.WebcamManager;
import com.chatapp.client.util.MediaStreamManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class CallController {
    // Voice call UI
    @FXML private VBox voiceContainer;
    @FXML private Label callerNameLabel;
    @FXML private Label callStatusLabel;
    @FXML private Label callDurationLabel;
    @FXML private Label callTypeLabel;

    // Video call UI
    @FXML private VBox videoContainer;
    @FXML private VBox videoInfoOverlay;
    @FXML private ImageView localVideoView;
    @FXML private ImageView remoteVideoView;
    @FXML private Label videoCallerNameLabel;
    @FXML private Label videoCallStatusLabel;
    @FXML private Label videoCallDurationLabel;

    // Control buttons
    @FXML private VBox acceptButtonContainer;
    @FXML private VBox rejectButtonContainer;
    @FXML private VBox muteButtonContainer;
    @FXML private VBox cameraButtonContainer;
    @FXML private Button acceptButton;
    @FXML private Button rejectButton;
    @FXML private Button endCallButton;
    @FXML private Button muteButton;
    @FXML private Button cameraButton;
    @FXML private Label muteLabel;
    @FXML private Label cameraLabel;

    private final NetworkManager networkManager;
    private final Gson gson;
    private CallInfo callInfo;
    private User otherUser;
    private boolean isIncoming;
    private long callStartTime;
    private Thread durationThread;

    // Media components
    private WebcamManager webcamManager;
    private MediaStreamManager mediaStreamManager;
    private boolean isMuted = false;
    private boolean isCameraOn = true;

    // WebRTC P2P components
    private WebView webView;
    private WebEngine webEngine;
    private boolean useWebRTC = true; // Use WebRTC P2P by default for video calls

    public CallController() {
        this.networkManager = NetworkManager.getInstance();
        this.gson = new Gson();
    }

    @FXML
    private void initialize() {
        callDurationLabel.setText("00:00");
        videoCallDurationLabel.setText("00:00");

        // Initialize WebRTC WebView for P2P video calls
        if (useWebRTC) {
            initializeWebRTC();
        } else {
            // Fallback to old webcam manager approach
            try {
                webcamManager = new WebcamManager();
                System.out.println("WebcamManager initialized successfully");
            } catch (Exception e) {
                System.err.println("Failed to initialize WebcamManager: " + e.getMessage());
                e.printStackTrace();
                webcamManager = null;
            }
        }
    }

    /**
     * Initialize WebRTC WebView for P2P video calls
     */
    private void initializeWebRTC() {
        try {
            webView = new WebView();
            webEngine = webView.getEngine();

            // Enable JavaScript
            webEngine.setJavaScriptEnabled(true);

            // Configure WebView to fill the container
            webView.setPrefWidth(800);
            webView.setPrefHeight(600);
            webView.setVisible(false); // Hidden initially, shown when video call starts

            // Load WebRTC page
            String webrtcPage = getClass().getResource("/webrtc-call.html").toExternalForm();
            System.out.println("Loading WebRTC page: " + webrtcPage);

            // Set JavaScript bridge after page loads
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    System.out.println("WebRTC page loaded successfully");

                    // Set Java bridge object
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaApp", new WebRTCBridge());

                    System.out.println("JavaScript bridge established");
                }
            });

            webEngine.load(webrtcPage);

            // Add WebView to video container (CRITICAL FIX)
            Platform.runLater(() -> {
                // Hide old ImageViews when using WebRTC
                if (localVideoView != null) {
                    localVideoView.setVisible(false);
                    localVideoView.setManaged(false);
                }
                if (remoteVideoView != null) {
                    remoteVideoView.setVisible(false);
                    remoteVideoView.setManaged(false);
                }

                // Add WebView to container
                videoContainer.getChildren().add(0, webView);
                System.out.println("WebView added to video container");
            });

            // Setup WebRTC signaling notification handlers
            setupWebRTCNotificationHandlers();

            System.out.println("WebRTC initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize WebRTC: " + e.getMessage());
            e.printStackTrace();
            useWebRTC = false;

            // Fallback to old approach
            try {
                webcamManager = new WebcamManager();
                System.out.println("Fallback to WebcamManager");
            } catch (Exception ex) {
                System.err.println("Failed to initialize WebcamManager: " + ex.getMessage());
            }
        }
    }

    /**
     * Setup handlers for WebRTC signaling notifications from server
     */
    private void setupWebRTCNotificationHandlers() {
        // Handle WebRTC Offer (as receiver)
        networkManager.setNotificationHandler(Protocol.NOTIFY_WEBRTC_OFFER, protocol -> {
            JsonObject data = protocol.getData().getAsJsonObject("data");
            String sdp = data.get("sdp").getAsString();

            System.out.println("Received WebRTC offer from server");

            Platform.runLater(() -> {
                try {
                    // Call JavaScript function to receive offer with proper JSON escaping
                    String escapedSdp = gson.toJson(sdp);
                    String script = "if(window.webrtc) { window.webrtc.receiveOffer(" + escapedSdp + "); }";
                    webEngine.executeScript(script);
                } catch (Exception e) {
                    System.err.println("Error passing offer to JavaScript: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        // Handle WebRTC Answer (as caller)
        networkManager.setNotificationHandler(Protocol.NOTIFY_WEBRTC_ANSWER, protocol -> {
            JsonObject data = protocol.getData().getAsJsonObject("data");
            String sdp = data.get("sdp").getAsString();

            System.out.println("Received WebRTC answer from server");

            Platform.runLater(() -> {
                try {
                    // Call JavaScript function to receive answer with proper JSON escaping
                    String escapedSdp = gson.toJson(sdp);
                    String script = "if(window.webrtc) { window.webrtc.receiveAnswer(" + escapedSdp + "); }";
                    webEngine.executeScript(script);
                } catch (Exception e) {
                    System.err.println("Error passing answer to JavaScript: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        // Handle ICE Candidate
        networkManager.setNotificationHandler(Protocol.NOTIFY_WEBRTC_ICE_CANDIDATE, protocol -> {
            JsonObject data = protocol.getData().getAsJsonObject("data");
            String candidate = data.get("candidate").getAsString();

            System.out.println("Received ICE candidate from server");

            Platform.runLater(() -> {
                try {
                    // Call JavaScript function to receive ICE candidate with proper JSON escaping
                    String escapedCandidate = gson.toJson(candidate);
                    String script = "if(window.webrtc) { window.webrtc.receiveIceCandidate(" + escapedCandidate + "); }";
                    webEngine.executeScript(script);
                } catch (Exception e) {
                    System.err.println("Error passing ICE candidate to JavaScript: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * JavaScript Bridge for WebRTC communication
     */
    public class WebRTCBridge {
        /**
         * Send WebRTC offer to server (called from JavaScript)
         */
        public void sendOffer(String offerJson) {
            System.out.println("Sending WebRTC offer to server");

            JsonObject data = new JsonObject();
            data.addProperty("receiverId", otherUser.getUserId());
            data.addProperty("sdp", offerJson);

            networkManager.sendRequest(Protocol.ACTION_WEBRTC_OFFER, data, response -> {
                if (response.isSuccess()) {
                    System.out.println("WebRTC offer sent successfully");
                } else {
                    System.err.println("Failed to send WebRTC offer: " + response.getMessage());
                }
            });
        }

        /**
         * Send WebRTC answer to server (called from JavaScript)
         */
        public void sendAnswer(String answerJson) {
            System.out.println("Sending WebRTC answer to server");

            JsonObject data = new JsonObject();
            data.addProperty("callerId", otherUser.getUserId());
            data.addProperty("sdp", answerJson);

            networkManager.sendRequest(Protocol.ACTION_WEBRTC_ANSWER, data, response -> {
                if (response.isSuccess()) {
                    System.out.println("WebRTC answer sent successfully");
                } else {
                    System.err.println("Failed to send WebRTC answer: " + response.getMessage());
                }
            });
        }

        /**
         * Send ICE candidate to server (called from JavaScript)
         */
        public void sendIceCandidate(String candidateJson) {
            JsonObject data = new JsonObject();
            data.addProperty("targetUserId", otherUser.getUserId());
            data.addProperty("candidate", candidateJson);

            networkManager.sendRequest(Protocol.ACTION_WEBRTC_ICE_CANDIDATE, data, response -> {
                if (!response.isSuccess()) {
                    System.err.println("Failed to send ICE candidate: " + response.getMessage());
                }
            });
        }

        /**
         * Called when WebRTC page is ready
         */
        public void onPageReady() {
            System.out.println("WebRTC page is ready");
        }

        /**
         * Called when call ends (from JavaScript)
         */
        public void onCallEnded() {
            System.out.println("Call ended from WebRTC");
            Platform.runLater(() -> {
                handleEndCall();
            });
        }

        /**
         * Called when connection fails (from JavaScript)
         */
        public void onConnectionFailed() {
            System.err.println("WebRTC connection failed");
            Platform.runLater(() -> {
                updateCallStatus("Connection failed");
            });
        }

        /**
         * Called on WebRTC errors (from JavaScript)
         */
        public void onError(String errorMessage) {
            System.err.println("WebRTC error: " + errorMessage);
            Platform.runLater(() -> {
                updateCallStatus("Error: " + errorMessage);
            });
        }
    }

    /**
     * Initialize for incoming call
     */
    public void initializeIncomingCall(CallInfo callInfo, User caller) {
        this.callInfo = callInfo;
        this.otherUser = caller;
        this.isIncoming = true;

        Platform.runLater(() -> {
            boolean isVideoCall = callInfo.getCallType() == CallInfo.CallType.VIDEO;

            if (isVideoCall) {
                // Show video UI
                videoContainer.setVisible(false);
                videoContainer.setManaged(false);
                voiceContainer.setVisible(true);
                voiceContainer.setManaged(true);
                videoInfoOverlay.setVisible(false);
                videoInfoOverlay.setManaged(false);

                callerNameLabel.setText(caller.getFullName());
                callTypeLabel.setText("Video Call");
                callStatusLabel.setText("Incoming video call...");
            } else {
                // Show voice UI
                voiceContainer.setVisible(true);
                voiceContainer.setManaged(true);
                videoContainer.setVisible(false);
                videoContainer.setManaged(false);

                callerNameLabel.setText(caller.getFullName());
                callTypeLabel.setText("Voice Call");
                callStatusLabel.setText("Incoming call...");
            }

            acceptButtonContainer.setVisible(true);
            acceptButtonContainer.setManaged(true);
            rejectButtonContainer.setVisible(true);
            rejectButtonContainer.setManaged(true);
            endCallButton.setVisible(false);
            muteButtonContainer.setVisible(false);
            muteButtonContainer.setManaged(false);
            cameraButtonContainer.setVisible(false);
            cameraButtonContainer.setManaged(false);
        });

        // Setup notification handlers for incoming call too
        setupCallNotificationHandlers();
    }

    /**
     * Initialize for outgoing call
     */
    public void initializeOutgoingCall(CallInfo callInfo, User receiver) {
        this.callInfo = callInfo;
        this.otherUser = receiver;
        this.isIncoming = false;

        Platform.runLater(() -> {
            boolean isVideoCall = callInfo.getCallType() == CallInfo.CallType.VIDEO;

            if (isVideoCall) {
                voiceContainer.setVisible(true);
                voiceContainer.setManaged(true);
                videoContainer.setVisible(false);
                videoContainer.setManaged(false);
                videoInfoOverlay.setVisible(false);

                callerNameLabel.setText(receiver.getFullName());
                callTypeLabel.setText("Video Call");
                callStatusLabel.setText("Calling...");
            } else {
                voiceContainer.setVisible(true);
                voiceContainer.setManaged(true);
                videoContainer.setVisible(false);
                videoContainer.setManaged(false);

                callerNameLabel.setText(receiver.getFullName());
                callTypeLabel.setText("Voice Call");
                callStatusLabel.setText("Calling...");
            }

            acceptButtonContainer.setVisible(false);
            acceptButtonContainer.setManaged(false);
            rejectButtonContainer.setVisible(false);
            rejectButtonContainer.setManaged(false);
            endCallButton.setVisible(true);
            muteButtonContainer.setVisible(false);
            muteButtonContainer.setManaged(false);
            cameraButtonContainer.setVisible(false);
            cameraButtonContainer.setManaged(false);
        });

        // Listen for call response
        setupCallNotificationHandlers();
    }

    private void setupCallNotificationHandlers() {
        // Handle when other person accepts
        networkManager.setNotificationHandler(Protocol.NOTIFY_CALL_ACCEPTED, protocol -> {
            Platform.runLater(() -> {
                updateCallStatus("Connected");
                startCallDuration();
                startMediaStream();
            });
        });

        // Handle when other person rejects
        networkManager.setNotificationHandler(Protocol.NOTIFY_CALL_REJECTED, protocol -> {
            Platform.runLater(() -> {
                updateCallStatus("Call rejected");
                closeWindow();
            });
        });

        // Handle when call ends
        networkManager.setNotificationHandler(Protocol.NOTIFY_CALL_ENDED, protocol -> {
            System.out.println("[CallController] Received NOTIFY_CALL_ENDED");

            // Stop media immediately (on current thread)
            stopCallDuration();
            stopMediaStream();

            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                updateCallStatus("Call ended");
                closeWindow();
            });
        });
    }

    @FXML
    private void handleAcceptCall() {
        JsonObject data = new JsonObject();
        data.addProperty("callId", callInfo.getCallId());

        networkManager.sendRequest(Protocol.ACTION_ACCEPT_CALL, data, response -> {
            if (response.isSuccess()) {
                Platform.runLater(() -> {
                    updateCallStatus("Connected");
                    acceptButtonContainer.setVisible(false);
                    acceptButtonContainer.setManaged(false);
                    rejectButtonContainer.setVisible(false);
                    rejectButtonContainer.setManaged(false);
                    endCallButton.setVisible(true);

                    // Show media controls
                    muteButtonContainer.setVisible(true);
                    muteButtonContainer.setManaged(true);

                    boolean isVideoCall = callInfo.getCallType() == CallInfo.CallType.VIDEO;
                    if (isVideoCall) {
                        cameraButtonContainer.setVisible(true);
                        cameraButtonContainer.setManaged(true);
                    }

                    startCallDuration();
                    startMediaStream();
                });
            }
        });
    }

    /**
     * Start media stream (video/audio)
     */
    private void startMediaStream() {
        boolean isVideoCall = callInfo.getCallType() == CallInfo.CallType.VIDEO;

        if (isVideoCall) {
            // Switch to video UI
            voiceContainer.setVisible(false);
            voiceContainer.setManaged(false);
            videoContainer.setVisible(true);
            videoContainer.setManaged(true);
            videoInfoOverlay.setVisible(true);
            videoInfoOverlay.setManaged(true);

            // Update video overlay info
            videoCallerNameLabel.setText(otherUser.getFullName());
            videoCallStatusLabel.setText("Connected");

            if (useWebRTC && webView != null) {
                // Use WebRTC P2P for video calls
                System.out.println("Starting WebRTC P2P video call");
                webView.setVisible(true);

                try {
                    // Start WebRTC call (JavaScript will handle getUserMedia and peer connection)
                    if (webEngine != null) {
                        if (isIncoming) {
                            // Receiver: Wait for offer (already handled in setupWebRTCNotificationHandlers)
                            webEngine.executeScript("if(window.webrtc) { window.webrtc.waitForOffer(); }");
                            System.out.println("WebRTC: Waiting for offer from caller");
                        } else {
                            // Caller: Create and send offer
                            webEngine.executeScript("if(window.webrtc) { window.webrtc.startCall(); }");
                            System.out.println("WebRTC: Starting call as caller");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to start WebRTC call: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Fallback to old client-server relay approach
                System.out.println("Using client-server relay for video call (WebRTC not available)");

                // Initialize media stream manager for fallback
                mediaStreamManager = new MediaStreamManager(callInfo.getCallId(), webcamManager);

                // Start local webcam display and video streaming
                if (webcamManager != null && webcamManager.isWebcamAvailable()) {
                    try {
                        // Show ImageViews for fallback
                        if (localVideoView != null) {
                            localVideoView.setVisible(true);
                            localVideoView.setManaged(true);
                        }
                        if (remoteVideoView != null) {
                            remoteVideoView.setVisible(true);
                            remoteVideoView.setManaged(true);
                        }

                        webcamManager.startCapture(localVideoView);
                        System.out.println("Local webcam capture started");

                        // Start video streaming to other user
                        mediaStreamManager.startVideoStream(otherUser.getUserId(), remoteVideoView);
                        System.out.println("Video streaming started to user " + otherUser.getUserId());

                    } catch (Exception e) {
                        System.err.println("Failed to start video stream: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("No webcam available or WebcamManager not initialized!");
                }

                // Start audio streaming
                try {
                    mediaStreamManager.startAudioStream(otherUser.getUserId());
                    System.out.println("Audio streaming started");
                } catch (Exception e) {
                    System.err.println("Failed to start audio stream: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            // Voice call - always use old approach (no WebRTC for voice-only)
            System.out.println("Starting voice call (audio only)");
            mediaStreamManager = new MediaStreamManager(callInfo.getCallId(), webcamManager);

            try {
                mediaStreamManager.startAudioStream(otherUser.getUserId());
                System.out.println("Audio streaming started");
            } catch (Exception e) {
                System.err.println("Failed to start audio stream: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Stop media stream
     */
    private void stopMediaStream() {
        System.out.println("[CallController] Stopping media stream...");

        // Stop WebRTC connection if active
        if (useWebRTC && webView != null && webEngine != null) {
            try {
                webEngine.executeScript("if(window.webrtc) { window.webrtc.endCall(); }");
                webView.setVisible(false);
                System.out.println("[CallController] WebRTC connection closed");
            } catch (Exception e) {
                System.err.println("Error stopping WebRTC: " + e.getMessage());
            }
        }

        // IMPORTANT: Stop MediaStreamManager FIRST (stops threads accessing webcam)
        if (mediaStreamManager != null) {
            mediaStreamManager.stopStreaming();
            mediaStreamManager = null;
        }

        // THEN stop webcam (safe now that threads are stopped)
        if (webcamManager != null) {
            webcamManager.stopCapture();
        }

        System.out.println("[CallController] Media stream stopped");
    }

    /**
     * Update call status across all UI elements
     */
    private void updateCallStatus(String status) {
        callStatusLabel.setText(status);
        videoCallStatusLabel.setText(status);
    }

    /**
     * Toggle microphone mute
     */
    @FXML
    private void handleToggleMute() {
        isMuted = !isMuted;

        if (isMuted) {
            muteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 24; -fx-background-radius: 50; -fx-cursor: hand;");
            muteLabel.setText("Unmute");

            // Mute microphone
            if (useWebRTC && webEngine != null) {
                try {
                    webEngine.executeScript("if(window.webrtc && window.webrtc.toggleMute) { window.webrtc.toggleMute(true); }");
                    System.out.println("Microphone muted (WebRTC)");
                } catch (Exception e) {
                    System.err.println("Error muting microphone: " + e.getMessage());
                }
            } else {
                // Fallback: mute not implemented for client-server relay
                // TODO: Implement mute for MediaStreamManager if needed
                System.out.println("Mute not available in fallback mode");
            }
        } else {
            muteButton.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-font-size: 24; -fx-background-radius: 50; -fx-cursor: hand;");
            muteLabel.setText("Mute");

            // Unmute microphone
            if (useWebRTC && webEngine != null) {
                try {
                    webEngine.executeScript("if(window.webrtc && window.webrtc.toggleMute) { window.webrtc.toggleMute(false); }");
                    System.out.println("Microphone unmuted (WebRTC)");
                } catch (Exception e) {
                    System.err.println("Error unmuting microphone: " + e.getMessage());
                }
            } else {
                // Fallback: unmute not implemented for client-server relay
                System.out.println("Unmute not available in fallback mode");
            }
        }
    }

    /**
     * Toggle camera on/off
     */
    @FXML
    private void handleToggleCamera() {
        isCameraOn = !isCameraOn;

        if (isCameraOn) {
            cameraButton.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-font-size: 24; -fx-background-radius: 50; -fx-cursor: hand;");
            cameraLabel.setText("Camera");

            // Turn camera on
            if (useWebRTC && webEngine != null) {
                try {
                    webEngine.executeScript("if(window.webrtc && window.webrtc.toggleCamera) { window.webrtc.toggleCamera(true); }");
                    System.out.println("Camera enabled (WebRTC)");
                } catch (Exception e) {
                    System.err.println("Error enabling camera: " + e.getMessage());
                }
            } else {
                // Fallback
                localVideoView.setVisible(true);
                if (webcamManager != null) {
                    webcamManager.startCapture(localVideoView);
                    System.out.println("Camera enabled (fallback)");
                }
            }
        } else {
            cameraButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 24; -fx-background-radius: 50; -fx-cursor: hand;");
            cameraLabel.setText("Camera Off");

            // Turn camera off
            if (useWebRTC && webEngine != null) {
                try {
                    webEngine.executeScript("if(window.webrtc && window.webrtc.toggleCamera) { window.webrtc.toggleCamera(false); }");
                    System.out.println("Camera disabled (WebRTC)");
                } catch (Exception e) {
                    System.err.println("Error disabling camera: " + e.getMessage());
                }
            } else {
                // Fallback
                localVideoView.setVisible(false);
                if (webcamManager != null) {
                    webcamManager.stopCapture();
                    System.out.println("Camera disabled (fallback)");
                }
            }
        }
    }

    @FXML
    private void handleRejectCall() {
        System.out.println("[CallController] Reject call button pressed");

        // Stop media immediately (don't wait for server response)
        stopMediaStream();
        updateCallStatus("Call rejected");

        // Then notify server
        JsonObject data = new JsonObject();
        data.addProperty("callId", callInfo.getCallId());

        networkManager.sendRequest(Protocol.ACTION_REJECT_CALL, data, response -> {
            // Server notified, now close window
            Platform.runLater(() -> {
                closeWindow();
            });
        });
    }

    @FXML
    private void handleEndCall() {
        System.out.println("[CallController] End call button pressed");

        // Stop media immediately (don't wait for server response)
        stopCallDuration();
        stopMediaStream();
        updateCallStatus("Call ended");

        // Then notify server
        JsonObject data = new JsonObject();
        data.addProperty("callId", callInfo.getCallId());

        networkManager.sendRequest(Protocol.ACTION_END_CALL, data, response -> {
            // Server notified, now close window
            Platform.runLater(() -> {
                closeWindow();
            });
        });
    }

    private void startCallDuration() {
        callStartTime = System.currentTimeMillis();

        durationThread = new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    Thread.sleep(1000);
                    long duration = (System.currentTimeMillis() - callStartTime) / 1000;
                    long minutes = duration / 60;
                    long seconds = duration % 60;

                    String timeStr = String.format("%02d:%02d", minutes, seconds);

                    Platform.runLater(() -> {
                        callDurationLabel.setText(timeStr);
                        videoCallDurationLabel.setText(timeStr);
                    });
                }
            } catch (InterruptedException e) {
                // Thread stopped
            }
        });
        durationThread.setDaemon(true);
        durationThread.start();
    }

    private void stopCallDuration() {
        if (durationThread != null) {
            durationThread.interrupt();
        }
    }

    private void closeWindow() {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait 2 seconds before closing
                Platform.runLater(() -> {
                    Stage stage = (Stage) callDurationLabel.getScene().getWindow();
                    stage.close();
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
