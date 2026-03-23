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
        // console demo helper
        if ("read".equalsIgnoreCase(action)) {
            fileAccessManager.readFile(user);
        } else if ("write".equalsIgnoreCase(action)) {
            boolean writeSuccess = fileAccessManager.writeFile(user, content);
            if (writeSuccess){
                System.out.println(user + " successfully wrote to the file.");
            }
            else{
                System.out.println(user + " failed to write to the file.");
            }
            
        }
    }
}