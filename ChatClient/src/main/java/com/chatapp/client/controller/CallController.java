package com.chatapp.client.controller;

import com.chatapp.client.model.CallInfo;
import com.chatapp.client.model.Protocol;
import com.chatapp.client.model.User;
import com.chatapp.client.service.NetworkManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class CallController {
    @FXML private Label callerNameLabel;
    @FXML private Label callStatusLabel;
    @FXML private Label callDurationLabel;
    @FXML private Button acceptButton;
    @FXML private Button rejectButton;
    @FXML private Button endCallButton;
    @FXML private Label callTypeLabel;

    private final NetworkManager networkManager;
    private final Gson gson;
    private CallInfo callInfo;
    private User otherUser;
    private boolean isIncoming;
    private long callStartTime;
    private Thread durationThread;

    public CallController() {
        this.networkManager = NetworkManager.getInstance();
        this.gson = new Gson();
    }

    @FXML
    private void initialize() {
        callDurationLabel.setText("00:00");
    }

    /**
     * Initialize for incoming call
     */
    public void initializeIncomingCall(CallInfo callInfo, User caller) {
        this.callInfo = callInfo;
        this.otherUser = caller;
        this.isIncoming = true;

        Platform.runLater(() -> {
            callerNameLabel.setText(caller.getFullName());
            callTypeLabel.setText(callInfo.getCallType().name() + " Call");
            callStatusLabel.setText("Incoming call...");

            acceptButton.setVisible(true);
            rejectButton.setVisible(true);
            endCallButton.setVisible(false);
        });
    }

    /**
     * Initialize for outgoing call
     */
    public void initializeOutgoingCall(CallInfo callInfo, User receiver) {
        this.callInfo = callInfo;
        this.otherUser = receiver;
        this.isIncoming = false;

        Platform.runLater(() -> {
            callerNameLabel.setText(receiver.getFullName());
            callTypeLabel.setText(callInfo.getCallType().name() + " Call");
            callStatusLabel.setText("Calling...");

            acceptButton.setVisible(false);
            rejectButton.setVisible(false);
            endCallButton.setVisible(true);
        });

        // Listen for call response
        setupCallNotificationHandlers();
    }

    private void setupCallNotificationHandlers() {
        // Handle when other person accepts
        networkManager.setNotificationHandler("CALL_ACCEPTED", protocol -> {
            Platform.runLater(() -> {
                callStatusLabel.setText("Connected");
                startCallDuration();
            });
        });

        // Handle when other person rejects
        networkManager.setNotificationHandler("CALL_REJECTED", protocol -> {
            Platform.runLater(() -> {
                callStatusLabel.setText("Call rejected");
                closeWindow();
            });
        });

        // Handle when call ends
        networkManager.setNotificationHandler("CALL_ENDED", protocol -> {
            Platform.runLater(() -> {
                stopCallDuration();
                callStatusLabel.setText("Call ended");
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
                    callStatusLabel.setText("Connected");
                    acceptButton.setVisible(false);
                    rejectButton.setVisible(false);
                    endCallButton.setVisible(true);
                    startCallDuration();
                });
            }
        });
    }

    @FXML
    private void handleRejectCall() {
        JsonObject data = new JsonObject();
        data.addProperty("callId", callInfo.getCallId());

        networkManager.sendRequest(Protocol.ACTION_REJECT_CALL, data, response -> {
            if (response.isSuccess()) {
                Platform.runLater(() -> {
                    callStatusLabel.setText("Call rejected");
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
                callStatusLabel.setText("Call ended");
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

                    Platform.runLater(() -> {
                        callDurationLabel.setText(String.format("%02d:%02d", minutes, seconds));
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
