public class UserSession extends Thread {
    private final User user;
    private final SessionManager sessionManager;
    private final FileAccessManager fileAccessManager;
    private final String action; // "read" or "write"

    public UserSession(User user, SessionManager sessionManager, FileAccessManager fileAccessManager, String action) {
        this.user = user;
        this.sessionManager = sessionManager;
        this.fileAccessManager = fileAccessManager;
        this.action = action;
    }

    public User getUser() {
        return user;
    }

    @Override
    public void run() {
        try {
            System.out.println(user + " session is active.");

            if ("read".equalsIgnoreCase(action)) {
                fileAccessManager.readFile(user);
            } else if ("write".equalsIgnoreCase(action)) {
                String updatedText = "Updated by " + user.getUsername() + " (ID: " + user.getId() + ")";
                fileAccessManager.writeFile(user, updatedText);
            } else {
                System.out.println(user + " has no valid action.");
            }

            Thread.sleep(1000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(user + " session interrupted.");
        } finally {
            sessionManager.logout(user);
        }
    }
}