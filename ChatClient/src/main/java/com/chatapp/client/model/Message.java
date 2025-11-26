package com.chatapp.client.model;

import java.sql.Timestamp;

public class Message {
    private int messageId;
    private int senderId;
    private Integer receiverId;  // null nếu là group message
    private Integer groupId;     // null nếu là private message
    private MessageType messageType;
    private String messageContent;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private boolean isRead;
    private Timestamp sentAt;

    // Thông tin bổ sung (không lưu trong DB)
    private String senderName;
    private String receiverName;

    public Message() {
        this.messageType = MessageType.TEXT;
        this.isRead = false;
    }

    public Message(int senderId, Integer receiverId, Integer groupId,
                   MessageType messageType, String messageContent) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.groupId = groupId;
        this.messageType = messageType;
        this.messageContent = messageContent;
        this.isRead = false;
    }

    // Getters and Setters
    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public Integer getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Integer receiverId) {
        this.receiverId = receiverId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public void setSentAt(Timestamp sentAt) {
        this.sentAt = sentAt;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public boolean isGroupMessage() {
        return groupId != null;
    }

    public boolean isPrivateMessage() {
        return receiverId != null;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId=" + messageId +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", groupId=" + groupId +
                ", messageType=" + messageType +
                ", messageContent='" + messageContent + '\'' +
                ", sentAt=" + sentAt +
                '}';
    }
}
