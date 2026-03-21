public class FileOperationTask extends Thread {
    private final User user;
    private final FileAccessManager fileAccessManager;
    private final String action;
    private final String content;

    public FileOperationTask(User user, FileAccessManager fileAccessManager, String action, String content) {
        this.user = user;
        this.fileAccessManager = fileAccessManager;
        this.action = action;
        this.content = content;
    }

    @Override
    public void run() {
        if ("read".equalsIgnoreCase(action)) {
            fileAccessManager.readFile(user);
        } else if ("write".equalsIgnoreCase(action)) {
            fileAccessManager.writeFile(user, content);
        }
    }
}