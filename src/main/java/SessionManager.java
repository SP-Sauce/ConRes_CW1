import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class SessionManager {
    private final Semaphore loginSemaphore;
    private final BlockingQueue<UserRequest> waitingQueue;
    private final ConcurrentHashMap<Integer, User> activeSessions;
    private final FileAccessManager fileAccessManager;

    public SessionManager(int maxConcurrentUsers, FileAccessManager fileAccessManager) {
        this.loginSemaphore = new Semaphore(maxConcurrentUsers, true);
        this.waitingQueue = new LinkedBlockingQueue<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.fileAccessManager = fileAccessManager;
    }

    public synchronized boolean requestLogin(User user) {
        if (user == null) {
            return false;
        }

        if (activeSessions.containsKey(user.getId())) {
            System.out.println(user + " is already logged in.");
            return true;
        }

        boolean acquired = loginSemaphore.tryAcquire();

        if (acquired) {
            activeSessions.put(user.getId(), user);
            System.out.println(user + " logged in successfully.");
            printSystemState();
            return true;
        } else {
            waitingQueue.offer(new UserRequest(user, "waiting"));
            System.out.println(user + " added to waiting queue.");
            printSystemState();
            return false;
        }
    }

    public synchronized boolean isUserLoggedIn(int userId) {
        return activeSessions.containsKey(userId);
    }

    public synchronized void logout(User user) {
        if (!activeSessions.containsKey(user.getId())) {
            return;
        }

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
                User nextUser = nextRequest.getUser();
                activeSessions.put(nextUser.getId(), nextUser);
                System.out.println(nextUser + " moved from waiting queue to active users.");
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

    public synchronized String getActiveUsersJson() {
        return activeSessions.values().stream()
                .map(user -> "\"" + user.toString() + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    public synchronized String getWaitingUsersJson() {
        return waitingQueue.stream()
                .map(request -> "\"" + request.getUser().toString() + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }
}