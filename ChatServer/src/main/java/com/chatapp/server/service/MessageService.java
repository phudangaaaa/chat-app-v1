package com.chatapp.server.service;

import com.chatapp.server.model.Message;
import com.chatapp.server.model.MessageType;
import com.chatapp.server.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private final DatabaseManager dbManager;

    public MessageService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Send private message
     */
    public Message sendPrivateMessage(int senderId, int receiverId, MessageType messageType,
                                     String content, String fileUrl, String fileName, Long fileSize) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message_type, message_content, " +
                     "file_url, file_name, file_size) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.setString(3, messageType.name());
            pstmt.setString(4, content);
            pstmt.setString(5, fileUrl);
            pstmt.setString(6, fileName);
            if (fileSize != null) {
                pstmt.setLong(7, fileSize);
            } else {
                pstmt.setNull(7, Types.BIGINT);
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int messageId = rs.getInt(1);
                        logger.info("Private message sent from {} to {}", senderId, receiverId);
                        return getMessageById(messageId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error sending private message from {} to {}", senderId, receiverId, e);
        }
        return null;
    }

    /**
     * Send group message
     */
    public Message sendGroupMessage(int senderId, int groupId, MessageType messageType,
                                   String content, String fileUrl, String fileName, Long fileSize) {
        String sql = "INSERT INTO messages (sender_id, group_id, message_type, message_content, " +
                     "file_url, file_name, file_size) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, senderId);
            pstmt.setInt(2, groupId);
            pstmt.setString(3, messageType.name());
            pstmt.setString(4, content);
            pstmt.setString(5, fileUrl);
            pstmt.setString(6, fileName);
            if (fileSize != null) {
                pstmt.setLong(7, fileSize);
            } else {
                pstmt.setNull(7, Types.BIGINT);
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int messageId = rs.getInt(1);
                        logger.info("Group message sent from {} to group {}", senderId, groupId);
                        return getMessageById(messageId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error sending group message from {} to group {}", senderId, groupId, e);
        }
        return null;
    }

    /**
     * Get private messages between two users
     */
    public List<Message> getPrivateMessages(int userId1, int userId2, int limit) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, " +
                     "s.username as sender_name, r.username as receiver_name " +
                     "FROM messages m " +
                     "JOIN users s ON m.sender_id = s.user_id " +
                     "JOIN users r ON m.receiver_id = r.user_id " +
                     "WHERE ((m.sender_id = ? AND m.receiver_id = ?) OR " +
                     "       (m.sender_id = ? AND m.receiver_id = ?)) " +
                     "ORDER BY m.sent_at DESC LIMIT ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            pstmt.setInt(3, userId2);
            pstmt.setInt(4, userId1);
            pstmt.setInt(5, limit);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                messages.add(extractMessageFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting private messages between {} and {}", userId1, userId2, e);
        }
        return messages;
    }

    /**
     * Get group messages
     */
    public List<Message> getGroupMessages(int groupId, int limit) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, s.username as sender_name " +
                     "FROM messages m " +
                     "JOIN users s ON m.sender_id = s.user_id " +
                     "WHERE m.group_id = ? " +
                     "ORDER BY m.sent_at DESC LIMIT ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, groupId);
            pstmt.setInt(2, limit);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                messages.add(extractMessageFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting group messages for group {}", groupId, e);
        }
        return messages;
    }

    /**
     * Mark message as read
     */
    public boolean markMessageAsRead(int messageId) {
        String sql = "UPDATE messages SET is_read = TRUE WHERE message_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, messageId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.debug("Message {} marked as read", messageId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error marking message {} as read", messageId, e);
        }
        return false;
    }

    /**
     * Get unread message count
     */
    public int getUnreadMessageCount(int userId) {
        String sql = "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = FALSE";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting unread message count for user {}", userId, e);
        }
        return 0;
    }

    /**
     * Get message by ID
     */
    private Message getMessageById(int messageId) {
        String sql = "SELECT m.*, " +
                     "s.username as sender_name, r.username as receiver_name " +
                     "FROM messages m " +
                     "JOIN users s ON m.sender_id = s.user_id " +
                     "LEFT JOIN users r ON m.receiver_id = r.user_id " +
                     "WHERE m.message_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, messageId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractMessageFromResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("Error getting message by ID {}", messageId, e);
        }
        return null;
    }

    /**
     * Extract Message from ResultSet
     */
    private Message extractMessageFromResultSet(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setMessageId(rs.getInt("message_id"));
        message.setSenderId(rs.getInt("sender_id"));

        int receiverId = rs.getInt("receiver_id");
        if (!rs.wasNull()) {
            message.setReceiverId(receiverId);
        }

        int groupId = rs.getInt("group_id");
        if (!rs.wasNull()) {
            message.setGroupId(groupId);
        }

        message.setMessageType(MessageType.valueOf(rs.getString("message_type")));
        message.setMessageContent(rs.getString("message_content"));
        message.setFileUrl(rs.getString("file_url"));
        message.setFileName(rs.getString("file_name"));

        long fileSize = rs.getLong("file_size");
        if (!rs.wasNull()) {
            message.setFileSize(fileSize);
        }

        message.setRead(rs.getBoolean("is_read"));
        message.setSentAt(rs.getTimestamp("sent_at"));
        message.setSenderName(rs.getString("sender_name"));

        String receiverName = rs.getString("receiver_name");
        if (receiverName != null) {
            message.setReceiverName(receiverName);
        }

        return message;
    }
}
