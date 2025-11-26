package com.chatapp.server.service;

import com.chatapp.server.model.CallInfo;
import com.chatapp.server.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CallService {
    private static final Logger logger = LoggerFactory.getLogger(CallService.class);
    private final DatabaseManager dbManager;

    public CallService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Initiate a call
     */
    public CallInfo initiateCall(int callerId, int receiverId, CallInfo.CallType callType) {
        String sql = "INSERT INTO calls (caller_id, receiver_id, call_type, call_status) VALUES (?, ?, ?, 'RINGING')";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, callerId);
            pstmt.setInt(2, receiverId);
            pstmt.setString(3, callType.name());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int callId = rs.getInt(1);
                        logger.info("Call initiated from {} to {} ({})", callerId, receiverId, callType);
                        return getCallById(callId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error initiating call from {} to {}", callerId, receiverId, e);
        }
        return null;
    }

    /**
     * Accept call
     */
    public boolean acceptCall(int callId) {
        String sql = "UPDATE calls SET call_status = 'ACCEPTED' WHERE call_id = ? AND call_status = 'RINGING'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, callId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Call {} accepted", callId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error accepting call {}", callId, e);
        }
        return false;
    }

    /**
     * Reject call
     */
    public boolean rejectCall(int callId) {
        String sql = "UPDATE calls SET call_status = 'REJECTED', ended_at = CURRENT_TIMESTAMP WHERE call_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, callId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Call {} rejected", callId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error rejecting call {}", callId, e);
        }
        return false;
    }

    /**
     * End call
     */
    public boolean endCall(int callId) {
        String sql = "UPDATE calls SET call_status = 'ENDED', ended_at = CURRENT_TIMESTAMP, " +
                     "duration = TIMESTAMPDIFF(SECOND, started_at, CURRENT_TIMESTAMP) WHERE call_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, callId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Call {} ended", callId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error ending call {}", callId, e);
        }
        return false;
    }

    /**
     * Mark call as missed
     */
    public boolean markCallAsMissed(int callId) {
        String sql = "UPDATE calls SET call_status = 'MISSED', ended_at = CURRENT_TIMESTAMP WHERE call_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, callId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Call {} marked as missed", callId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error marking call {} as missed", callId, e);
        }
        return false;
    }

    /**
     * Get call history for a user
     */
    public List<CallInfo> getCallHistory(int userId, int limit) {
        List<CallInfo> calls = new ArrayList<>();
        String sql = "SELECT c.*, " +
                     "caller.full_name as caller_name, receiver.full_name as receiver_name " +
                     "FROM calls c " +
                     "JOIN users caller ON c.caller_id = caller.user_id " +
                     "JOIN users receiver ON c.receiver_id = receiver.user_id " +
                     "WHERE c.caller_id = ? OR c.receiver_id = ? " +
                     "ORDER BY c.started_at DESC LIMIT ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, limit);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                calls.add(extractCallInfoFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting call history for user {}", userId, e);
        }
        return calls;
    }

    /**
     * Get call by ID
     */
    public CallInfo getCallById(int callId) {
        String sql = "SELECT c.*, " +
                     "caller.full_name as caller_name, receiver.full_name as receiver_name " +
                     "FROM calls c " +
                     "JOIN users caller ON c.caller_id = caller.user_id " +
                     "JOIN users receiver ON c.receiver_id = receiver.user_id " +
                     "WHERE c.call_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, callId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractCallInfoFromResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("Error getting call by ID {}", callId, e);
        }
        return null;
    }

    /**
     * Extract CallInfo from ResultSet
     */
    private CallInfo extractCallInfoFromResultSet(ResultSet rs) throws SQLException {
        CallInfo callInfo = new CallInfo();
        callInfo.setCallId(rs.getInt("call_id"));
        callInfo.setCallerId(rs.getInt("caller_id"));
        callInfo.setReceiverId(rs.getInt("receiver_id"));
        callInfo.setCallType(CallInfo.CallType.valueOf(rs.getString("call_type")));
        callInfo.setCallStatus(CallInfo.CallStatus.valueOf(rs.getString("call_status")));
        callInfo.setStartedAt(rs.getTimestamp("started_at"));
        callInfo.setEndedAt(rs.getTimestamp("ended_at"));
        callInfo.setDuration(rs.getInt("duration"));
        callInfo.setCallerName(rs.getString("caller_name"));
        callInfo.setReceiverName(rs.getString("receiver_name"));
        return callInfo;
    }
}
