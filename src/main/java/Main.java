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
