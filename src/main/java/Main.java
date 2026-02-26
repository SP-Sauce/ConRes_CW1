//This is the workshop 3
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
    System.out.println(file.getfileText());
  }

  public void Edit(File file, String Text) {
    file.updatefileText(Text);
  }
}

public class Main {

  public static void main(String[] args) {
    File file = new File();
    file.initialiseText("");
    ThreadSafeTransaction thread1 = new ThreadSafeTransaction();
    ThreadSafeTransaction thread2 = new ThreadSafeTransaction();
    thread2.Edit(file, "Hi");
    thread1.Read(file);
    thread2.Edit(file, "world");
  }
}
