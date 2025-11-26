package com.chatapp.server.service;

import com.chatapp.server.model.FriendRequest;
import com.chatapp.server.model.User;
import com.chatapp.server.model.UserStatus;
import com.chatapp.server.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FriendService {
    private static final Logger logger = LoggerFactory.getLogger(FriendService.class);
    private final DatabaseManager dbManager;
    private final UserService userService;

    public FriendService() {
        this.dbManager = DatabaseManager.getInstance();
        this.userService = new UserService();
    }

    /**
     * Send friend request
     */
    public FriendRequest sendFriendRequest(int senderId, int receiverId) {
        // Kiểm tra xem đã là bạn bè chưa
        if (areFriends(senderId, receiverId)) {
            logger.warn("Users {} and {} are already friends", senderId, receiverId);
            return null;
        }

        // Kiểm tra xem đã có request chưa
        if (hasPendingRequest(senderId, receiverId)) {
            logger.warn("Friend request already exists between {} and {}", senderId, receiverId);
            return null;
        }

        String sql = "INSERT INTO friend_requests (sender_id, receiver_id, request_status) VALUES (?, ?, 'PENDING')";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int requestId = rs.getInt(1);
                        logger.info("Friend request sent from {} to {}", senderId, receiverId);
                        return getFriendRequestById(requestId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error sending friend request from {} to {}", senderId, receiverId, e);
        }
        return null;
    }

    /**
     * Accept friend request
     */
    public boolean acceptFriendRequest(int requestId) {
        FriendRequest request = getFriendRequestById(requestId);
        if (request == null || request.getRequestStatus() != FriendRequest.RequestStatus.PENDING) {
            return false;
        }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // Update request status
            String updateSql = "UPDATE friend_requests SET request_status = 'ACCEPTED' WHERE request_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setInt(1, requestId);
                pstmt.executeUpdate();
            }

            // Add to friends table (both directions)
            String insertSql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setInt(1, request.getSenderId());
                pstmt.setInt(2, request.getReceiverId());
                pstmt.executeUpdate();

                pstmt.setInt(1, request.getReceiverId());
                pstmt.setInt(2, request.getSenderId());
                pstmt.executeUpdate();
            }

            conn.commit();
            logger.info("Friend request {} accepted", requestId);
            return true;

        } catch (SQLException e) {
            logger.error("Error accepting friend request {}", requestId, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Error resetting auto-commit", e);
                }
            }
        }
    }

    /**
     * Reject friend request
     */
    public boolean rejectFriendRequest(int requestId) {
        String sql = "UPDATE friend_requests SET request_status = 'REJECTED' WHERE request_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, requestId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Friend request {} rejected", requestId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error rejecting friend request {}", requestId, e);
        }
        return false;
    }

    /**
     * Get friend requests for a user
     */
    public List<FriendRequest> getFriendRequests(int userId) {
        List<FriendRequest> requests = new ArrayList<>();
        String sql = "SELECT fr.*, " +
                     "s.username as sender_username, s.full_name as sender_fullname, " +
                     "r.username as receiver_username, r.full_name as receiver_fullname " +
                     "FROM friend_requests fr " +
                     "JOIN users s ON fr.sender_id = s.user_id " +
                     "JOIN users r ON fr.receiver_id = r.user_id " +
                     "WHERE fr.receiver_id = ? AND fr.request_status = 'PENDING' " +
                     "ORDER BY fr.created_at DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                requests.add(extractFriendRequestFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting friend requests for user {}", userId, e);
        }
        return requests;
    }

    /**
     * Get friends list
     */
    public List<User> getFriends(int userId) {
        List<User> friends = new ArrayList<>();
        String sql = "SELECT u.* FROM users u " +
                     "JOIN friends f ON u.user_id = f.friend_id " +
                     "WHERE f.user_id = ? " +
                     "ORDER BY u.user_status DESC, u.full_name";

        logger.info("Getting friends list for user {}", userId);

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                try {
                    // Extract user directly from ResultSet instead of calling getUserById
                    User friend = new User();
                    friend.setUserId(rs.getInt("user_id"));
                    friend.setUsername(rs.getString("username"));
                    friend.setEmail(rs.getString("email"));
                    friend.setFullName(rs.getString("full_name"));
                    friend.setUserStatus(UserStatus.valueOf(rs.getString("user_status")));
                    friend.setStatusMessage(rs.getString("status_message"));
                    friend.setCreatedAt(rs.getTimestamp("created_at"));
                    friend.setLastLogin(rs.getTimestamp("last_login"));

                    friends.add(friend);
                    count++;
                    logger.debug("Added friend: {} ({})", friend.getFullName(), friend.getUserId());
                } catch (Exception e) {
                    logger.error("Error extracting friend data from result set", e);
                }
            }

            logger.info("Successfully retrieved {} friends for user {}", count, userId);

            if (count == 0) {
                logger.warn("No friends found for user {}. Check friends table in database.", userId);
            }

        } catch (SQLException e) {
            logger.error("Error getting friends for user {}", userId, e);
            logger.error("SQL: {}", sql);
        }
        return friends;
    }

    /**
     * Check if two users are friends
     */
    public boolean areFriends(int userId1, int userId2) {
        String sql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking friendship between {} and {}", userId1, userId2, e);
        }
        return false;
    }

    /**
     * Check if there's a pending request
     */
    private boolean hasPendingRequest(int senderId, int receiverId) {
        String sql = "SELECT COUNT(*) FROM friend_requests " +
                     "WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) " +
                     "AND request_status = 'PENDING'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.setInt(3, receiverId);
            pstmt.setInt(4, senderId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking pending request between {} and {}", senderId, receiverId, e);
        }
        return false;
    }

    /**
     * Get friend request by ID
     */
    private FriendRequest getFriendRequestById(int requestId) {
        String sql = "SELECT fr.*, " +
                     "s.username as sender_username, s.full_name as sender_fullname, " +
                     "r.username as receiver_username, r.full_name as receiver_fullname " +
                     "FROM friend_requests fr " +
                     "JOIN users s ON fr.sender_id = s.user_id " +
                     "JOIN users r ON fr.receiver_id = r.user_id " +
                     "WHERE fr.request_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, requestId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractFriendRequestFromResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("Error getting friend request by ID {}", requestId, e);
        }
        return null;
    }

    /**
     * Extract FriendRequest from ResultSet
     */
    private FriendRequest extractFriendRequestFromResultSet(ResultSet rs) throws SQLException {
        FriendRequest request = new FriendRequest();
        request.setRequestId(rs.getInt("request_id"));
        request.setSenderId(rs.getInt("sender_id"));
        request.setReceiverId(rs.getInt("receiver_id"));
        request.setRequestStatus(FriendRequest.RequestStatus.valueOf(rs.getString("request_status")));
        request.setCreatedAt(rs.getTimestamp("created_at"));
        request.setUpdatedAt(rs.getTimestamp("updated_at"));
        request.setSenderUsername(rs.getString("sender_username"));
        request.setSenderFullName(rs.getString("sender_fullname"));
        request.setReceiverUsername(rs.getString("receiver_username"));
        request.setReceiverFullName(rs.getString("receiver_fullname"));
        return request;
    }
}
