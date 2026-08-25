package App.Middlewares;

import App.Middlewares.Auth.EmailAlreadyExistsException;
import App.Middlewares.Auth.InvalidCredentialsException;
import App.Middlewares.Auth.PhoneNumberAlreadyExistsException;
import App.Middlewares.Auth.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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


    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(
            UserNotFoundException exception
    ) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", 409);
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
}