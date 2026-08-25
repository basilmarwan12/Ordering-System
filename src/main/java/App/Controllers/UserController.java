package App.Controllers;

import App.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
