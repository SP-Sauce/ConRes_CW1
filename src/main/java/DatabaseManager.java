import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // The "jdbc:sqlite:" prefix tells Java to use the SQLite driver
    private static final String URL = "jdbc:sqlite:ConRes.db";

    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found", e);
        }
        return DriverManager.getConnection(URL);
    }

    public static void initialise() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE" +
                ")";

        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database initialized and ready.");
        } catch (SQLException e) {
            System.out.println("Init Error: " + e.getMessage());
        }
    }

    public static void setup_users() {
        String sql = "INSERT INTO users (id, username) VALUES " +
                "(1, 'Ali'), " +
                "(2, 'Sara'), " +
                " (3, 'Zain'), " +
                "(4, 'Adam'), " +
                "(5, 'Omar')";

        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database users added");
        } catch (SQLException e) {
            System.out.println("Init Error: " + e.getMessage());
        }
    }

    public static void DBsetup() {
        initialise();
        setup_users();
    }
}