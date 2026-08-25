package App.Config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;

import org.springframework.security.oauth2.jwt.JwtDecoder;

import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import java.util.UUID;


@Configuration
public class AuthorizationServerConfig {

    // =====================================================
    // AUTHORIZATION SERVER SECURITY CHAIN
    // =====================================================

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
                .securityMatcher(
                        authorizationServerConfigurer.getEndpointsMatcher()
                )

                .with(
                        authorizationServerConfigurer,
                        authorizationServer ->
                                authorizationServer
                                        .oidc(Customizer.withDefaults())
                )

                .authorizeHttpRequests(auth ->
                        auth.anyRequest().authenticated()
                );

        return http.build();
    }


    // =====================================================
    // REGISTERED OAUTH CLIENT
    // =====================================================

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            PasswordEncoder passwordEncoder
    ) {

        RegisteredClient orderingClient =
                RegisteredClient
                        .withId(UUID.randomUUID().toString())

                        // Client identifier
                        .clientId("ordering-client")

                        // Client secret
                        .clientSecret(
                                passwordEncoder.encode("ordering-secret")
                        )

                        // How the client authenticates itself
                        .clientAuthenticationMethod(
                                ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                        )

                        // OAuth grant types
                        .authorizationGrantType(
                                AuthorizationGrantType.AUTHORIZATION_CODE
                        )

                        .authorizationGrantType(
                                AuthorizationGrantType.REFRESH_TOKEN
                        )

                        // Callback URL after authorization
                        .redirectUri(
                                "http://127.0.0.1:8080/login/oauth2/code/ordering-client"
                        )

                        // OAuth / OIDC scopes
                        .scope(OidcScopes.OPENID)
                        .scope(OidcScopes.PROFILE)
                        .scope("orders.read")
                        .scope("orders.write")

                        .clientSettings(
                                ClientSettings
                                        .builder()
                                        .requireAuthorizationConsent(true)
                                        .build()
                        )

                        .build();

        return new InMemoryRegisteredClientRepository(
                orderingClient
        );
    }

    // =====================================================
    // GENERATE RSA KEY
    // =====================================================

    private static RSAKey generateRsaKey() {

        KeyPair keyPair = generateRsaKeyPair();

        RSAPublicKey publicKey =
                (RSAPublicKey) keyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }


    private static KeyPair generateRsaKeyPair() {

        try {

            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to generate RSA key pair",
                    exception
            );
        }
    }
}