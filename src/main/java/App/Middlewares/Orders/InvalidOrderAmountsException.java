package App.Middlewares.Orders;

public class InvalidOrderAmountsException extends RuntimeException {

    public InvalidOrderAmountsException(String message) {
        super(message);
    }
}
