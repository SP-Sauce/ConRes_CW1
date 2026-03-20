import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.TreeSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileAccessManager {
    private final Path filePath;
    private final ReentrantReadWriteLock lock;

    private final Set<Integer> activeReaders;
    private Integer currentWriter;

    public FileAccessManager(String fileName) {
        this.filePath = Path.of(fileName);
        this.lock = new ReentrantReadWriteLock(true);
        this.activeReaders = new TreeSet<>();
        this.currentWriter = null;
        initialiseFile();
    }

    private void initialiseFile() {
        try {
            if (!Files.exists(filePath)) {
                Files.writeString(
                        filePath,
                        "Initial Product Specification\n",
                        StandardOpenOption.CREATE
                );
                System.out.println("Shared file created: " + filePath);
            }
        } catch (IOException e) {
            System.out.println("File init error: " + e.getMessage());
        }
    }

    public void readFile(User user) {
        lock.readLock().lock();
        addReader(user.getId());

        try {
            System.out.println(user + " is READING the file...");
            printFileState();

            String content = Files.readString(filePath);

            Thread.sleep(2000);

            System.out.println("----- FILE CONTENT FOR " + user.getUsername() + " -----");
            System.out.println(content);
            System.out.println("--------------------------------------------");

        } catch (IOException e) {
            System.out.println("Read error for " + user + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(user + " read interrupted.");
        } finally {
            removeReader(user.getId());
            System.out.println(user + " finished READING.");
            printFileState();
            lock.readLock().unlock();
        }
    }

    public void writeFile(User user, String newContent) {
        lock.writeLock().lock();
        setWriter(user.getId());

        try {
            System.out.println(user + " is WRITING to the file...");
            printFileState();

            Thread.sleep(3000);

            Files.writeString(
                    filePath,
                    newContent + System.lineSeparator(),
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            System.out.println(user + " finished WRITING.");

        } catch (IOException e) {
            System.out.println("Write error for " + user + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(user + " write interrupted.");
        } finally {
            clearWriter();
            printFileState();
            lock.writeLock().unlock();
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
        if (!activeReaders.isEmpty()) {
            return "Reading: " + activeReaders;
        }
        return "Idle";
    }

    public synchronized void printFileState() {
        System.out.println("----- FILE ACCESS STATE -----");
        System.out.println("Readers: " + activeReaders);
        System.out.println("Writer: " + (currentWriter == null ? "None" : currentWriter));
        System.out.println("Status: " + getFileStatus());
        System.out.println("-----------------------------");
    }
}