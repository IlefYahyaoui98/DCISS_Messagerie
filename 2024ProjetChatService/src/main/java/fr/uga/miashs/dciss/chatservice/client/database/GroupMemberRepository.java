package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupMemberRepository {
    private final Connection connection;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public GroupMemberRepository() {
        this.connection = DatabaseManager.getConnection();
        this.userRepository = new UserRepository();
        this.groupRepository = new GroupRepository();
    }

    public void saveGroupMember(GroupMember member) {
        String sql = "INSERT INTO group_members (group_id, user_id, join_date, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, member.getGroupId());
            pstmt.setInt(2, member.getUserId());
            pstmt.setTimestamp(3, member.getJoinDate());
            pstmt.setInt(4, member.getRole());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du membre au groupe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public GroupMember getGroupMember(int groupId, int userId) {
        String sql = "SELECT * FROM group_members WHERE group_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                GroupMember member = new GroupMember(
                    rs.getInt("group_id"),
                    rs.getInt("user_id"),
                    rs.getTimestamp("join_date"),
                    rs.getInt("role")
                );
                // Charger les objets liés
                member.setUser(userRepository.getUserById(userId));
                member.setGroup(groupRepository.getGroupById(groupId));
                return member;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du membre du groupe: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public List<GroupMember> getGroupMembers(int groupId) {
        List<GroupMember> members = new ArrayList<>();
        String sql = "SELECT * FROM group_members WHERE group_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                GroupMember member = new GroupMember(
                    rs.getInt("group_id"),
                    rs.getInt("user_id"),
                    rs.getTimestamp("join_date"),
                    rs.getInt("role")
                );
                member.setUser(userRepository.getUserById(member.getUserId()));
                member.setGroup(groupRepository.getGroupById(groupId));
                members.add(member);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des membres du groupe: " + e.getMessage());
            e.printStackTrace();
        }
        return members;
    }

    public void updateMemberRole(int groupId, int userId, int newRole) {
        String sql = "UPDATE group_members SET role = ? WHERE group_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, newRole);
            pstmt.setInt(2, groupId);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du rôle du membre: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeMember(int groupId, int userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du membre du groupe: " + e.getMessage());
            e.printStackTrace();
        }
    }
}