package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.Timestamp;

public class Attachment {
    private final long id;
    private final long messageId;
    private final String filePath;
    private final String fileName;
    private final String fileType;
    private final long fileSize;
    private final Timestamp uploadTimestamp;

    public Attachment(long id, long messageId, String filePath, String fileName, 
                     String fileType, long fileSize, Timestamp uploadTimestamp) {
        this.id = id;
        this.messageId = messageId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadTimestamp = uploadTimestamp;
    }

    // Getters
    public long getId() { return id; }
    public long getMessageId() { return messageId; }
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public String getFileType() { return fileType; }
    public long getFileSize() { return fileSize; }
    public Timestamp getUploadTimestamp() { return uploadTimestamp; }
}
