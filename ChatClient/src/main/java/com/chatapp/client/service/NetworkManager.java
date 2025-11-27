package com.chatapp.client.service;

import com.chatapp.client.model.Protocol;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NetworkManager {
    private static final Logger logger = LoggerFactory.getLogger(NetworkManager.class);
    private static NetworkManager instance;

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;
    private boolean connected;

    private final Map<String, Consumer<Protocol>> responseHandlers;
    private final Map<String, Consumer<Protocol>> notificationHandlers;

    private NetworkManager() {
        this.responseHandlers = new HashMap<>();
        this.notificationHandlers = new HashMap<>();
        this.connected = false;
    }

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            startListening();

            logger.info("Connected to server at {}:{}", SERVER_HOST, SERVER_PORT);
            return true;

        } catch (IOException e) {
            logger.error("Failed to connect to server", e);
            return false;
        }
    }

    public void disconnect() {
        connected = false;

        try {
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();

            logger.info("Disconnected from server");
        } catch (IOException e) {
            logger.error("Error disconnecting from server", e);
        }
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    final String response = line;
                    logger.debug("Received: {}", response);

                    Platform.runLater(() -> handleResponse(response));
                }
            } catch (IOException e) {
                if (connected) {
                    logger.error("Error reading from server", e);
                    Platform.runLater(this::handleConnectionLost);
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleResponse(String responseJson) {
        try {
            Protocol protocol = Protocol.fromJson(responseJson);
            String action = protocol.getAction();

            // Check if it's a notification
            if (action.startsWith("NOTIFY_")) {
                Consumer<Protocol> handler = notificationHandlers.get(action);
                if (handler != null) {
                    handler.accept(protocol);
                }
            } else {
                // It's a response to a request
                Consumer<Protocol> handler = responseHandlers.get(action);
                if (handler != null) {
                    handler.accept(protocol);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling response", e);
        }
    }

    private void handleConnectionLost() {
        connected = false;
        logger.error("Connection to server lost");
        // Notify UI about connection loss
        Consumer<Protocol> handler = notificationHandlers.get("CONNECTION_LOST");
        if (handler != null) {
            handler.accept(null);
        }
    }

    public void sendRequest(String action, JsonObject data, Consumer<Protocol> responseHandler) {
        if (!connected) {
            logger.error("Not connected to server");
            if (responseHandler != null) {
                Protocol errorResponse = Protocol.createResponse(action, false, "Not connected to server");
                Platform.runLater(() -> responseHandler.accept(errorResponse));
            }
            return;
        }

        Protocol request = new Protocol(action, data);
        responseHandlers.put(action, responseHandler);

        out.println(request.toJson());
        logger.debug("Sent: {}", request.toJson());
    }

    public void sendRequest(String action, Consumer<Protocol> responseHandler) {
        sendRequest(action, new JsonObject(), responseHandler);
    }

    public void setNotificationHandler(String notificationType, Consumer<Protocol> handler) {
        notificationHandlers.put(notificationType, handler);
    }

    public void removeNotificationHandler(String notificationType) {
        notificationHandlers.remove(notificationType);
    }

    /**
     * Send a notification/signal without expecting response
     */
    public void sendNotification(String action, JsonObject data) {
        if (!connected) {
            logger.error("Not connected to server");
            return;
        }

        Protocol notification = new Protocol(action, data);
        out.println(notification.toJson());
        logger.debug("Sent notification: {}", action);
    }

    public boolean isConnected() {
        return connected && socket != null && socket.isConnected();
    }
}
