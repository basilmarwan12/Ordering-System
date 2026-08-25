package App.Config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class JwtConfig {

    @Bean
    public ImmutableJWKSet<SecurityContext> jwkSource() {

        RSAKey rsaKey = generateRsa();

        JWKSet jwkSet = new JWKSet(rsaKey);

        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(
            ImmutableJWKSet<SecurityContext> jwkSource
    ) {

        return OAuth2AuthorizationServerConfiguration
                .jwtDecoder(jwkSource);
    }

    private static RSAKey generateRsa() {

        KeyPair keyPair = generateRsaKey();

        RSAPublicKey publicKey =
                (RSAPublicKey) keyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKey() {

        try {

            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            return generator.generateKeyPair();

        } catch (Exception ex) {

            throw new IllegalStateException(ex);
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(
            ImmutableJWKSet<SecurityContext> jwkSource
    ) {
        return new NimbusJwtEncoder(jwkSource);
    }
}