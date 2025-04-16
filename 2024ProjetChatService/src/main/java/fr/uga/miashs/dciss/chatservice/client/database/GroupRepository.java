package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.*;

public class GroupRepository {
    private final Connection connection;

    public GroupRepository() {
        this.connection = DatabaseManager.getConnection();
    }

    public void saveGroup(Group group) {
        String sql = "INSERT INTO groups (id, name, owner_id, creation_date, description, avatar_path) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, group.getId());
            pstmt.setString(2, group.getName());
            pstmt.setInt(3, group.getOwnerId());
            pstmt.setTimestamp(4, group.getCreationDate());
            pstmt.setString(5, group.getDescription());
            pstmt.setString(6, group.getAvatarPath());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde du groupe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Group getGroupById(int groupId) {
        String sql = "SELECT * FROM groups WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Group(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("owner_id"),
                    rs.getTimestamp("creation_date"),
                    rs.getString("description"),
                    rs.getString("avatar_path")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du groupe: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void updateGroup(Group group) {
        String sql = "UPDATE groups SET name = ?, description = ?, avatar_path = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, group.getName());
            pstmt.setString(2, group.getDescription());
            pstmt.setString(3, group.getAvatarPath());
            pstmt.setInt(4, group.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du groupe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteGroup(int groupId) {
        String sql = "DELETE FROM groups WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du groupe: " + e.getMessage());
            e.printStackTrace();
        }
    }
}