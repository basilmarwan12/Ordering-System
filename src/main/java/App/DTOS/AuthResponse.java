package App.DTOS;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class AuthResponse {


    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private Instant expiresAt;
}