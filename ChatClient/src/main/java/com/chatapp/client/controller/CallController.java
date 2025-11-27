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
import javafx.stage.Stage;

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

    public CallController() {
        this.networkManager = NetworkManager.getInstance();
        this.gson = new Gson();
    }

    @FXML
    private void initialize() {
        callDurationLabel.setText("00:00");
        videoCallDurationLabel.setText("00:00");

        // Initialize webcam manager
        try {
            webcamManager = new WebcamManager();
            System.out.println("WebcamManager initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize WebcamManager: " + e.getMessage());
            e.printStackTrace();
            webcamManager = null;
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
            Platform.runLater(() -> {
                stopCallDuration();
                stopMediaStream();
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

            // Start webcam capture
            if (webcamManager != null && webcamManager.isWebcamAvailable()) {
                try {
                    webcamManager.startCapture(localVideoView);
                    System.out.println("Webcam capture started");
                } catch (Exception e) {
                    System.err.println("Failed to start webcam: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("No webcam available or WebcamManager not initialized!");
            }

            // Initialize media stream manager (for future P2P streaming)
            // mediaStreamManager = new MediaStreamManager("localhost", callInfo.getCallId());
            // mediaStreamManager.startVideoStream(frame -> {
            //     Platform.runLater(() -> remoteVideoView.setImage(frame));
            // });
        }

        // Start audio streaming
        // if (mediaStreamManager != null) {
        //     mediaStreamManager.startAudioStream();
        // }
    }

    /**
     * Stop media stream
     */
    private void stopMediaStream() {
        if (webcamManager != null) {
            webcamManager.stopCapture();
        }

        if (mediaStreamManager != null) {
            mediaStreamManager.stopStreaming();
        }
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
            // TODO: Actually mute microphone
        } else {
            muteButton.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-font-size: 24; -fx-background-radius: 50; -fx-cursor: hand;");
            muteLabel.setText("Mute");
            // TODO: Unmute microphone
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
            localVideoView.setVisible(true);
            if (webcamManager != null) {
                webcamManager.startCapture(localVideoView);
            }
        } else {
            cameraButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 24; -fx-background-radius: 50; -fx-cursor: hand;");
            cameraLabel.setText("Camera Off");
            localVideoView.setVisible(false);
            if (webcamManager != null) {
                webcamManager.stopCapture();
            }
        }
    }

    @FXML
    private void handleRejectCall() {
        JsonObject data = new JsonObject();
        data.addProperty("callId", callInfo.getCallId());

        networkManager.sendRequest(Protocol.ACTION_REJECT_CALL, data, response -> {
            if (response.isSuccess()) {
                Platform.runLater(() -> {
                    stopMediaStream();
                    updateCallStatus("Call rejected");
                    closeWindow();
                });
            }
        });
    }

    @FXML
    private void handleEndCall() {
        JsonObject data = new JsonObject();
        data.addProperty("callId", callInfo.getCallId());

        networkManager.sendRequest(Protocol.ACTION_END_CALL, data, response -> {
            Platform.runLater(() -> {
                stopCallDuration();
                stopMediaStream();
                updateCallStatus("Call ended");
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
