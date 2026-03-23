import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class UserSession extends Thread {
    private final User user;
    private final FileAccessManager fileAccessManager;
    private final BlockingQueue<SessionTask<?>> taskQueue;
    private volatile boolean active = true;

    public UserSession(User user, FileAccessManager fileAccessManager) {
        this.user = user;
        this.fileAccessManager = fileAccessManager;
        this.taskQueue = new LinkedBlockingQueue<>();
        setName("UserSession-" + user.getId());
    }

    public User getUser() {
        return user;
    }

    public FileAccessManager getFileAccessManager() {
        return fileAccessManager;
    }

    public boolean isActiveSession() {
        return active;
    }

    public <T> T submitAndWait(Callable<T> action) {
        if (!active) {
            throw new IllegalStateException("Session is no longer active.");
        }

        SessionTask<T> task = new SessionTask<>(action);
        taskQueue.offer(task);
        return task.awaitResult();
    }

    public void terminate() {
        active = false;
        interrupt();
    }

    @Override
    public void run() {
        System.out.println(user + " session thread started as " + getName());
        try {
            while (active || !taskQueue.isEmpty()) {
                SessionTask<?> task = taskQueue.take();
                task.execute();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println(user + " session thread ended");
        }
    }

    private static class SessionTask<T> {
        private final Callable<T> action;
        private final CountDownLatch doneSignal = new CountDownLatch(1);
        private T result;
        private RuntimeException runtimeFailure;

        SessionTask(Callable<T> action) {
            this.action = action;
        }

        void execute() {
            try {
                result = action.call();
            } catch (RuntimeException e) {
                runtimeFailure = e;
            } catch (Exception e) {
                runtimeFailure = new RuntimeException(e);
            } finally {
                doneSignal.countDown();
            }
        }

        T awaitResult() {
            try {
                doneSignal.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for session task.", e);
            }

            if (runtimeFailure != null) {
                throw runtimeFailure;
            }

            return result;
        }
    }
}