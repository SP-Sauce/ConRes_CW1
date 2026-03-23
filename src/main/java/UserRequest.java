public class UserRequest {
    private final User user;
    private final String action;

    public UserRequest(User user, String action) {
        this.user = user;
        this.action = action;
    }

    public User getUser() {
        return user;
    }

    public String getAction() {
        return action;
    }
}