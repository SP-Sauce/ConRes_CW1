import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

class File {
  StringBuffer fileText = new StringBuffer("");

  public void initialiseText(String Text) {
    fileText.append(Text);
  }

  public StringBuffer getfileText() {
    return fileText;
  }

  public synchronized void updatefileText(String Text) {
    fileText.append(Text);
  }
}

class ThreadSafeTransaction extends Thread {

  public void Read(File file) {
    System.out.println("fileText: " + file.getfileText());
  }

  public void Edit(File file, String Text) {
    file.updatefileText(Text);
  }
}

public class Main {

  public static void main(String[] args) {
    DatabaseManager.initialize();

    System.out.println("App is running with DB capability!");
    // 2. Start Javalin Web Server
    Javalin app = Javalin.create(config -> {
        // Tell Javalin where your HTML files are
        config.staticFiles.add("/public", Location.CLASSPATH);
    }).start(8080);

    // 3. Create an "Endpoint" for the button
    app.get("/add-user", ctx -> {
        // This is where your concurrency logic lives!
        // For now, we'll just log it.
        System.out.println("Thread " + Thread.currentThread().getId() + " is processing a request.");

        // Call your Database code here
        // DatabaseManager.addUser("User_" + System.currentTimeMillis());

        ctx.result("User added successfully by Thread: " + Thread.currentThread().getId());
    });
    File file = new File();
    file.initialiseText("");
    ThreadSafeTransaction thread1 = new ThreadSafeTransaction();
    ThreadSafeTransaction thread2 = new ThreadSafeTransaction();
    thread1.Edit(file, "hello world");
    thread2.Read(file);
    thread1.Edit(file, "hello world123 this is new text added");
    thread2.Read(file);
  }
}
