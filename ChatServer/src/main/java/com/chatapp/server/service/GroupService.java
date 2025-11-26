package com.chatapp.server.service;

import com.chatapp.server.model.Group;
import com.chatapp.server.model.User;
import com.chatapp.server.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupService {
    private static final Logger logger = LoggerFactory.getLogger(GroupService.class);
    private final DatabaseManager dbManager;
    private final UserService userService;

    public GroupService() {
        this.dbManager = DatabaseManager.getInstance();
        this.userService = new UserService();
    }

    /**
     * Create new group
     */
    public Group createGroup(String groupName, String groupDescription, int creatorId) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            // Insert group
            String groupSql = "INSERT INTO chat_groups (group_name, group_description, creator_id) VALUES (?, ?, ?)";
            int groupId;

            try (PreparedStatement pstmt = conn.prepareStatement(groupSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, groupName);
                pstmt.setString(2, groupDescription);
                pstmt.setInt(3, creatorId);
                pstmt.executeUpdate();

                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    groupId = rs.getInt(1);
                } else {
                    throw new SQLException("Failed to create group");
                }
            }

            // Add creator as admin member
            String memberSql = "INSERT INTO group_members (group_id, user_id, member_role) VALUES (?, ?, 'ADMIN')";
            try (PreparedStatement pstmt = conn.prepareStatement(memberSql)) {
                pstmt.setInt(1, groupId);
                pstmt.setInt(2, creatorId);
                pstmt.executeUpdate();
            }

            conn.commit();
            logger.info("Group created: {} by user {}", groupName, creatorId);
            return getGroupById(groupId);

        } catch (SQLException e) {
            logger.error("Error creating group: {}", groupName, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            return null;
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
     * Add member to group
     */
    public boolean addMember(int groupId, int userId) {
        String sql = "INSERT INTO group_members (group_id, user_id, member_role) VALUES (?, ?, 'MEMBER')";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("User {} added to group {}", userId, groupId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error adding user {} to group {}", userId, groupId, e);
        }
        return false;
    }

    /**
     * Remove member from group
     */
    public boolean removeMember(int groupId, int userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("User {} removed from group {}", userId, groupId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error removing user {} from group {}", userId, groupId, e);
        }
        return false;
    }

    /**
     * Get groups for a user
     */
    public List<Group> getUserGroups(int userId) {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT g.* FROM chat_groups g " +
                     "JOIN group_members gm ON g.group_id = gm.group_id " +
                     "WHERE gm.user_id = ? " +
                     "ORDER BY g.created_at DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Group group = extractGroupFromResultSet(rs);
                group.setMemberIds(getGroupMemberIds(group.getGroupId()));
                groups.add(group);
            }
        } catch (SQLException e) {
            logger.error("Error getting groups for user {}", userId, e);
        }
        return groups;
    }

    /**
     * Get group members
     */
    public List<User> getGroupMembers(int groupId) {
        List<User> members = new ArrayList<>();
        String sql = "SELECT u.* FROM users u " +
                     "JOIN group_members gm ON u.user_id = gm.user_id " +
                     "WHERE gm.group_id = ? " +
                     "ORDER BY gm.member_role DESC, u.full_name";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(userService.getUserById(rs.getInt("user_id")));
            }
        } catch (SQLException e) {
            logger.error("Error getting members for group {}", groupId, e);
        }
        return members;
    }

    /**
     * Get group member IDs
     */
    private List<Integer> getGroupMemberIds(int groupId) {
        List<Integer> memberIds = new ArrayList<>();
        String sql = "SELECT user_id FROM group_members WHERE group_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                memberIds.add(rs.getInt("user_id"));
            }
        } catch (SQLException e) {
            logger.error("Error getting member IDs for group {}", groupId, e);
        }
        return memberIds;
    }

    /**
     * Get group by ID
     */
    public Group getGroupById(int groupId) {
        String sql = "SELECT * FROM chat_groups WHERE group_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Group group = extractGroupFromResultSet(rs);
                group.setMemberIds(getGroupMemberIds(groupId));
                return group;
            }
        } catch (SQLException e) {
            logger.error("Error getting group by ID {}", groupId, e);
        }
        return null;
    }

    /**
     * Check if user is member of group
     */
    public boolean isMember(int groupId, int userId) {
        String sql = "SELECT COUNT(*) FROM group_members WHERE group_id = ? AND user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking membership for user {} in group {}", userId, groupId, e);
        }
        return false;
    }

    /**
     * Extract Group from ResultSet
     */
    private Group extractGroupFromResultSet(ResultSet rs) throws SQLException {
        Group group = new Group();
        group.setGroupId(rs.getInt("group_id"));
        group.setGroupName(rs.getString("group_name"));
        group.setGroupDescription(rs.getString("group_description"));
        group.setCreatorId(rs.getInt("creator_id"));
        group.setGroupAvatarUrl(rs.getString("group_avatar_url"));
        group.setCreatedAt(rs.getTimestamp("created_at"));
        return group;
    }
}
