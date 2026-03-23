import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;

public class FileAccessManager {
    private final Path filePath;
    private final ReentrantReadWriteLock lock;

    private final Set<Integer> activeReaders;
    private final Set<Integer> readLockHolders;
    private Integer currentWriter;

    // Reservation state for editing session
    private Integer reservedWriter;
    private final Queue<User> writeWaitingQueue;

    public FileAccessManager(String fileName) {
        this.filePath = Path.of(fileName);
        this.lock = new ReentrantReadWriteLock(true);
        this.readLockHolders = new TreeSet<>();
        this.activeReaders = new TreeSet<>();
        this.currentWriter = null;
        this.reservedWriter = null;
        this.writeWaitingQueue = new LinkedList<>();
        initialiseFile();
    }

    private void initialiseFile() {
        try {
            if (!Files.exists(filePath)) {
                Files.writeString(
                        filePath,
                        "Initial Product Specification\n",
                        StandardOpenOption.CREATE);
                System.out.println("Shared file created: " + filePath);
            }
        } catch (IOException e) {
            System.out.println("File init error: " + e.getMessage());
        }
    }

    public synchronized String requestWriteReservation(User user) {
        if (reservedWriter == null) {
            reservedWriter = user.getId();
            System.out.println(user + " granted WRITE RESERVATION.");
            printFileState();
            return "WRITE_GRANTED";
        }

        if (reservedWriter.equals(user.getId())) {
            return "WRITE_GRANTED";
        }

        boolean alreadyQueued = writeWaitingQueue.stream()
                .anyMatch(u -> u.getId() == user.getId());

        if (!alreadyQueued) {
            writeWaitingQueue.offer(user);
            System.out.println(user + " added to WRITE waiting queue.");
        }

        int position = getWriteWaitingPosition(user.getId());
        printFileState();
        return "WRITE_WAITING:" + position;
    }

    public synchronized void releaseWriteReservation(User user) {
        if (reservedWriter == null || !reservedWriter.equals(user.getId())) {
            return;
        }

        reservedWriter = null;
        System.out.println(user + " released WRITE RESERVATION.");

        User nextUser = writeWaitingQueue.poll();
        if (nextUser != null) {
            reservedWriter = nextUser.getId();
            System.out.println(nextUser + " promoted from WRITE waiting queue to reserved writer.");
        }

        printFileState();
    }

    public synchronized boolean hasWriteReservation(int userId) {
        return reservedWriter != null && reservedWriter == userId;
    }

    public synchronized int getWriteWaitingPosition(int userId) {
        int position = 1;
        for (User user : writeWaitingQueue) {
            if (user.getId() == userId) {
                return position;
            }
            position++;
        }
        return -1;
    }

    public synchronized String getReaderStatus() {
        return activeReaders.isEmpty() ? "None" : activeReaders.toString();
    }

    public synchronized String getWriterStatus() {
        if (reservedWriter != null) {
            return reservedWriter.toString();
        }
        return "None";
    }

    public synchronized String getWriteReservationStatus() {
        if (!writeWaitingQueue.isEmpty()) {
            return "Write Queue: " + writeWaitingQueue.stream().map(User::getId).toList();
        }
        return "No Write Reservation";
    }
// readFile is a console/demo helper 
    public void readFile(User user) {
        lock.readLock().lock();
        addReader(user.getId());

        try {
            System.out.println(user + " is READING the file...");
            printFileState();

            String content = Files.readString(filePath);

            System.out.println("----- FILE CONTENT FOR " + user.getUsername() + " -----");
            System.out.println(content);
            System.out.println("--------------------------------------------");

        } catch (IOException e) {
            System.out.println("Read error for " + user + ": " + e.getMessage());
        } finally {
            removeReader(user.getId());
            System.out.println(user + " finished READING.");
            printFileState();
            lock.readLock().unlock();
        }
    }

    public boolean writeFile(User user, String newContent) {
        if (!hasWriteReservation(user.getId())) {
            System.out.println(user + " attempted WRITE without reservation.");
            return false;
        }
        boolean acquired = false;
        boolean writerSet = false;
        try {
            acquired = lock.writeLock().tryLock(10, TimeUnit.SECONDS);
            if (!acquired){
                System.out.println(user + " failed to acquire write lock within timeout.");
                return false;
            }
            setWriter(user.getId());
            writerSet = true;
            System.out.println(user + " is WRITING to the file...");
            printFileState();
            
            Files.writeString(
                    filePath,
                    newContent + System.lineSeparator(),
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);

            System.out.println(user + " finished WRITING.");
            return true;
        } catch(InterruptedException e){
            Thread.currentThread().interrupt();
            System.out.println(user + " was interrupted while waiting for lock.");
            return false;
        } 
        catch (IOException e) {
            System.out.println("Write error for " + user + ": " + e.getMessage());
            return false;
        } finally {
            if (writerSet ==  true){
                clearWriter();
            }
            printFileState();
            if (acquired){
                lock.writeLock().unlock();
            }
        }
    }

    public synchronized String requestReadAccess(User user) {
        if (readLockHolders.contains(user.getId())) {
            return "READ_GRANTED";
        }

        lock.readLock().lock();
        activeReaders.add(user.getId());
        readLockHolders.add(user.getId());

        System.out.println(user + " acquired READ LOCK and entered READ mode.");
        printFileState();
        return "READ_GRANTED";
    }

    public synchronized void releaseReadAccess(User user) {
        if (!readLockHolders.contains(user.getId())) {
            return;
        }

        activeReaders.remove(user.getId());
        readLockHolders.remove(user.getId());
        lock.readLock().unlock();

        System.out.println(user + " released READ LOCK and exited READ mode.");
        printFileState();
    }

    public synchronized String getFileContentForActiveReader(User user) {
        if (!readLockHolders.contains(user.getId())) {
            return "Read access not held by this user.";
        }

        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            return "Error reading file content.";
        }
    }

    private synchronized void addReader(int userId) {
        activeReaders.add(userId);
    }

    private synchronized void removeReader(int userId) {
        activeReaders.remove(userId);
    }

    private synchronized void setWriter(int userId) {
        currentWriter = userId;
    }

    private synchronized void clearWriter() {
        currentWriter = null;
    }

    public synchronized String getFileStatus() {
        if (currentWriter != null) {
            return "Updating: " + currentWriter;
        }
        if (reservedWriter != null) {
            return "Write Reserved By: " + reservedWriter;
        }
        if (!activeReaders.isEmpty()) {
            return "Reading: " + activeReaders;
        }
        return "Idle";
    }

    public synchronized void printFileState() {
        System.out.println("----- FILE ACCESS STATE -----");
        System.out.println("Readers: " + activeReaders);
        System.out.println("Writer: " + (currentWriter == null ? "None" : currentWriter));
        System.out.println("Reserved Writer: " + (reservedWriter == null ? "None" : reservedWriter));
        System.out.println("Write Waiting Queue: " + writeWaitingQueue.stream().map(User::getId).toList());
        System.out.println("Status: " + getFileStatus());
        System.out.println("-----------------------------");
    }

    public String getFileContent() {
        lock.readLock().lock();
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            return "Error reading file content.";
        } finally {
            lock.readLock().unlock();
        }
    }
}