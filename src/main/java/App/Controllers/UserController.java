package App.Controllers;

import App.DTOS.RegisterRequest;
import App.DTOS.UserResponse;
import App.Models.User;
import App.Services.AuthService;
import App.Services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "User management", description = "Administrative user creation and deletion")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/admins")
    @Operation(
            summary = "Add an administrator",
            description = "Creates an ADMIN account. This operation requires an ADMIN JWT and is available in Swagger after authentication."
    )
    public ResponseEntity<UserResponse> createAdmin(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authService.register(request, "ADMIN");
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/customers")
    @Operation(
            summary = "Add a normal user",
            description = "Creates a CUSTOMER account. This operation requires an ADMIN JWT and is available in Swagger after authentication."
    )
    public ResponseEntity<UserResponse> createCustomer(
            @Valid @RequestBody RegisterRequest request
    ) {
        User user = authService.register(request, "CUSTOMER");
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal Jwt jwt
    ) {

        String actingUserId = jwt.getClaim("userId");

        userService.deleteUser(uuid, actingUserId);

        return ResponseEntity
                .noContent()
                .build();
    }
}
