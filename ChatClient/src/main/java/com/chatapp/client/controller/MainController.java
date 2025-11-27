package com.chatapp.client.controller;

import com.chatapp.client.model.*;
import com.chatapp.client.service.NetworkManager;
import com.chatapp.client.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MainController {
    @FXML private Label userNameLabel;
    @FXML private Label userStatusLabel;
    @FXML private Label statusDot;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private ListView<User> friendListView;
    @FXML private ListView<Group> groupListView;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button logoutButton;
    @FXML private TabPane conversationsTabPane;
    @FXML private Button refreshStatusButton;
    @FXML private Button refreshFriendsButton;
    @FXML private Button refreshGroupsButton;

    // New UI components for chat panel
    @FXML private StackPane chatContainer;
    @FXML private VBox welcomeScreen;
    @FXML private BorderPane chatArea;
    @FXML private SplitPane mainSplitPane;

    private final NetworkManager networkManager;
    private final Gson gson;
    private final ObservableList<User> friendsList;
    private final ObservableList<Group> groupsList;

    // Track current chat
    private ChatController currentChatController;

    public MainController() {
        this.networkManager = NetworkManager.getInstance();
        this.gson = new Gson();
        this.friendsList = FXCollections.observableArrayList();
        this.groupsList = FXCollections.observableArrayList();
    }

    @FXML
    private void initialize() {
        setupUserInfo();
        setupStatusComboBox();
        setupFriendListView();
        setupGroupListView();
        setupNotificationHandlers();

        loadFriends();
        loadGroups();
        loadFriendRequests();
    }

    private void setupUserInfo() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        userNameLabel.setText(currentUser.getFullName());
        userStatusLabel.setText(currentUser.getUserStatus().toString());

        // Set initial status dot color
        updateStatusDotColor(currentUser.getUserStatus().toString());
    }

    private void setupStatusComboBox() {
        statusComboBox.setItems(FXCollections.observableArrayList(
            "ONLINE", "AWAY", "BUSY", "OFFLINE"
        ));
        statusComboBox.setValue(SessionManager.getInstance().getCurrentUser().getUserStatus().toString());

        statusComboBox.setOnAction(event -> handleStatusChange());
    }

    private void setupFriendListView() {
        friendListView.setItems(friendsList);
        friendListView.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(user.getFullName() + " (@" + user.getUsername() + ") - " + user.getUserStatus());
                    setStyle(user.getUserStatus() == UserStatus.ONLINE ? "-fx-text-fill: green;" : "");
                }
            }
        });

        friendListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                User selectedFriend = friendListView.getSelectionModel().getSelectedItem();
                if (selectedFriend != null) {
                    openChatWindow(selectedFriend, null);
                }
            }
        });
    }

    private void setupGroupListView() {
        groupListView.setItems(groupsList);
        groupListView.setCellFactory(param -> new ListCell<Group>() {
            @Override
            protected void updateItem(Group group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                } else {
                    setText(group.getGroupName() + " (" + group.getMemberIds().size() + " members)");
                }
            }
        });

        groupListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Group selectedGroup = groupListView.getSelectionModel().getSelectedItem();
                if (selectedGroup != null) {
                    openChatWindow(null, selectedGroup);
                }
            }
        });
    }

    private void setupNotificationHandlers() {
        networkManager.setNotificationHandler(Protocol.NOTIFY_USER_ONLINE, protocol -> {
            User user = gson.fromJson(protocol.getData().get("data"), User.class);
            updateFriendStatus(user, true);
        });

        networkManager.setNotificationHandler(Protocol.NOTIFY_USER_OFFLINE, protocol -> {
            User user = gson.fromJson(protocol.getData().get("data"), User.class);
            updateFriendStatus(user, false);
        });

        networkManager.setNotificationHandler(Protocol.NOTIFY_NEW_MESSAGE, protocol -> {
            Platform.runLater(() -> showAlert("New Message", "You have a new message!"));
        });

        networkManager.setNotificationHandler(Protocol.NOTIFY_FRIEND_REQUEST, protocol -> {
            Platform.runLater(() -> {
                showAlert("Friend Request", "You have a new friend request!");
                loadFriendRequests();
            });
        });
    }

    private void loadFriends() {
        networkManager.sendRequest(Protocol.ACTION_GET_FRIENDS, response -> {
            if (response.isSuccess()) {
                List<User> friends = gson.fromJson(
                    response.getData().get("friends"),
                    new TypeToken<List<User>>(){}.getType()
                );
                friendsList.setAll(friends);
            }
        });
    }

    private void loadGroups() {
        networkManager.sendRequest(Protocol.ACTION_GET_GROUPS, response -> {
            if (response.isSuccess()) {
                List<Group> groups = gson.fromJson(
                    response.getData().get("groups"),
                    new TypeToken<List<Group>>(){}.getType()
                );
                groupsList.setAll(groups);
            }
        });
    }

    private void loadFriendRequests() {
        networkManager.sendRequest(Protocol.ACTION_GET_FRIEND_REQUESTS, response -> {
            if (response.isSuccess()) {
                List<FriendRequest> requests = gson.fromJson(
                    response.getData().get("requests"),
                    new TypeToken<List<FriendRequest>>(){}.getType()
                );

                if (!requests.isEmpty()) {
                    showFriendRequestsDialog(requests);
                }
            }
        });
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            showAlert("Search", "Please enter a search keyword");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("keyword", keyword);

        networkManager.sendRequest(Protocol.ACTION_SEARCH_USERS, data, response -> {
            if (response.isSuccess()) {
                List<User> users = gson.fromJson(
                    response.getData().get("users"),
                    new TypeToken<List<User>>(){}.getType()
                );
                showSearchResultsDialog(users);
            }
        });
    }

    private void showSearchResultsDialog(List<User> users) {
        Platform.runLater(() -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Search Results");
            dialog.setHeaderText("Found " + users.size() + " user(s)");

            ListView<User> listView = new ListView<>(FXCollections.observableArrayList(users));
            listView.setCellFactory(param -> new ListCell<User>() {
                private Button addButton = new Button("Add Friend");

                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(user.getFullName() + " (@" + user.getUsername() + ")");
                        addButton.setOnAction(e -> sendFriendRequest(user.getUserId()));
                        setGraphic(addButton);
                    }
                }
            });

            dialog.getDialogPane().setContent(listView);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        });
    }

    private void sendFriendRequest(int receiverId) {
        JsonObject data = new JsonObject();
        data.addProperty("receiverId", receiverId);

        networkManager.sendRequest(Protocol.ACTION_SEND_FRIEND_REQUEST, data, response -> {
            showAlert("Friend Request", response.getMessage());
        });
    }

    private void showFriendRequestsDialog(List<FriendRequest> requests) {
        Platform.runLater(() -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Friend Requests");
            dialog.setHeaderText("You have " + requests.size() + " friend request(s)");

            // Use ObservableList to allow dynamic updates
            ObservableList<FriendRequest> requestsList = FXCollections.observableArrayList(requests);
            ListView<FriendRequest> listView = new ListView<>(requestsList);
            listView.setCellFactory(param -> new ListCell<FriendRequest>() {
                @Override
                protected void updateItem(FriendRequest request, boolean empty) {
                    super.updateItem(request, empty);
                    if (empty || request == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(request.getSenderFullName() + " (@" + request.getSenderUsername() + ")");

                        Button acceptBtn = new Button("✓ Accept");
                        acceptBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                        acceptBtn.setOnAction(e -> {
                            handleFriendRequest(request.getRequestId(), true, requestsList, dialog);
                            acceptBtn.setDisable(true);
                        });

                        Button rejectBtn = new Button("✕ Reject");
                        rejectBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                        rejectBtn.setOnAction(e -> {
                            handleFriendRequest(request.getRequestId(), false, requestsList, dialog);
                            rejectBtn.setDisable(true);
                        });

                        setGraphic(new javafx.scene.layout.HBox(10, acceptBtn, rejectBtn));
                    }
                }
            });

            dialog.getDialogPane().setContent(listView);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        });
    }

    private void handleFriendRequest(int requestId, boolean accept, ObservableList<FriendRequest> requestsList, Dialog<ButtonType> dialog) {
        JsonObject data = new JsonObject();
        data.addProperty("requestId", requestId);

        String action = accept ? Protocol.ACTION_ACCEPT_FRIEND_REQUEST : Protocol.ACTION_REJECT_FRIEND_REQUEST;

        networkManager.sendRequest(action, data, response -> {
            Platform.runLater(() -> {
                if (response.isSuccess()) {
                    // Show success message
                    String message = accept ? "Friend request accepted!" : "Friend request rejected.";
                    showAlert("Friend Request", message);

                    // Remove request from list
                    requestsList.removeIf(req -> req.getRequestId() == requestId);

                    // Update dialog header
                    dialog.setHeaderText("You have " + requestsList.size() + " friend request(s)");

                    // Reload friends list if accepted
                    if (accept) {
                        loadFriends();
                    }

                    // Close dialog if no more requests
                    if (requestsList.isEmpty()) {
                        dialog.close();
                    }
                } else {
                    showAlert("Error", response.getMessage());
                }
            });
        });
    }

    private void handleStatusChange() {
        String newStatus = statusComboBox.getValue();
        JsonObject data = new JsonObject();
        data.addProperty("status", newStatus);

        networkManager.sendRequest(Protocol.ACTION_UPDATE_STATUS, data, response -> {
            if (response.isSuccess()) {
                Platform.runLater(() -> {
                    userStatusLabel.setText(newStatus);
                    updateStatusDotColor(newStatus);
                });
            }
        });
    }

    /**
     * Update status dot color based on status
     */
    private void updateStatusDotColor(String status) {
        switch (status) {
            case "ONLINE":
                statusDot.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 10;");
                userStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 11;");
                break;
            case "AWAY":
                statusDot.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 10;");
                userStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 11;");
                break;
            case "BUSY":
                statusDot.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10;");
                userStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11;");
                break;
            case "OFFLINE":
                statusDot.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10;");
                userStatusLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11;");
                break;
        }
    }

    @FXML
    private void handleCreateGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Group");
        dialog.setHeaderText("Create a new chat group");
        dialog.setContentText("Group name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(groupName -> {
            if (!groupName.trim().isEmpty()) {
                JsonObject data = new JsonObject();
                data.addProperty("groupName", groupName);
                data.addProperty("groupDescription", "");

                networkManager.sendRequest(Protocol.ACTION_CREATE_GROUP, data, response -> {
                    if (response.isSuccess()) {
                        showAlert("Success", "Group created successfully!");
                        loadGroups();
                    } else {
                        showAlert("Error", response.getMessage());
                    }
                });
            }
        });
    }

    @FXML
    private void handleLogout() {
        networkManager.sendRequest(Protocol.ACTION_LOGOUT, response -> {
            SessionManager.getInstance().clearSession();

            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) logoutButton.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setResizable(false);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * Load chat into the right panel (Zalo/Messenger style)
     */
    private void openChatWindow(User friend, Group group) {
        try {
            // Load Chat.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Chat.fxml"));
            Parent chatContent = loader.load();

            // Get controller and initialize
            ChatController controller = loader.getController();
            currentChatController = controller;

            if (friend != null) {
                controller.initializePrivateChat(friend);
                System.out.println("Loading chat with: " + friend.getFullName());
            } else if (group != null) {
                controller.initializeGroupChat(group);
                System.out.println("Loading group chat: " + group.getGroupName());
            }

            // Hide welcome screen, show chat area
            welcomeScreen.setVisible(false);
            welcomeScreen.setManaged(false);

            chatArea.setVisible(true);
            chatArea.setManaged(true);

            // Load chat content into center of chatArea
            chatArea.setCenter(chatContent);

            System.out.println("Chat loaded successfully into panel");

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load chat: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Unexpected error loading chat");
        }
    }

    private void updateFriendStatus(User user, boolean online) {
        for (int i = 0; i < friendsList.size(); i++) {
            User friend = friendsList.get(i);
            if (friend.getUserId() == user.getUserId()) {
                friend.setUserStatus(online ? UserStatus.ONLINE : UserStatus.OFFLINE);
                friendsList.set(i, friend); // Trigger update
                break;
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Refresh friends list
     */
    @FXML
    private void handleRefreshFriends() {
        System.out.println("Refreshing friends list...");
        loadFriends();
    }

    /**
     * Refresh groups list
     */
    @FXML
    private void handleRefreshGroups() {
        System.out.println("Refreshing groups list...");
        loadGroups();
    }

    /**
     * Refresh status from current session
     */
    @FXML
    private void handleRefreshStatus() {
        System.out.println("Refreshing user status display...");
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            Platform.runLater(() -> {
                userNameLabel.setText(currentUser.getFullName());
                userStatusLabel.setText(currentUser.getUserStatus().toString());
                updateStatusDotColor(currentUser.getUserStatus().toString());
                statusComboBox.setValue(currentUser.getUserStatus().toString());
                System.out.println("Status display refreshed: " + currentUser.getUserStatus());
            });
        }
    }
}
