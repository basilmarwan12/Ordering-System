package App.Middlewares.Users;

public class UserHasOrdersException extends RuntimeException {
    public UserHasOrdersException(String message) {
        super(message);
    }
}
