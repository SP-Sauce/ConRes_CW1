//This is the workshop 3
class File {
  int fileText = 0;

  public void initialiseText(int Text) {
    fileText = Text;
  }

  public int getfileText() {
    return fileText;
  }

  public synchronized void updatefileText(int Text) {
    fileText = Text;
  }
}

class ThreadSafeTransaction extends Thread {

  public void Deposit(File file, int Text) {
    int newfileText = file.getfileText() + Text;
    file.updatefileText(newfileText);
    System.out.println("Deposit successful");
  }

  public void Withdraw(File file, int Text) {
    if (file.getfileText() >= Text) {
      int newfileText = file.getfileText() - Text;
      file.updatefileText(newfileText);
      System.out.println("Withdrawal successful");
    } else {
      System.out.println("Insufficient fileText");
    }
  }
}

public class Main {

  public static void main(String[] args) {
    File file = new File();
    file.initialiseText(1000);
    ThreadSafeTransaction thread1 = new ThreadSafeTransaction();
    ThreadSafeTransaction thread2 = new ThreadSafeTransaction();
    thread1.Deposit(file, 500);
    System.out.println("fileText: " + file.getfileText());
    thread2.Withdraw(file, 700);
    System.out.println("fileText: " + file.getfileText());
  }
}
