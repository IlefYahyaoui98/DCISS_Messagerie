package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttachmentRepository {
    private final Connection connection;

    public AttachmentRepository() {
        this.connection = DatabaseManager.getConnection();
    }

    public long saveAttachment(Attachment attachment) {
        String sql = "INSERT INTO attachments (message_id, file_path, file_name, file_type, file_size) " +
                    "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, attachment.getMessageId());
            pstmt.setString(2, attachment.getFilePath());
            pstmt.setString(3, attachment.getFileName());
            pstmt.setString(4, attachment.getFileType());
            pstmt.setLong(5, attachment.getFileSize());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde de la pièce jointe: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public List<Attachment> getMessageAttachments(long messageId) {
        List<Attachment> attachments = new ArrayList<>();
        String sql = "SELECT * FROM attachments WHERE message_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                attachments.add(new Attachment(
                    rs.getLong("id"),
                    rs.getLong("message_id"),
                    rs.getString("file_path"),
                    rs.getString("file_name"),
                    rs.getString("file_type"),
                    rs.getLong("file_size"),
                    rs.getTimestamp("upload_timestamp")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des pièces jointes: " + e.getMessage());
            e.printStackTrace();
        }
        return attachments;
    }

    public void deleteAttachment(long attachmentId) {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, attachmentId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la pièce jointe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteMessageAttachments(long messageId) {
        String sql = "DELETE FROM attachments WHERE message_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression des pièces jointes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}