package App.Middlewares;

import App.Middlewares.Auth.EmailAlreadyExistsException;
import App.Middlewares.Auth.InvalidCredentialsException;
import App.Middlewares.Auth.PhoneNumberAlreadyExistsException;
import App.Middlewares.Auth.UserNotFoundException;
import App.Middlewares.Orders.InvalidOrderAmountsException;
import App.Middlewares.Orders.OrderNotFoundException;
import App.Middlewares.Products.ProductNotFoundException;
import App.Middlewares.Users.SelfDeletionNotAllowedException;
import App.Middlewares.Users.UserHasOrdersException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException exception
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Validation failed");
        response.put("errors", errors);

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(
            EmailAlreadyExistsException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 409);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
            InvalidCredentialsException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 401);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(PhoneNumberAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handlePhoneAlreadyExists(
            PhoneNumberAlreadyExistsException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 409);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleMethodValidation(
            HandlerMethodValidationException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 400);
        response.put("message", "Validation failed");

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBody(
            HttpMessageNotReadableException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 400);
        response.put(
                "message",
                "Request body is missing or invalid"
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }


    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFound(
            OrderNotFoundException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 404);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(InvalidOrderAmountsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOrderAmounts(
            InvalidOrderAmountsException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 400);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 400);
        response.put(
                "message",
                "Invalid value for '" + exception.getName() + "'"
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(SelfDeletionNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleSelfDeletion(
            SelfDeletionNotAllowedException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 409);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(UserHasOrdersException.class)
    public ResponseEntity<Map<String, Object>> handleUserHasOrders(
            UserHasOrdersException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 409);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    // Must be handled explicitly: otherwise the Exception handler below
    // swallows the denial from @PreAuthorize and reports it as a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 401);
        response.put("message", "Unauthorized");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception exception
    ) {
        exception.printStackTrace();
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 500);
        response.put("message", "Internal server error");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }


    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(
            UserNotFoundException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 404);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFound(
            UsernameNotFoundException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 401);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFoundException(
            ProductNotFoundException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 404);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
}