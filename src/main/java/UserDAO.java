import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public User findUserByIdAndUsername(int id, String username) {
        String sql = "SELECT id, username FROM users WHERE id = ? AND username = ?";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.setString(2, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"));
            }

        } catch (SQLException e) {
            System.out.println("Database error during authentication: " + e.getMessage());
        }

        return null;
    }
}