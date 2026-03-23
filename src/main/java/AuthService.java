public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public User authenticate(int id, String username) {
        return userDAO.findUserByIdAndUsername(id, username);
    }
}