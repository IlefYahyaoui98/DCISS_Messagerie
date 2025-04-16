package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.Timestamp;

public class User {
    private final int id;
    private String nickname;
    private Timestamp lastSeen;
    private String avatarPath;
    private String status;

    public User(int id, String nickname, Timestamp lastSeen, String avatarPath, String status) {
        this.id = id;
        this.nickname = nickname;
        this.lastSeen = lastSeen;
        this.avatarPath = avatarPath;
        this.status = status;
    }

    // Getters et Setters
    public int getId() { return id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Timestamp getLastSeen() { return lastSeen; }
    public void setLastSeen(Timestamp lastSeen) { this.lastSeen = lastSeen; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}