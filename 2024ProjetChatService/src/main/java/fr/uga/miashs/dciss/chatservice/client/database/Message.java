package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.Timestamp;

public class Message {
    private final long id;
    private final int senderId;
    private final int receiverId;
    private final String content;
    private final int messageType;
    private final Timestamp timestamp;
    private int status;

    public Message(long id, int senderId, int receiverId, String content, 
                  int messageType, Timestamp timestamp, int status) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.messageType = messageType;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters
    public long getId() { return id; }
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public int getMessageType() { return messageType; }
    public Timestamp getTimestamp() { return timestamp; }
    public int getStatus() { return status; }

    // Setter pour le status uniquement
    public void setStatus(int status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Message{id=%d, from=%d, to=%d, content='%s', type=%d, status=%d}", 
            id, senderId, receiverId, content, messageType, status);
    }

    // Constantes pour les types de messages
    public static final int TYPE_TEXT = 1;
    public static final int TYPE_FILE = 2;
    public static final int TYPE_IMAGE = 3;
    
    // Constantes pour les statuts de messages
    public static final int STATUS_SENT = 0;
    public static final int STATUS_RECEIVED = 1;
    public static final int STATUS_READ = 2;
}