package com.chatapp.client.controller;

import com.chatapp.client.model.*;
import com.chatapp.client.service.NetworkManager;
import com.chatapp.client.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.vdurmont.emoji.EmojiParser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class ChatController {
    @FXML private Label chatTitleLabel;
    @FXML private ListView<Message> messageListView;
    @FXML private TextArea messageInputArea;
    @FXML private Button sendButton;
    @FXML private Button sendFileButton;
    @FXML private Button emojiButton;
    @FXML private Button videoCallButton;
    @FXML private Button voiceCallButton;
    @FXML private Button addMemberButton;

    private final NetworkManager networkManager;
    private final Gson gson;
    private final ObservableList<Message> messages;

    private User friend;
    private Group group;
    private boolean isGroupChat;

    public ChatController() {
        this.networkManager = NetworkManager.getInstance();
        this.gson = new Gson();
        this.messages = FXCollections.observableArrayList();
    }

    @FXML
    private void initialize() {
        setupMessageListView();
        setupNotificationHandler();

        // Enter to send
        messageInputArea.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER") && !event.isShiftDown()) {
                handleSendMessage();
                event.consume();
            }
        });
    }

    public void initializePrivateChat(User friend) {
        this.friend = friend;
        this.isGroupChat = false;
        chatTitleLabel.setText("Chat with " + friend.getFullName());
        addMemberButton.setVisible(false);
        addMemberButton.setManaged(false);
        loadPrivateMessages();
    }

    public void initializeGroupChat(Group group) {
        this.group = group;
        this.isGroupChat = true;
        chatTitleLabel.setText("Group: " + group.getGroupName());
        addMemberButton.setVisible(true);
        addMemberButton.setManaged(true);
        voiceCallButton.setVisible(false);
        voiceCallButton.setManaged(false);
        videoCallButton.setVisible(false);
        videoCallButton.setManaged(false);
        loadGroupMessages();
    }

    private void setupMessageListView() {
        messageListView.setItems(messages);
        messageListView.setCellFactory(param -> new ListCell<Message>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(2);
                    vbox.setStyle("-fx-padding: 5;");

                    // Sender name (for group chats)
                    if (isGroupChat) {
                        Label senderLabel = new Label(message.getSenderName());
                        senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10;");
                        vbox.getChildren().add(senderLabel);
                    }

                    // Message content
                    boolean isMine = message.getSenderId() == SessionManager.getInstance().getCurrentUserId();

                    // Check if it's a file message
                    if (message.getMessageType() != MessageType.TEXT && message.getFileUrl() != null) {
                        // Create clickable file link
                        Hyperlink fileLink = new Hyperlink(formatMessageContent(message));
                        fileLink.setWrapText(true);
                        fileLink.setMaxWidth(400);
                        fileLink.setOnAction(e -> handleDownloadFile(message));

                        if (isMine) {
                            fileLink.setStyle("-fx-background-color: #dcf8c6; -fx-padding: 8; -fx-background-radius: 10;");
                            vbox.setStyle("-fx-alignment: center-right;");
                        } else {
                            fileLink.setStyle("-fx-background-color: #ffffff; -fx-padding: 8; -fx-background-radius: 10; -fx-border-color: #ddd; -fx-border-radius: 10;");
                            vbox.setStyle("-fx-alignment: center-left;");
                        }
                        vbox.getChildren().add(fileLink);
                    } else {
                        // Regular text message
                        Label contentLabel = new Label(formatMessageContent(message));
                        contentLabel.setWrapText(true);
                        contentLabel.setMaxWidth(400);

                        if (isMine) {
                            contentLabel.setStyle("-fx-background-color: #dcf8c6; -fx-padding: 8; -fx-background-radius: 10;");
                            vbox.setStyle("-fx-alignment: center-right;");
                        } else {
                            contentLabel.setStyle("-fx-background-color: #ffffff; -fx-padding: 8; -fx-background-radius: 10; -fx-border-color: #ddd; -fx-border-radius: 10;");
                            vbox.setStyle("-fx-alignment: center-left;");
                        }
                        vbox.getChildren().add(contentLabel);
                    }

                    // Timestamp
                    Label timeLabel = new Label(message.getSentAt() != null ? message.getSentAt().toString() : "");
                    timeLabel.setStyle("-fx-font-size: 9; -fx-text-fill: gray;");
                    vbox.getChildren().add(timeLabel);

                    setGraphic(vbox);
                }
            }
        });
    }

    private String formatMessageContent(Message message) {
        switch (message.getMessageType()) {
            case TEXT:
                return EmojiParser.parseToUnicode(message.getMessageContent());
            case IMAGE:
                return "[Image: " + message.getFileName() + "]";
            case FILE:
                return "[File: " + message.getFileName() + "]";
            case VIDEO:
                return "[Video: " + message.getFileName() + "]";
            case AUDIO:
                return "[Audio: " + message.getFileName() + "]";
            default:
                return message.getMessageContent();
        }
    }

    private void setupNotificationHandler() {
        networkManager.setNotificationHandler(Protocol.NOTIFY_NEW_MESSAGE, protocol -> {
            Message message = gson.fromJson(protocol.getData().get("data"), Message.class);

            // Check if message is for this chat
            if (isGroupChat) {
                if (message.getGroupId() != null && message.getGroupId() == group.getGroupId()) {
                    Platform.runLater(() -> messages.add(message));
                }
            } else {
                if (message.getSenderId() == friend.getUserId() ||
                    (message.getReceiverId() != null && message.getReceiverId() == friend.getUserId())) {
                    Platform.runLater(() -> messages.add(message));
                }
            }
        });

        // Handle incoming call notification
        networkManager.setNotificationHandler(Protocol.NOTIFY_INCOMING_CALL, protocol -> {
            CallInfo callInfo = gson.fromJson(protocol.getData().get("data"), CallInfo.class);
            if (!isGroupChat && callInfo.getCallerId() == friend.getUserId()) {
                Platform.runLater(() -> openCallWindow(callInfo, friend, true));
            }
        });
    }

    private void loadPrivateMessages() {
        JsonObject data = new JsonObject();
        data.addProperty("userId", friend.getUserId());
        data.addProperty("limit", 100);

        networkManager.sendRequest(Protocol.ACTION_GET_MESSAGES, data, response -> {
            if (response.isSuccess()) {
                List<Message> messageList = gson.fromJson(
                    response.getData().get("messages"),
                    new TypeToken<List<Message>>(){}.getType()
                );
                Collections.reverse(messageList); // Show oldest first
                messages.setAll(messageList);
            }
        });
    }

    private void loadGroupMessages() {
        JsonObject data = new JsonObject();
        data.addProperty("groupId", group.getGroupId());
        data.addProperty("limit", 100);

        System.out.println("Loading group messages for group ID: " + group.getGroupId());

        networkManager.sendRequest(Protocol.ACTION_GET_MESSAGES, data, response -> {
            if (response.isSuccess()) {
                try {
                    List<Message> messageList = gson.fromJson(
                        response.getData().get("messages"),
                        new TypeToken<List<Message>>(){}.getType()
                    );
                    System.out.println("Received " + messageList.size() + " group messages");
                    Collections.reverse(messageList);
                    Platform.runLater(() -> {
                        messages.setAll(messageList);
                        scrollToBottom();
                    });
                } catch (Exception e) {
                    System.err.println("Error parsing group messages: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("Failed to load group messages: " + response.getMessage());
            }
        });
    }

    @FXML
    private void handleSendMessage() {
        String content = messageInputArea.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("content", EmojiParser.parseToAliases(content));
        data.addProperty("type", MessageType.TEXT.name());

        String action;
        if (isGroupChat) {
            action = Protocol.ACTION_SEND_GROUP_MESSAGE;
            data.addProperty("groupId", group.getGroupId());
        } else {
            action = Protocol.ACTION_SEND_MESSAGE;
            data.addProperty("receiverId", friend.getUserId());
        }

        networkManager.sendRequest(action, data, response -> {
            if (response.isSuccess()) {
                Message message = gson.fromJson(response.getData().get("message"), Message.class);
                Platform.runLater(() -> {
                    messages.add(message);
                    messageInputArea.clear();
                    scrollToBottom();
                });
            } else {
                showAlert("Error", response.getMessage());
            }
        });
    }

    @FXML
    private void handleSendFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        File file = fileChooser.showOpenDialog(sendFileButton.getScene().getWindow());

        if (file != null) {
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);

                String fileType = determineFileType(file.getName());

                JsonObject data = new JsonObject();
                data.addProperty("fileName", file.getName());
                data.addProperty("fileData", base64Data);
                data.addProperty("fileType", fileType);

                if (isGroupChat) {
                    data.addProperty("groupId", group.getGroupId());
                } else {
                    data.addProperty("receiverId", friend.getUserId());
                }

                networkManager.sendRequest(Protocol.ACTION_SEND_FILE, data, response -> {
                    if (response.isSuccess()) {
                        showAlert("Success", "File sent successfully!");
                    } else {
                        showAlert("Error", "Failed to send file: " + response.getMessage());
                    }
                });

            } catch (IOException e) {
                showAlert("Error", "Failed to read file: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleInsertEmoji() {
        // Simple emoji picker
        ChoiceDialog<String> dialog = new ChoiceDialog<>("😀", "😀", "😂", "❤️", "👍", "🎉", "😊", "🔥", "💯", "✨");
        dialog.setTitle("Emoji Picker");
        dialog.setHeaderText("Select an emoji");
        dialog.setContentText("Emoji:");

        dialog.showAndWait().ifPresent(emoji -> {
            messageInputArea.appendText(emoji);
        });
    }

    @FXML
    private void handleAddMembers() {
        if (!isGroupChat || group == null) {
            return;
        }

        // Get current user's friends
        networkManager.sendRequest(Protocol.ACTION_GET_FRIENDS, response -> {
            if (response.isSuccess()) {
                List<User> friends = gson.fromJson(
                    response.getData().get("friends"),
                    new com.google.gson.reflect.TypeToken<List<User>>(){}.getType()
                );

                Platform.runLater(() -> showAddMembersDialog(friends));
            }
        });
    }

    private void showAddMembersDialog(List<User> friends) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Members to Group");
        dialog.setHeaderText("Select friends to add to " + group.getGroupName());

        // Filter out users who are already in the group
        List<User> availableFriends = new java.util.ArrayList<>();
        for (User friend : friends) {
            if (!group.getMemberIds().contains(friend.getUserId())) {
                availableFriends.add(friend);
            }
        }

        if (availableFriends.isEmpty()) {
            showAlert("Add Members", "All your friends are already in this group!");
            return;
        }

        ListView<User> friendListView = new ListView<>(FXCollections.observableArrayList(availableFriends));
        friendListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        friendListView.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getFullName() + " (@" + user.getUsername() + ")");
                }
            }
        });

        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Select members to add (Ctrl+Click for multiple):"),
            friendListView
        );
        content.setPrefHeight(300);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                List<User> selectedUsers = friendListView.getSelectionModel().getSelectedItems();
                if (!selectedUsers.isEmpty()) {
                    addMembersToGroup(selectedUsers);
                }
            }
        });
    }

    private void addMembersToGroup(List<User> users) {
        for (User user : users) {
            JsonObject data = new JsonObject();
            data.addProperty("groupId", group.getGroupId());
            data.addProperty("userId", user.getUserId());

            networkManager.sendRequest(Protocol.ACTION_JOIN_GROUP, data, response -> {
                if (response.isSuccess()) {
                    Platform.runLater(() -> {
                        group.addMember(user.getUserId());
                        showAlert("Success", user.getFullName() + " added to group!");
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("Error", "Failed to add " + user.getFullName() + ": " + response.getMessage());
                    });
                }
            });
        }
    }

    @FXML
    private void handleVideoCall() {
        if (!isGroupChat) {
            initiateCall(CallInfo.CallType.VIDEO);
        } else {
            showAlert("Video Call", "Video calls are only available in private chats");
        }
    }

    @FXML
    private void handleVoiceCall() {
        if (!isGroupChat) {
            initiateCall(CallInfo.CallType.VOICE);
        } else {
            showAlert("Voice Call", "Voice calls are only available in private chats");
        }
    }

    private void initiateCall(CallInfo.CallType callType) {
        JsonObject data = new JsonObject();
        data.addProperty("receiverId", friend.getUserId());
        data.addProperty("callType", callType.name());

        networkManager.sendRequest(Protocol.ACTION_INITIATE_CALL, data, response -> {
            if (response.isSuccess()) {
                CallInfo callInfo = gson.fromJson(response.getData().get("call"), CallInfo.class);
                Platform.runLater(() -> openCallWindow(callInfo, friend, false));
            } else {
                showAlert("Error", response.getMessage());
            }
        });
    }

    private void openCallWindow(CallInfo callInfo, User otherUser, boolean isIncoming) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Call.fxml"));
            Parent root = loader.load();

            CallController controller = loader.getController();
            if (isIncoming) {
                controller.initializeIncomingCall(callInfo, otherUser);
            } else {
                controller.initializeOutgoingCall(callInfo, otherUser);
            }

            Stage callStage = new Stage();
            callStage.setTitle(callInfo.getCallType().name() + " Call");
            callStage.setScene(new Scene(root));
            callStage.initModality(Modality.APPLICATION_MODAL);
            callStage.setResizable(false);
            callStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open call window: " + e.getMessage());
        }
    }

    private String determineFileType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                return "IMAGE";
            case "mp4":
            case "avi":
            case "mov":
                return "VIDEO";
            case "mp3":
            case "wav":
                return "AUDIO";
            default:
                return "FILE";
        }
    }

    private void handleDownloadFile(Message message) {
        if (message.getFileUrl() == null) {
            showAlert("Error", "File not found");
            return;
        }

        // Request file from server
        JsonObject data = new JsonObject();
        data.addProperty("filePath", message.getFileUrl());

        networkManager.sendRequest(Protocol.ACTION_RECEIVE_FILE, data, response -> {
            if (response.isSuccess()) {
                try {
                    String base64Data = response.getData().get("fileData").getAsString();
                    byte[] fileBytes = Base64.getDecoder().decode(base64Data);

                    // Show save dialog
                    Platform.runLater(() -> {
                        FileChooser fileChooser = new FileChooser();
                        fileChooser.setTitle("Save File");
                        fileChooser.setInitialFileName(message.getFileName());

                        File saveFile = fileChooser.showSaveDialog(messageListView.getScene().getWindow());
                        if (saveFile != null) {
                            try {
                                Files.write(saveFile.toPath(), fileBytes);
                                showAlert("Success", "File downloaded successfully!");
                            } catch (IOException e) {
                                showAlert("Error", "Failed to save file: " + e.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    showAlert("Error", "Failed to decode file: " + e.getMessage());
                }
            } else {
                showAlert("Error", "Failed to download file: " + response.getMessage());
            }
        });
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            if (!messages.isEmpty()) {
                messageListView.scrollTo(messages.size() - 1);
            }
        });
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
