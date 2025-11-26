package com.chatapp.server.service;

import com.chatapp.server.model.User;
import com.chatapp.server.model.UserStatus;
import com.chatapp.server.util.DatabaseManager;
import com.chatapp.server.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final DatabaseManager dbManager;

    public UserService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Register new user
     */
    public User registerUser(String username, String email, String password, String fullName) {
        String sql = "INSERT INTO users (username, email, password_hash, full_name) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            String hashedPassword = PasswordUtil.hashPassword(password);

            pstmt.setString(1, username);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, fullName);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        logger.info("User registered successfully: {}", username);
                        return getUserById(userId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error registering user: {}", username, e);
        }
        return null;
    }

    /**
     * Login user
     */
    public User loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                if (PasswordUtil.verifyPassword(password, storedHash)) {
                    User user = extractUserFromResultSet(rs);
                    updateLastLogin(user.getUserId());
                    updateUserStatus(user.getUserId(), UserStatus.ONLINE);
                    logger.info("User logged in: {}", username);
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Error logging in user: {}", username, e);
        }
        return null;
    }

    /**
     * Logout user
     */
    public boolean logoutUser(int userId) {
        return updateUserStatus(userId, UserStatus.OFFLINE);
    }

    /**
     * Get user by ID
     */
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("Error getting user by ID: {}", userId, e);
        }
        return null;
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("Error getting user by username: {}", username, e);
        }
        return null;
    }

    /**
     * Search users by keyword
     */
    public List<User> searchUsers(String keyword) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE username LIKE ? OR full_name LIKE ? OR email LIKE ? LIMIT 50";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error searching users with keyword: {}", keyword, e);
        }
        return users;
    }

    /**
     * Update user profile
     */
    public boolean updateProfile(int userId, String fullName, String statusMessage) {
        String sql = "UPDATE users SET full_name = ?, status_message = ? WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            pstmt.setString(2, statusMessage);
            pstmt.setInt(3, userId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Profile updated for user ID: {}", userId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error updating profile for user ID: {}", userId, e);
        }
        return false;
    }

    /**
     * Update user status (ONLINE, OFFLINE, AWAY, BUSY)
     */
    public boolean updateUserStatus(int userId, UserStatus status) {
        String sql = "UPDATE users SET user_status = ? WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name());
            pstmt.setInt(2, userId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("Status updated for user ID {}: {}", userId, status);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error updating status for user ID: {}", userId, e);
        }
        return false;
    }

    /**
     * Update last login timestamp
     */
    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating last login for user ID: {}", userId, e);
        }
    }

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking username existence: {}", username, e);
        }
        return false;
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking email existence: {}", email, e);
        }
        return false;
    }

    /**
     * Extract User object from ResultSet
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setStatusMessage(rs.getString("status_message"));
        user.setUserStatus(UserStatus.valueOf(rs.getString("user_status")));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setLastLogin(rs.getTimestamp("last_login"));
        return user;
    }
}
