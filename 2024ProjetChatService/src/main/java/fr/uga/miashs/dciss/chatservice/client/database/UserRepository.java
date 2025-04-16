package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.*;

public class UserRepository {
    private final Connection connection;

    public UserRepository() {
        this.connection = DatabaseManager.getConnection();
    }

    public void saveUser(User user) {
        String sql = "INSERT INTO users (id, nickname, last_seen, avatar_path, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, user.getId());
            pstmt.setString(2, user.getNickname());
            pstmt.setTimestamp(3, user.getLastSeen());
            pstmt.setString(4, user.getAvatarPath());
            pstmt.setString(5, user.getStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la sauvegarde de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("nickname"),
                    rs.getTimestamp("last_seen"),
                    rs.getString("avatar_path"),
                    rs.getString("status")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void updateUser(User user) {
        String sql = "UPDATE users SET nickname = ?, last_seen = ?, avatar_path = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getNickname());
            pstmt.setTimestamp(2, user.getLastSeen());
            pstmt.setString(3, user.getAvatarPath());
            pstmt.setString(4, user.getStatus());
            pstmt.setInt(5, user.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de l'utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateLastSeen(int userId) {
        String sql = "UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de last_seen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}