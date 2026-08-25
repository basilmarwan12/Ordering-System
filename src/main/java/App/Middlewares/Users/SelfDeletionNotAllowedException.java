package App.Middlewares.Users;

public class SelfDeletionNotAllowedException extends RuntimeException {
    public SelfDeletionNotAllowedException(String message) {
        super(message);
    }
}
