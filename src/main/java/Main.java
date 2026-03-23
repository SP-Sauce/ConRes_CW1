public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager.DBsetup();

            AuthService authService = new AuthService();
            FileAccessManager fileAccessManager = new FileAccessManager("ProductSpecification.txt");
            SessionManager sessionManager = new SessionManager(2, fileAccessManager);

            WebServer webServer = new WebServer(authService, sessionManager, fileAccessManager);
            webServer.start();
            Thread.currentThread().join();

        } catch (Exception e) {
            System.out.println("Server startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}