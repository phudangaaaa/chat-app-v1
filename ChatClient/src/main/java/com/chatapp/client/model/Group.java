package com.chatapp.client.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Group {
    private int groupId;
    private String groupName;
    private String groupDescription;
    private int creatorId;
    private String groupAvatarUrl;
    private Timestamp createdAt;
    private List<Integer> memberIds;

    public Group() {
        this.memberIds = new ArrayList<>();
    }

    public Group(String groupName, String groupDescription, int creatorId) {
        this.groupName = groupName;
        this.groupDescription = groupDescription;
        this.creatorId = creatorId;
        this.memberIds = new ArrayList<>();
    }

    // Getters and Setters
    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public String getGroupAvatarUrl() {
        return groupAvatarUrl;
    }

    public void setGroupAvatarUrl(String groupAvatarUrl) {
        this.groupAvatarUrl = groupAvatarUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<Integer> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Integer> memberIds) {
        this.memberIds = memberIds;
    }

    public void addMember(int userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    public void removeMember(int userId) {
        memberIds.remove(Integer.valueOf(userId));
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupId=" + groupId +
                ", groupName='" + groupName + '\'' +
                ", creatorId=" + creatorId +
                ", memberCount=" + memberIds.size() +
                '}';
    }
}
