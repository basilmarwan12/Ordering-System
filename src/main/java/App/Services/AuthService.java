package App.Services;

import App.DTOS.AuthResponse;
import App.DTOS.LoginRequest;
import App.DTOS.RegisterRequest;
import App.Middlewares.Auth.EmailAlreadyExistsException;
import App.Middlewares.Auth.InvalidCredentialsException;
import App.Middlewares.Auth.PhoneNumberAlreadyExistsException;
import App.Models.User;
import App.Repositories.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    public User register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new PhoneNumberAlreadyExistsException(
                    "Phone number already exists"
            );
        }

        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setBirthday(request.getBirthday());

        user.setRole("CUSTOMER");

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        return userRepository.saveAndFlush(user);

    }

    public AuthResponse login(LoginRequest request) {

        // Find user
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );


        // Check password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }


        // Token lifetime
        Instant now = Instant.now();
        long expirySeconds = 50000; // 15 minutes
        Instant expiresAt = now.plusSeconds(expirySeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ordering-system")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim(
                        "userId",
                        user.getUuid().toString()
                )
                .claim(
                        "role",
                        user.getRole()
                )
                .build();


        // Generate signed JWT
        String accessToken =
                jwtEncoder
                        .encode(
                                JwtEncoderParameters.from(claims)
                        )
                        .getTokenValue();


        return new AuthResponse(
                accessToken,
                "Bearer",
                expirySeconds,
                expiresAt
        );
    }
}