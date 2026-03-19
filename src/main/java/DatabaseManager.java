import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // The "jdbc:sqlite:" prefix tells Java to use the SQLite driver
    private static final String URL = "jdbc:sqlite:concurrency_demo.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "username TEXT NOT NULL, " +
                     "balance REAL DEFAULT 0.0)";

        try (Connection conn = connect(); 
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Database initialized and ready.");
        } catch (SQLException e) {
            System.out.println("Init Error: " + e.getMessage());
        }
    }
}