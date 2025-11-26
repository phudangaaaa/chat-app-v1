package com.chatapp.server;

import com.chatapp.server.handler.ClientHandler;
import com.chatapp.server.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private static final int PORT = 8888;
    private static final int MAX_THREADS = 100;

    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final Map<Integer, ClientHandler> onlineUsers;
    private volatile boolean running;

    public ChatServer() {
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        this.onlineUsers = new ConcurrentHashMap<>();
        this.running = true;
    }

    public void start() {
        try {
            // Test database connection
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.testConnection()) {
                logger.error("Failed to connect to database. Please check your database configuration.");
                return;
            }
            logger.info("Database connection successful");

            // Start server
            serverSocket = new ServerSocket(PORT);
            logger.info("Chat Server started on port {}", PORT);
            logger.info("Waiting for clients...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("New client connection from: {}", clientSocket.getInetAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    threadPool.execute(clientHandler);

                } catch (IOException e) {
                    if (running) {
                        logger.error("Error accepting client connection", e);
                    }
                }
            }

        } catch (IOException e) {
            logger.error("Could not start server on port {}", PORT, e);
        } finally {
            stop();
        }
    }

    public void stop() {
        logger.info("Shutting down server...");
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }

        threadPool.shutdown();
        DatabaseManager.getInstance().closeConnection();
        logger.info("Server stopped");
    }

    public void addOnlineUser(int userId, ClientHandler handler) {
        onlineUsers.put(userId, handler);
        logger.info("User {} is now online. Total online users: {}", userId, onlineUsers.size());
    }

    public void removeOnlineUser(int userId) {
        onlineUsers.remove(userId);
        logger.info("User {} went offline. Total online users: {}", userId, onlineUsers.size());
    }

    public ClientHandler getOnlineUser(int userId) {
        return onlineUsers.get(userId);
    }

    public boolean isUserOnline(int userId) {
        return onlineUsers.containsKey(userId);
    }

    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received");
            server.stop();
        }));

        server.start();
    }
}
