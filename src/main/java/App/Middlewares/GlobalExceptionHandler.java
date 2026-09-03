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
import App.DTOS.ErrorResponseDto;
import App.Middlewares.Orders.InvalidOrderStateException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponseDto buildErrorResponse(
            HttpServletRequest request,
            HttpStatus status,
            String message
    ) {
        return new ErrorResponseDto(
                request.getRequestURI(),
                status.value(),
                message,
                LocalDateTime.now()
        );
    }

    private ErrorResponseDto buildValidationErrorResponse(
            HttpServletRequest request,
            Map<String, String> errors
    ) {
        return new ErrorResponseDto(
                request.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                LocalDateTime.now(),
                errors
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationErrors(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
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

        return ResponseEntity
                .badRequest()
                .body(buildValidationErrorResponse(request, errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseDto> handleBindException(
            BindException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity
                .badRequest()
                .body(buildValidationErrorResponse(request, errors));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailAlreadyExists(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(request, HttpStatus.CONFLICT, exception.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(request, HttpStatus.UNAUTHORIZED, exception.getMessage()));
    }

    @ExceptionHandler(PhoneNumberAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handlePhoneAlreadyExists(
            PhoneNumberAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(request, HttpStatus.CONFLICT, exception.getMessage()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : (error.getCodes() != null && error.getCodes().length > 0
                        ? error.getCodes()[0]
                        : "parameter");
            errors.put(fieldName, error.getDefaultMessage());
        });

        return ResponseEntity
                .badRequest()
                .body(buildValidationErrorResponse(request, errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            errors.put(fieldName, violation.getMessage());
        });

        return ResponseEntity
                .badRequest()
                .body(buildValidationErrorResponse(request, errors));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            ValidationException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(buildErrorResponse(request, HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(buildErrorResponse(request, HttpStatus.BAD_REQUEST, "Request body is missing or invalid"));
    }


    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleOrderNotFound(
            OrderNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(request, HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(InvalidOrderAmountsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidOrderAmounts(
            InvalidOrderAmountsException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(buildErrorResponse(request, HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidOrderState(
            InvalidOrderStateException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(buildErrorResponse(request, HttpStatus.BAD_REQUEST, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(buildErrorResponse(request, HttpStatus.BAD_REQUEST,
                        "Invalid value for '" + exception.getName() + "'"));
    }

    @ExceptionHandler(SelfDeletionNotAllowedException.class)
    public ResponseEntity<ErrorResponseDto> handleSelfDeletion(
            SelfDeletionNotAllowedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(request, HttpStatus.CONFLICT, exception.getMessage()));
    }

    @ExceptionHandler(UserHasOrdersException.class)
    public ResponseEntity<ErrorResponseDto> handleUserHasOrders(
            UserHasOrdersException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildErrorResponse(request, HttpStatus.CONFLICT, exception.getMessage()));
    }

    // Must be handled explicitly: otherwise the Exception handler below
    // swallows the denial from @PreAuthorize and reports it as a 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(request, HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(request, HttpStatus.NOT_FOUND,
                        "No resource found for path: " + request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneralException(
            Exception exception,
            HttpServletRequest request
    ) {
        exception.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUserNotFoundException(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(request, HttpStatus.NOT_FOUND, exception.getMessage()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleUsernameNotFound(
            UsernameNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildErrorResponse(request, HttpStatus.UNAUTHORIZED, exception.getMessage()));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProductNotFoundException(
            ProductNotFoundException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorResponse(request, HttpStatus.NOT_FOUND, exception.getMessage()));
    }
}