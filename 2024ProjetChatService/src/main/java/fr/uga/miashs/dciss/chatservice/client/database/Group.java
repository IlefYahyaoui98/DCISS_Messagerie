package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.Timestamp;

public class Group {
    private final int id;
    private String name;
    private final int ownerId;
    private final Timestamp creationDate;
    private String description;
    private String avatarPath;

    public Group(int id, String name, int ownerId, Timestamp creationDate, 
                String description, String avatarPath) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.creationDate = creationDate;
        this.description = description;
        this.avatarPath = avatarPath;
    }

    // Getters et Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getOwnerId() { return ownerId; }
    public Timestamp getCreationDate() { return creationDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
}