package com.chatapp.client.controller;

import com.chatapp.client.model.Protocol;
import com.chatapp.client.service.NetworkManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField fullNameField;
    @FXML private Button registerButton;
    @FXML private Hyperlink loginLink;
    @FXML private Label messageLabel;

    private final NetworkManager networkManager;
    private final Gson gson;

    public RegisterController() {
        this.networkManager = NetworkManager.getInstance();
        this.gson = new Gson();
    }

    @FXML
    private void initialize() {
        messageLabel.setText("");
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String fullName = fullNameField.getText().trim();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            showMessage("Please fill in all fields", false);
            return;
        }

        if (username.length() < 3) {
            showMessage("Username must be at least 3 characters", false);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showMessage("Invalid email format", false);
            return;
        }

        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters", false);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Passwords do not match", false);
            return;
        }

        registerButton.setDisable(true);
        messageLabel.setText("Registering...");

        JsonObject data = new JsonObject();
        data.addProperty("username", username);
        data.addProperty("email", email);
        data.addProperty("password", password);
        data.addProperty("fullName", fullName);

        networkManager.sendRequest(Protocol.ACTION_REGISTER, data, response -> {
            registerButton.setDisable(false);

            if (response.isSuccess()) {
                showMessage("Registration successful! Please login.", true);

                // Wait a bit then go to login
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        javafx.application.Platform.runLater(this::handleBackToLogin);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

            } else {
                showMessage(response.getMessage(), false);
            }
        });
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginLink.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error loading login form", false);
        }
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }
}
