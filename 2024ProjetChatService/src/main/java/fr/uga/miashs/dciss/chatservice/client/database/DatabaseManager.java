package fr.uga.miashs.dciss.chatservice.client.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chat_client.db";
    private static Connection connection;

    public static void initializeDatabase() {
        try {
            // Chargement explicite du driver SQLite
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Erreur d'initialisation de la base de données: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Table messages
            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sender_id INTEGER NOT NULL," +
                "receiver_id INTEGER NOT NULL," +
                "content TEXT NOT NULL," +
                "timestamp DATETIME DEFAULT (datetime('now','localtime'))," +
                "message_type INTEGER NOT NULL," +
                "status INTEGER DEFAULT 0" +
                ")");

            // Table attachments
            stmt.execute("CREATE TABLE IF NOT EXISTS attachments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "message_id INTEGER," +
                "file_path TEXT NOT NULL," +
                "file_name TEXT NOT NULL," +
                "file_type TEXT NOT NULL," +
                "file_size INTEGER," +
                "upload_timestamp DATETIME DEFAULT (datetime('now','localtime'))," +
                "FOREIGN KEY (message_id) REFERENCES messages(id)" +
                ")");

            // Table users
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY," +
                "nickname TEXT," +
                "last_seen DATETIME," +
                "avatar_path TEXT," +
                "status TEXT" +
                ")");

            // Table groups
            stmt.execute("CREATE TABLE IF NOT EXISTS groups (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "owner_id INTEGER NOT NULL," +
                "creation_date DATETIME DEFAULT (datetime('now','localtime'))," +
                "description TEXT," +
                "avatar_path TEXT" +
                ")");

            // Table group_members
            stmt.execute("CREATE TABLE IF NOT EXISTS group_members (" +
                "group_id INTEGER," +
                "user_id INTEGER," +
                "join_date DATETIME DEFAULT (datetime('now','localtime'))," +
                "role INTEGER DEFAULT 0," +
                "PRIMARY KEY (group_id, user_id)," +
                "FOREIGN KEY (group_id) REFERENCES groups(id)," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")");

            // Index pour optimisation
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages(receiver_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_group_members ON group_members(group_id)");
        }
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initializeDatabase();
            }
            return connection;
        } catch (SQLException e) {
            System.err.println("Erreur de connexion à la base de données: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }
}