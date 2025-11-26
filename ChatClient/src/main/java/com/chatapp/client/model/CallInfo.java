package com.chatapp.client.model;

import java.sql.Timestamp;

public class CallInfo {
    private int callId;
    private int callerId;
    private int receiverId;
    private CallType callType;
    private CallStatus callStatus;
    private Timestamp startedAt;
    private Timestamp endedAt;
    private int duration; // in seconds

    // Thông tin bổ sung
    private String callerName;
    private String receiverName;

    public enum CallType {
        VOICE,
        VIDEO
    }

    public enum CallStatus {
        RINGING,
        ACCEPTED,
        REJECTED,
        ENDED,
        MISSED
    }

    public CallInfo() {
        this.callStatus = CallStatus.RINGING;
        this.duration = 0;
    }

    public CallInfo(int callerId, int receiverId, CallType callType) {
        this.callerId = callerId;
        this.receiverId = receiverId;
        this.callType = callType;
        this.callStatus = CallStatus.RINGING;
        this.duration = 0;
    }

    // Getters and Setters
    public int getCallId() {
        return callId;
    }

    public void setCallId(int callId) {
        this.callId = callId;
    }

    public int getCallerId() {
        return callerId;
    }

    public void setCallerId(int callerId) {
        this.callerId = callerId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public CallStatus getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(CallStatus callStatus) {
        this.callStatus = callStatus;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Timestamp endedAt) {
        this.endedAt = endedAt;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    @Override
    public String toString() {
        return "CallInfo{" +
                "callId=" + callId +
                ", callerId=" + callerId +
                ", receiverId=" + receiverId +
                ", callType=" + callType +
                ", callStatus=" + callStatus +
                '}';
    }
}
