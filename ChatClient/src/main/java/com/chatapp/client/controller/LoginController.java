package com.chatapp.client.controller;

import com.chatapp.client.model.Protocol;
import com.chatapp.client.model.User;
import com.chatapp.client.service.NetworkManager;
import com.chatapp.client.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private Label messageLabel;

    private final NetworkManager networkManager;
    private final Gson gson;

    public LoginController() {
        this.networkManager = NetworkManager.getInstance();
        this.gson = new Gson();
    }

    @FXML
    private void initialize() {
        messageLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter username and password", false);
            return;
        }

        loginButton.setDisable(true);
        messageLabel.setText("Logging in...");

        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("password", password);

        networkManager.sendRequest(Protocol.ACTION_LOGIN, data, response -> {
            loginButton.setDisable(false);

            if (response.isSuccess()) {
                User user = gson.fromJson(response.getData().get("user"), User.class);
                SessionManager.getInstance().setCurrentUser(user);

                showMessage("Login successful!", true);

                // Navigate to main window
                openMainWindow();
            } else {
                showMessage(response.getMessage(), false);
            }
        });
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Register.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) registerLink.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error loading registration form", false);
        }
    }

    private void openMainWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 900, 600);
            stage.setScene(scene);
            stage.setTitle("Chat App - " + SessionManager.getInstance().getCurrentUserFullName());
            stage.setResizable(true);

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error loading main window", false);
        }
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }
}
