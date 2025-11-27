package com.chatapp.client.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Protocol để giao tiếp giữa Client và Server
 * Format: JSON
 * {
 *   "action": "LOGIN|REGISTER|SEND_MESSAGE|...",
 *   "data": {...}
 * }
 */
public class Protocol {
    private String action;
    private JsonObject data;
    private boolean success;
    private String message;

    private static final Gson gson = new Gson();

    // Actions
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_UPDATE_PROFILE = "UPDATE_PROFILE";
    public static final String ACTION_UPDATE_STATUS = "UPDATE_STATUS";

    public static final String ACTION_SEARCH_USERS = "SEARCH_USERS";
    public static final String ACTION_SEND_FRIEND_REQUEST = "SEND_FRIEND_REQUEST";
    public static final String ACTION_ACCEPT_FRIEND_REQUEST = "ACCEPT_FRIEND_REQUEST";
    public static final String ACTION_REJECT_FRIEND_REQUEST = "REJECT_FRIEND_REQUEST";
    public static final String ACTION_GET_FRIENDS = "GET_FRIENDS";
    public static final String ACTION_GET_FRIEND_REQUESTS = "GET_FRIEND_REQUESTS";
    public static final String ACTION_GET_USER_PROFILE = "GET_USER_PROFILE";

    public static final String ACTION_SEND_MESSAGE = "SEND_MESSAGE";
    public static final String ACTION_GET_MESSAGES = "GET_MESSAGES";
    public static final String ACTION_SEND_FILE = "SEND_FILE";
    public static final String ACTION_RECEIVE_FILE = "RECEIVE_FILE";

    public static final String ACTION_CREATE_GROUP = "CREATE_GROUP";
    public static final String ACTION_JOIN_GROUP = "JOIN_GROUP";
    public static final String ACTION_GET_GROUPS = "GET_GROUPS";
    public static final String ACTION_GET_GROUP_MEMBERS = "GET_GROUP_MEMBERS";
    public static final String ACTION_SEND_GROUP_MESSAGE = "SEND_GROUP_MESSAGE";

    public static final String ACTION_INITIATE_CALL = "INITIATE_CALL";
    public static final String ACTION_ACCEPT_CALL = "ACCEPT_CALL";
    public static final String ACTION_REJECT_CALL = "REJECT_CALL";
    public static final String ACTION_END_CALL = "END_CALL";
    public static final String ACTION_CALL_SIGNAL = "CALL_SIGNAL";

    // Notifications
    public static final String NOTIFY_USER_ONLINE = "NOTIFY_USER_ONLINE";
    public static final String NOTIFY_USER_OFFLINE = "NOTIFY_USER_OFFLINE";
    public static final String NOTIFY_NEW_MESSAGE = "NOTIFY_NEW_MESSAGE";
    public static final String NOTIFY_FRIEND_REQUEST = "NOTIFY_FRIEND_REQUEST";
    public static final String NOTIFY_INCOMING_CALL = "NOTIFY_INCOMING_CALL";
    public static final String NOTIFY_CALL_ACCEPTED = "NOTIFY_CALL_ACCEPTED";
    public static final String NOTIFY_CALL_REJECTED = "NOTIFY_CALL_REJECTED";
    public static final String NOTIFY_CALL_ENDED = "NOTIFY_CALL_ENDED";

    public Protocol() {
        this.data = new JsonObject();
    }

    public Protocol(String action) {
        this.action = action;
        this.data = new JsonObject();
    }

    public Protocol(String action, JsonObject data) {
        this.action = action;
        this.data = data;
    }

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public JsonObject getData() {
        return data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void addData(String key, Object value) {
        this.data.add(key, gson.toJsonTree(value));
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static Protocol fromJson(String json) {
        return gson.fromJson(json, Protocol.class);
    }

    public static Protocol createResponse(String action, boolean success, String message) {
        Protocol protocol = new Protocol(action);
        protocol.setSuccess(success);
        protocol.setMessage(message);
        return protocol;
    }

    public static Protocol createResponse(String action, boolean success, String message, JsonObject data) {
        Protocol protocol = new Protocol(action, data);
        protocol.setSuccess(success);
        protocol.setMessage(message);
        return protocol;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
