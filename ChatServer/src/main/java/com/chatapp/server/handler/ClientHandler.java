package com.chatapp.server.handler;

import com.chatapp.server.ChatServer;
import com.chatapp.server.model.*;
import com.chatapp.server.service.*;
import com.chatapp.server.util.FileUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket clientSocket;
    private final ChatServer server;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;

    private final Gson gson;
    private final UserService userService;
    private final FriendService friendService;
    private final MessageService messageService;
    private final GroupService groupService;
    private final CallService callService;

    public ClientHandler(Socket socket, ChatServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.gson = new Gson();
        this.userService = new UserService();
        this.friendService = new FriendService();
        this.messageService = new MessageService();
        this.groupService = new GroupService();
        this.callService = new CallService();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            logger.info("Client connected: {}", clientSocket.getInetAddress());

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                logger.debug("Received: {}", inputLine);
                handleRequest(inputLine);
            }

        } catch (IOException e) {
            logger.error("Error handling client", e);
        } finally {
            cleanup();
        }
    }

    private void handleRequest(String requestJson) {
        try {
            Protocol request = Protocol.fromJson(requestJson);
            String action = request.getAction();
            JsonObject data = request.getData();

            switch (action) {
                case Protocol.ACTION_REGISTER:
                    handleRegister(data);
                    break;
                case Protocol.ACTION_LOGIN:
                    handleLogin(data);
                    break;
                case Protocol.ACTION_LOGOUT:
                    handleLogout();
                    break;
                case Protocol.ACTION_UPDATE_PROFILE:
                    handleUpdateProfile(data);
                    break;
                case Protocol.ACTION_UPDATE_STATUS:
                    handleUpdateStatus(data);
                    break;
                case Protocol.ACTION_SEARCH_USERS:
                    handleSearchUsers(data);
                    break;
                case Protocol.ACTION_SEND_FRIEND_REQUEST:
                    handleSendFriendRequest(data);
                    break;
                case Protocol.ACTION_ACCEPT_FRIEND_REQUEST:
                    handleAcceptFriendRequest(data);
                    break;
                case Protocol.ACTION_REJECT_FRIEND_REQUEST:
                    handleRejectFriendRequest(data);
                    break;
                case Protocol.ACTION_GET_FRIENDS:
                    handleGetFriends();
                    break;
                case Protocol.ACTION_GET_FRIEND_REQUESTS:
                    handleGetFriendRequests();
                    break;
                case Protocol.ACTION_GET_USER_PROFILE:
                    handleGetUserProfile(data);
                    break;
                case Protocol.ACTION_SEND_MESSAGE:
                    handleSendMessage(data);
                    break;
                case Protocol.ACTION_GET_MESSAGES:
                    handleGetMessages(data);
                    break;
                case Protocol.ACTION_SEND_FILE:
                    handleSendFile(data);
                    break;
                case Protocol.ACTION_RECEIVE_FILE:
                    handleReceiveFile(data);
                    break;
                case Protocol.ACTION_CREATE_GROUP:
                    handleCreateGroup(data);
                    break;
                case Protocol.ACTION_JOIN_GROUP:
                    handleJoinGroup(data);
                    break;
                case Protocol.ACTION_GET_GROUPS:
                    handleGetGroups();
                    break;
                case Protocol.ACTION_GET_GROUP_MEMBERS:
                    handleGetGroupMembers(data);
                    break;
                case Protocol.ACTION_SEND_GROUP_MESSAGE:
                    handleSendGroupMessage(data);
                    break;
                case Protocol.ACTION_INITIATE_CALL:
                    handleInitiateCall(data);
                    break;
                case Protocol.ACTION_ACCEPT_CALL:
                    handleAcceptCall(data);
                    break;
                case Protocol.ACTION_REJECT_CALL:
                    handleRejectCall(data);
                    break;
                case Protocol.ACTION_END_CALL:
                    handleEndCall(data);
                    break;
                case Protocol.ACTION_CALL_SIGNAL:
                    handleCallSignal(data);
                    break;
                default:
                    sendResponse(Protocol.createResponse(action, false, "Unknown action"));
            }
        } catch (Exception e) {
            logger.error("Error handling request", e);
            sendResponse(Protocol.createResponse("ERROR", false, "Internal server error"));
        }
    }

    private void handleRegister(JsonObject data) {
        String username = data.get("username").getAsString();
        String email = data.get("email").getAsString();
        String password = data.get("password").getAsString();
        String fullName = data.get("fullName").getAsString();

        if (userService.usernameExists(username)) {
            sendResponse(Protocol.createResponse(Protocol.ACTION_REGISTER, false, "Username already exists"));
            return;
        }

        if (userService.emailExists(email)) {
            sendResponse(Protocol.createResponse(Protocol.ACTION_REGISTER, false, "Email already exists"));
            return;
        }

        User user = userService.registerUser(username, email, password, fullName);
        if (user != null) {
            JsonObject responseData = new JsonObject();
            responseData.add("user", gson.toJsonTree(user));
            sendResponse(Protocol.createResponse(Protocol.ACTION_REGISTER, true, "Registration successful", responseData));
        } else {
            sendResponse(Protocol.createResponse(Protocol.ACTION_REGISTER, false, "Registration failed"));
        }
    }

    private void handleLogin(JsonObject data) {
        String username = data.get("username").getAsString();
        String password = data.get("password").getAsString();

        User user = userService.loginUser(username, password);
        if (user != null) {
            this.currentUser = user;
            server.addOnlineUser(user.getUserId(), this);

            JsonObject responseData = new JsonObject();
            responseData.add("user", gson.toJsonTree(user));
            sendResponse(Protocol.createResponse(Protocol.ACTION_LOGIN, true, "Login successful", responseData));

            // Notify friends that user is online
            notifyFriendsOnlineStatus(true);
        } else {
            sendResponse(Protocol.createResponse(Protocol.ACTION_LOGIN, false, "Invalid credentials"));
        }
    }

    private void handleLogout() {
        if (currentUser != null) {
            userService.logoutUser(currentUser.getUserId());
            server.removeOnlineUser(currentUser.getUserId());
            notifyFriendsOnlineStatus(false);
            sendResponse(Protocol.createResponse(Protocol.ACTION_LOGOUT, true, "Logout successful"));
        }
    }

    private void handleUpdateProfile(JsonObject data) {
        if (currentUser == null) {
            sendResponse(Protocol.createResponse(Protocol.ACTION_UPDATE_PROFILE, false, "Not logged in"));
            return;
        }

        String fullName = data.get("fullName").getAsString();
        String statusMessage = data.get("statusMessage").getAsString();

        boolean success = userService.updateProfile(currentUser.getUserId(), fullName, statusMessage);
        sendResponse(Protocol.createResponse(Protocol.ACTION_UPDATE_PROFILE, success,
                success ? "Profile updated" : "Update failed"));
    }

    private void handleUpdateStatus(JsonObject data) {
        if (currentUser == null) return;

        UserStatus status = UserStatus.valueOf(data.get("status").getAsString());
        boolean success = userService.updateUserStatus(currentUser.getUserId(), status);

        if (success) {
            currentUser.setUserStatus(status);
            notifyFriendsStatusChange(status);
        }

        sendResponse(Protocol.createResponse(Protocol.ACTION_UPDATE_STATUS, success,
                success ? "Status updated" : "Update failed"));
    }

    private void handleSearchUsers(JsonObject data) {
        String keyword = data.get("keyword").getAsString();
        List<User> users = userService.searchUsers(keyword);

        JsonObject responseData = new JsonObject();
        responseData.add("users", gson.toJsonTree(users));
        sendResponse(Protocol.createResponse(Protocol.ACTION_SEARCH_USERS, true, "Search completed", responseData));
    }

    private void handleSendFriendRequest(JsonObject data) {
        if (currentUser == null) return;

        int receiverId = data.get("receiverId").getAsInt();
        logger.info("User {} sending friend request to user {}", currentUser.getUserId(), receiverId);

        FriendRequest request = friendService.sendFriendRequest(currentUser.getUserId(), receiverId);

        if (request != null) {
            JsonObject responseData = new JsonObject();
            responseData.add("request", gson.toJsonTree(request));
            sendResponse(Protocol.createResponse(Protocol.ACTION_SEND_FRIEND_REQUEST, true, "Friend request sent", responseData));

            // Notify receiver
            notifyUser(receiverId, Protocol.NOTIFY_FRIEND_REQUEST, request);
            logger.info("Friend request {} created and notification sent", request.getRequestId());
        } else {
            logger.warn("Failed to send friend request from {} to {}", currentUser.getUserId(), receiverId);
            sendResponse(Protocol.createResponse(Protocol.ACTION_SEND_FRIEND_REQUEST, false, "Failed to send request. User may already be a friend or have a pending request."));
        }
    }

    private void handleAcceptFriendRequest(JsonObject data) {
        int requestId = data.get("requestId").getAsInt();
        logger.info("User {} accepting friend request {}", currentUser != null ? currentUser.getUserId() : "null", requestId);

        boolean success = friendService.acceptFriendRequest(requestId);

        if (success) {
            logger.info("Friend request {} accepted successfully", requestId);
        } else {
            logger.warn("Failed to accept friend request {}", requestId);
        }

        sendResponse(Protocol.createResponse(Protocol.ACTION_ACCEPT_FRIEND_REQUEST, success,
                success ? "Friend request accepted" : "Failed to accept request"));
    }

    private void handleRejectFriendRequest(JsonObject data) {
        int requestId = data.get("requestId").getAsInt();
        logger.info("User {} rejecting friend request {}", currentUser != null ? currentUser.getUserId() : "null", requestId);

        boolean success = friendService.rejectFriendRequest(requestId);

        sendResponse(Protocol.createResponse(Protocol.ACTION_REJECT_FRIEND_REQUEST, success,
                success ? "Friend request rejected" : "Failed to reject"));
    }

    private void handleGetFriends() {
        if (currentUser == null) return;

        List<User> friends = friendService.getFriends(currentUser.getUserId());
        JsonObject responseData = new JsonObject();
        responseData.add("friends", gson.toJsonTree(friends));
        sendResponse(Protocol.createResponse(Protocol.ACTION_GET_FRIENDS, true, "Friends retrieved", responseData));
    }

    private void handleGetFriendRequests() {
        if (currentUser == null) return;

        List<FriendRequest> requests = friendService.getFriendRequests(currentUser.getUserId());
        JsonObject responseData = new JsonObject();
        responseData.add("requests", gson.toJsonTree(requests));
        sendResponse(Protocol.createResponse(Protocol.ACTION_GET_FRIEND_REQUESTS, true, "Requests retrieved", responseData));
    }

    private void handleGetUserProfile(JsonObject data) {
        int userId = data.get("userId").getAsInt();
        User user = userService.getUserById(userId);

        if (user != null) {
            JsonObject responseData = new JsonObject();
            responseData.add("user", gson.toJsonTree(user));
            sendResponse(Protocol.createResponse(Protocol.ACTION_GET_USER_PROFILE, true, "Profile retrieved", responseData));
        } else {
            sendResponse(Protocol.createResponse(Protocol.ACTION_GET_USER_PROFILE, false, "User not found"));
        }
    }

    private void handleSendMessage(JsonObject data) {
        if (currentUser == null) return;

        int receiverId = data.get("receiverId").getAsInt();
        String content = data.get("content").getAsString();
        MessageType type = MessageType.valueOf(data.get("type").getAsString());

        Message message = messageService.sendPrivateMessage(currentUser.getUserId(), receiverId,
                type, content, null, null, null);

        if (message != null) {
            JsonObject responseData = new JsonObject();
            responseData.add("message", gson.toJsonTree(message));
            sendResponse(Protocol.createResponse(Protocol.ACTION_SEND_MESSAGE, true, "Message sent", responseData));

            // Notify receiver
            notifyUser(receiverId, Protocol.NOTIFY_NEW_MESSAGE, message);
        } else {
            sendResponse(Protocol.createResponse(Protocol.ACTION_SEND_MESSAGE, false, "Failed to send message"));
        }
    }

    private void handleGetMessages(JsonObject data) {
        if (currentUser == null) return;

        int limit = data.has("limit") ? data.get("limit").getAsInt() : 50;
        List<Message> messages;

        // Check if it's for group or private chat
        if (data.has("groupId")) {
            // Get group messages
            int groupId = data.get("groupId").getAsInt();
            logger.info("Getting group messages for group {} (user: {})", groupId, currentUser.getUserId());
            messages = messageService.getGroupMessages(groupId, limit);
            logger.info("Retrieved {} group messages for group {}", messages.size(), groupId);
        } else {
            // Get private messages
            int otherUserId = data.get("userId").getAsInt();
            logger.info("Getting private messages between {} and {}", currentUser.getUserId(), otherUserId);
            messages = messageService.getPrivateMessages(currentUser.getUserId(), otherUserId, limit);
            logger.info("Retrieved {} private messages", messages.size());
        }

        JsonObject responseData = new JsonObject();
        responseData.add("messages", gson.toJsonTree(messages));
        sendResponse(Protocol.createResponse(Protocol.ACTION_GET_MESSAGES, true, "Messages retrieved", responseData));
    }

    private void handleSendFile(JsonObject data) {
        if (currentUser == null) return;

        int receiverId = data.get("receiverId").getAsInt();
        String fileName = data.get("fileName").getAsString();
        String fileData = data.get("fileData").getAsString();
        String fileType = data.get("fileType").getAsString();

        String filePath = FileUtil.saveFile(fileData, fileName, fileType);

        if (filePath != null) {
            long fileSize = FileUtil.getFileSize(filePath);
            Message message = messageService.sendPrivateMessage(currentUser.getUserId(), receiverId,
                    MessageType.valueOf(fileType), "File: " + fileName, filePath, fileName, fileSize);

            if (message != null) {
                JsonObject responseData = new JsonObject();
                responseData.add("message", gson.toJsonTree(message));
                sendResponse(Protocol.createResponse(Protocol.ACTION_SEND_FILE, true, "File sent", responseData));

                notifyUser(receiverId, Protocol.NOTIFY_NEW_MESSAGE, message);
            }
        } else {
            sendResponse(Protocol.createResponse(Protocol.ACTION_SEND_FILE, false, "Failed to save file"));
        }
    }

    private void handleReceiveFile(JsonObject data) {
        String filePath = data.get("filePath").getAsString();

        String fileData = FileUtil.readFileAsBase64(filePath);

        if (fileData != null) {
            JsonObject responseData = new JsonObject();
            responseData.addProperty("fileData", fileData);
            sendResponse(Protocol.createResponse(Protocol.ACTION_RECEIVE_FILE, true, "File retrieved", responseData));
        } else {
            sendResponse(Protocol.createResponse(Protocol.ACTION_RECEIVE_FILE, false, "Failed to read file"));
        }
    }

    private void handleCreateGroup(JsonObject data) {
        if (currentUser == null) return;

        String groupName = data.get("groupName").getAsString();
        String groupDescription = data.get("groupDescription").getAsString();

        Group group = groupService.createGroup(groupName, groupDescription, currentUser.getUserId());

        if (group != null) {
            JsonObject responseData = new JsonObject();
            responseData.add("group", gson.toJsonTree(group));
            sendResponse(Protocol.createResponse(Protocol.ACTION_CREATE_GROUP, true, "Group created", responseData));
        } else {
            sendResponse(Protocol.createResponse(Protocol.ACTION_CREATE_GROUP, false, "Failed to create group"));
        }
    }

    private void handleJoinGroup(JsonObject data) {
        if (currentUser == null) return;

        int groupId = data.get("groupId").getAsInt();
        // Check if userId is specified (for adding other users) or use current user
        int userId = data.has("userId") ? data.get("userId").getAsInt() : currentUser.getUserId();
        boolean success = groupService.addMember(groupId, userId);

        sendResponse(Protocol.createResponse(Protocol.ACTION_JOIN_GROUP, success,
                success ? "Joined group" : "Failed to join"));
    }

    private void handleGetGroups() {
        if (currentUser == null) return;

        List<Group> groups = groupService.getUserGroups(currentUser.getUserId());
        JsonObject responseData = new JsonObject();
        responseData.add("groups", gson.toJsonTree(groups));
        sendResponse(Protocol.createResponse(Protocol.ACTION_GET_GROUPS, true, "Groups retrieved", responseData));
    }

    private void handleGetGroupMembers(JsonObject data) {
        int groupId = data.get("groupId").getAsInt();
        List<User> members = groupService.getGroupMembers(groupId);

        JsonObject responseData = new JsonObject();
        responseData.add("members", gson.toJsonTree(members));
        sendResponse(Protocol.createResponse(Protocol.ACTION_GET_GROUP_MEMBERS, true, "Members retrieved", responseData));
    }

    private void handleSendGroupMessage(JsonObject data) {
        if (currentUser == null) return;

        int groupId = data.get("groupId").getAsInt();
        String content = data.get("content").getAsString();
        MessageType type = MessageType.valueOf(data.get("type").getAsString());

        Message message = messageService.sendGroupMessage(currentUser.getUserId(), groupId,
                type, content, null, null, null);

        if (message != null) {
            JsonObject responseData = new JsonObject();
            responseData.add("message", gson.toJsonTree(message));
            sendResponse(Protocol.createResponse(Protocol.ACTION_SEND_GROUP_MESSAGE, true, "Message sent", responseData));

            // Notify group members
            notifyGroupMembers(groupId, Protocol.NOTIFY_NEW_MESSAGE, message);
        } else {
            sendResponse(Protocol.createResponse(Protocol.ACTION_SEND_GROUP_MESSAGE, false, "Failed to send message"));
        }
    }

    private void handleInitiateCall(JsonObject data) {
        if (currentUser == null) return;

        int receiverId = data.get("receiverId").getAsInt();
        CallInfo.CallType callType = CallInfo.CallType.valueOf(data.get("callType").getAsString());

        CallInfo call = callService.initiateCall(currentUser.getUserId(), receiverId, callType);

        if (call != null) {
            JsonObject responseData = new JsonObject();
            responseData.add("call", gson.toJsonTree(call));
            sendResponse(Protocol.createResponse(Protocol.ACTION_INITIATE_CALL, true, "Call initiated", responseData));

            notifyUser(receiverId, Protocol.NOTIFY_INCOMING_CALL, call);
        } else {
            sendResponse(Protocol.createResponse(Protocol.ACTION_INITIATE_CALL, false, "Failed to initiate call"));
        }
    }

    private void handleAcceptCall(JsonObject data) {
        int callId = data.get("callId").getAsInt();

        // Get call info to know who to notify
        CallInfo call = callService.getCallById(callId);

        boolean success = callService.acceptCall(callId);

        sendResponse(Protocol.createResponse(Protocol.ACTION_ACCEPT_CALL, success,
                success ? "Call accepted" : "Failed to accept"));

        // Notify caller that call was accepted
        if (success && call != null) {
            notifyUser(call.getCallerId(), Protocol.NOTIFY_CALL_ACCEPTED, call);
            logger.info("Notified caller {} that call {} was accepted", call.getCallerId(), callId);
        }
    }

    private void handleRejectCall(JsonObject data) {
        int callId = data.get("callId").getAsInt();

        // Get call info to know who to notify
        CallInfo call = callService.getCallById(callId);

        boolean success = callService.rejectCall(callId);

        sendResponse(Protocol.createResponse(Protocol.ACTION_REJECT_CALL, success,
                success ? "Call rejected" : "Failed to reject"));

        // Notify caller that call was rejected
        if (success && call != null) {
            notifyUser(call.getCallerId(), Protocol.NOTIFY_CALL_REJECTED, call);
            logger.info("Notified caller {} that call {} was rejected", call.getCallerId(), callId);
        }
    }

    private void handleEndCall(JsonObject data) {
        int callId = data.get("callId").getAsInt();

        // Get call info to know who to notify
        CallInfo call = callService.getCallById(callId);

        boolean success = callService.endCall(callId);

        sendResponse(Protocol.createResponse(Protocol.ACTION_END_CALL, success,
                success ? "Call ended" : "Failed to end"));

        // Notify the other person that call ended
        if (success && call != null && currentUser != null) {
            int otherUserId;
            if (currentUser.getUserId() == call.getCallerId()) {
                // Current user is caller, notify receiver
                otherUserId = call.getReceiverId();
            } else {
                // Current user is receiver, notify caller
                otherUserId = call.getCallerId();
            }
            notifyUser(otherUserId, Protocol.NOTIFY_CALL_ENDED, call);
            logger.info("Notified user {} that call {} ended", otherUserId, callId);
        }
    }

    private void handleCallSignal(JsonObject data) {
        // Forward media/signaling data to the other peer
        if (!data.has("receiverId")) {
            logger.error("CALL_SIGNAL missing receiverId field");
            return;
        }

        int receiverId = data.get("receiverId").getAsInt();

        // Extract signal type (VIDEO_FRAME, AUDIO_CHUNK, etc.)
        String signalType = data.has("type") ? data.get("type").getAsString() : Protocol.ACTION_CALL_SIGNAL;

        // Check if receiver is online
        ClientHandler receiver = server.getOnlineUser(receiverId);
        if (receiver == null) {
            logger.warn("Cannot forward {} signal to user {} - user offline", signalType, receiverId);
            return;
        }

        // Forward with appropriate notification type
        notifyUser(receiverId, signalType, data);

        // Log periodically to avoid spam (every 30th frame/chunk)
        if (signalType.equals("VIDEO_FRAME") || signalType.equals("AUDIO_CHUNK")) {
            // Only log occasionally
            if (Math.random() < 0.03) { // ~3% of messages
                logger.debug("Forwarding {} from user {} to user {}", signalType,
                    currentUser != null ? currentUser.getUserId() : "unknown", receiverId);
            }
        } else {
            logger.info("Forwarded {} signal from {} to {}", signalType,
                currentUser != null ? currentUser.getUserId() : "unknown", receiverId);
        }
    }

    private void notifyUser(int userId, String notificationType, Object data) {
        ClientHandler handler = server.getOnlineUser(userId);
        if (handler != null) {
            Protocol notification = new Protocol(notificationType);
            notification.addData("data", data);
            handler.sendResponse(notification);
        }
    }

    private void notifyGroupMembers(int groupId, String notificationType, Object data) {
        List<User> members = groupService.getGroupMembers(groupId);
        for (User member : members) {
            if (currentUser != null && member.getUserId() != currentUser.getUserId()) {
                notifyUser(member.getUserId(), notificationType, data);
            }
        }
    }

    private void notifyFriendsOnlineStatus(boolean online) {
        if (currentUser == null) return;

        List<User> friends = friendService.getFriends(currentUser.getUserId());
        String notification = online ? Protocol.NOTIFY_USER_ONLINE : Protocol.NOTIFY_USER_OFFLINE;

        for (User friend : friends) {
            notifyUser(friend.getUserId(), notification, currentUser);
        }
    }

    private void notifyFriendsStatusChange(UserStatus status) {
        if (currentUser == null) return;

        List<User> friends = friendService.getFriends(currentUser.getUserId());
        for (User friend : friends) {
            JsonObject data = new JsonObject();
            data.addProperty("userId", currentUser.getUserId());
            data.addProperty("status", status.name());
            notifyUser(friend.getUserId(), "STATUS_CHANGE", data);
        }
    }

    public void sendResponse(Protocol response) {
        if (out != null) {
            out.println(response.toJson());
        }
    }

    private void cleanup() {
        if (currentUser != null) {
            userService.logoutUser(currentUser.getUserId());
            server.removeOnlineUser(currentUser.getUserId());
            notifyFriendsOnlineStatus(false);
        }

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            logger.info("Client disconnected");
        } catch (IOException e) {
            logger.error("Error closing client connection", e);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}
