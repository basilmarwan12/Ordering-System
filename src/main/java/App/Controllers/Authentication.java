package App.Controllers;

import App.DTOS.AuthResponse;
import App.DTOS.LoginRequest;
import App.DTOS.RegisterRequest;
import App.Models.User;
import App.Services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/auth")
public class Authentication {

    final private AuthService authService;

    public Authentication(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity
                .ok(authService.login(request));
    }
}
