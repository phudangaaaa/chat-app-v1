package com.chatapp.client;

import com.chatapp.client.service.NetworkManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatClientApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ChatClientApp.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // Connect to server
            NetworkManager networkManager = NetworkManager.getInstance();
            if (!networkManager.connect()) {
                logger.error("Failed to connect to server");
                showErrorAndExit(primaryStage, "Cannot connect to server. Please make sure the server is running.");
                return;
            }

            // Load login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle("Chat Application");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

            // Handle window close
            primaryStage.setOnCloseRequest(event -> {
                networkManager.disconnect();
                System.exit(0);
            });

        } catch (Exception e) {
            logger.error("Error starting application", e);
            showErrorAndExit(primaryStage, "Error starting application: " + e.getMessage());
        }
    }

    private void showErrorAndExit(Stage stage, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        System.exit(1);
    }

    @Override
    public void stop() {
        NetworkManager.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
