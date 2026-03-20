import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class SessionManager {
    private final Semaphore loginSemaphore;
    private final BlockingQueue<UserRequest> waitingQueue;
    private final ConcurrentHashMap<Integer, UserSession> activeSessions;
    private final FileAccessManager fileAccessManager;

    public SessionManager(int maxConcurrentUsers, FileAccessManager fileAccessManager) {
        this.loginSemaphore = new Semaphore(maxConcurrentUsers, true);
        this.waitingQueue = new LinkedBlockingQueue<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.fileAccessManager = fileAccessManager;
    }

    public synchronized void requestLogin(User user, String action) {
        if (user == null) {
            System.out.println("Authentication failed.");
            return;
        }

        if (activeSessions.containsKey(user.getId())) {
            System.out.println(user + " is already logged in.");
            return;
        }

        boolean acquired = loginSemaphore.tryAcquire();

        if (acquired) {
            startSession(user, action);
        } else {
            waitingQueue.offer(new UserRequest(user, action));
            System.out.println(user + " added to waiting queue.");
            printSystemState();
        }
    }

    private synchronized void startSession(User user, String action) {
        UserSession session = new UserSession(user, this, fileAccessManager, action);
        activeSessions.put(user.getId(), session);
        System.out.println(user + " logged in successfully.");
        printSystemState();
        session.start();
    }

    public synchronized void logout(User user) {
        activeSessions.remove(user.getId());
        loginSemaphore.release();
        System.out.println(user + " logged out.");
        promoteNextUser();
        printSystemState();
    }

    private synchronized void promoteNextUser() {
        UserRequest nextRequest = waitingQueue.poll();
        if (nextRequest != null) {
            boolean acquired = loginSemaphore.tryAcquire();
            if (acquired) {
                startSession(nextRequest.getUser(), nextRequest.getAction());
            } else {
                waitingQueue.offer(nextRequest);
            }
        }
    }

    public synchronized void printSystemState() {
        System.out.println("----- SYSTEM STATE -----");
        System.out.println("Active users: " + activeSessions.keySet());
        System.out.println("Waiting users: " +
                waitingQueue.stream().map(r -> r.getUser().getId()).toList());
        System.out.println("------------------------");
        fileAccessManager.printFileState();
    }
}