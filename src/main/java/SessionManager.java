import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

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

    public synchronized String requestLogin(User user) {
       
        if (user == null) {
            return "INVALID_USER";
        }

        if (activeSessions.containsKey(user.getId())) {
            System.out.println(user + " is already logged in.");
            return "ALREADY_LOGGED_IN";
        }

        if (isUserWaiting(user.getId())){
            System.out.println(user + " is already in the waiting queue.");
            printSystemState();
            return "WAITING:"+getWaitingPosition(user.getId());
        }

        boolean acquired = loginSemaphore.tryAcquire();

        if (acquired) { 
            UserSession session = new UserSession(user);
            activeSessions.put(user.getId(), session);
            session.start();
           
            System.out.println(user + " logged in successfully.");
            printSystemState();
            return "LOGIN_SUCCESS";
        } else {
            waitingQueue.offer(new UserRequest(user, "waiting"));
            System.out.println(user + " added to waiting queue.");
            printSystemState();
            return "WAITING:"+getWaitingPosition(user.getId());
        }
    }

    public synchronized boolean isUserLoggedIn(int userId) {
        return activeSessions.containsKey(userId);
    }

    public synchronized void logout(User user) {
        UserSession session = activeSessions.remove(user.getId());
        
        if (session == null){
            return;
        }

        
        fileAccessManager.releaseReadAccess(user);
        fileAccessManager.releaseWriteReservation(user);
        session.terminate();
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
                UserSession session = new UserSession(nextUser);
                activeSessions.put(nextUser.getId(), session);
                session.start();
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
                .map(session -> "\"" + session.getUser().toString() + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    public synchronized String getWaitingUsersJson() {
        return waitingQueue.stream()
                .map(request -> "\"" + request.getUser().toString() + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    public synchronized int getWaitingPosition(int userId) {
        int position = 1;
        for (UserRequest request : waitingQueue) {
            if (request.getUser().getId() == userId) {
                return position;
            }
            position++;
        }
        return -1;
    }

    public synchronized boolean isUserWaiting(int userId) {
        return getWaitingPosition(userId) != -1;
    }
}