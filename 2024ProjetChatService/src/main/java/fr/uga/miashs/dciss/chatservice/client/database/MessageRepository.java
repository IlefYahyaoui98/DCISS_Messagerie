package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import fr.uga.miashs.dciss.chatservice.common.Packet;

public class MessageRepository {
    private final Connection connection;

    public MessageRepository() {
        this.connection = DatabaseManager.getConnection();
    }

    public long saveMessage(int senderId, int receiverId, String content, int messageType) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content, message_type) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.setString(3, content);
            pstmt.setInt(4, messageType);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde du message: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    public void saveMessageFromPacket(Packet packet) {
        saveMessage(packet.srcId, packet.destId, new String(packet.data), determineMessageType(packet));
    }

    public List<Message> getMessageHistory(int userId, int contactId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE (sender_id = ? AND receiver_id = ?) " +
                    "OR (sender_id = ? AND receiver_id = ?) ORDER BY timestamp DESC LIMIT 100";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, contactId);
            pstmt.setInt(3, contactId);
            pstmt.setInt(4, userId);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getInt("sender_id"),
                    rs.getInt("receiver_id"),
                    rs.getString("content"),
                    rs.getInt("message_type"),
                    rs.getTimestamp("timestamp"),
                    rs.getInt("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'historique: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    public List<Message> getGroupMessages(int groupId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE receiver_id = ? ORDER BY timestamp DESC LIMIT 100";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    rs.getInt("sender_id"),
                    rs.getInt("receiver_id"),
                    rs.getString("content"),
                    rs.getInt("message_type"),
                    rs.getTimestamp("timestamp"),
                    rs.getInt("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des messages du groupe: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    public void updateMessageStatus(long messageId, int status) {
        String sql = "UPDATE messages SET status = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, status);
            pstmt.setLong(2, messageId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du statut: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int determineMessageType(Packet packet) {
        // TODO: Implémenter la logique pour déterminer le type de message
        // Par exemple: 1 = texte, 2 = fichier, 3 = image, etc.
        return 1; // Par défaut, on considère que c'est un message texte
    }

    public void deleteMessage(long messageId) {
        String sql = "DELETE FROM messages WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}