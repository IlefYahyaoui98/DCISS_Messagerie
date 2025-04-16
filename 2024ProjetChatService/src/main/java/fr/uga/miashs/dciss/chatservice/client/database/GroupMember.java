package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.Timestamp;

public class GroupMember {
    private final int groupId;
    private final int userId;
    private final Timestamp joinDate;
    private int role;
    
    // Références aux objets liés
    private User user;
    private Group group;

    public static final int ROLE_MEMBER = 0;
    public static final int ROLE_ADMIN = 1;

    public GroupMember(int groupId, int userId, Timestamp joinDate, int role) {
        this.groupId = groupId;
        this.userId = userId;
        this.joinDate = joinDate;
        this.role = role;
    }

    // Getters et Setters classiques
    public int getGroupId() { return groupId; }
    public int getUserId() { return userId; }
    public Timestamp getJoinDate() { return joinDate; }
    public int getRole() { return role; }
    public void setRole(int role) { this.role = role; }

    // Getters et Setters pour les objets liés
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
}