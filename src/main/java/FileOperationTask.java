public class FileOperationTask extends Thread {
    private final User user;
    private final FileAccessManager fileAccessManager;
    private final String action;

    public FileOperationTask(User user, FileAccessManager fileAccessManager, String action) {
        this.user = user;
        this.fileAccessManager = fileAccessManager;
        this.action = action;
    }

    @Override
    public void run() {
        if ("read".equalsIgnoreCase(action)) {
            fileAccessManager.readFile(user);
        } else if ("write".equalsIgnoreCase(action)) {
            String updatedText = "Updated by " + user.getUsername() + " (ID: " + user.getId() + ")";
            fileAccessManager.writeFile(user, updatedText);
        }
    }
}