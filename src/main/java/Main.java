public class Main {
    public static void main(String[] args) {
        DatabaseManager.DBsetup();

        AuthService authService = new AuthService();
        FileAccessManager fileAccessManager = new FileAccessManager("ProductSpecification.txt");
        SessionManager sessionManager = new SessionManager(4, fileAccessManager);

        User u1 = authService.authenticate(1, "Ali");
        User u2 = authService.authenticate(2, "Sara");
        User u3 = authService.authenticate(3, "Zain");
        User u4 = authService.authenticate(4, "Adam");
        User u5 = authService.authenticate(5, "Omar");

        sessionManager.requestLogin(u1, "read");
        sessionManager.requestLogin(u2, "read");
        sessionManager.requestLogin(u3, "write");
        sessionManager.requestLogin(u4, "read");
        sessionManager.requestLogin(u5, "write");
    }
}