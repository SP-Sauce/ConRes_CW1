public class UserSession extends Thread{
  private final User user;
  private volatile boolean active = true;

  public UserSession(User user){
    this.user = user;
    setName("UserSession - " + user.getId());
  }
  public User getUser(){
    return user;
  }

  public void terminate(){
    active = false;
    interrupt();
  }

  @Override
  public void run(){
    System.out.println(user + "Session Thread started as" + getName());
    try{
      while(active){
        Thread.sleep(1000);
      }
    } catch (InterruptedException e){
      Thread.currentThread().interrupt();
    }finally{
      System.out.println(user + "Session Thread ended");
    }
    }
}