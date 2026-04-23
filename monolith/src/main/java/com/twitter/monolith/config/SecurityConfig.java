package com.twitter.monolith.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${auth0.audience}")
    private String audience;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${auth0.local-mode:true}")
    private boolean localMode;

    /* ───────────── Security filter chain ───────────── */
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtDecoder jwtDecoder,
        JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Static frontend
                .requestMatchers("/", "/index.html", "/static/**", "/*.js", "/*.css", "/*.ico").permitAll()
                // Public: read stream & posts
                .requestMatchers(HttpMethod.GET, "/api/stream", "/api/posts", "/api/posts/**").permitAll()
                // Swagger UI & API docs
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**").permitAll()
                // H2 console (dev only)
                .requestMatchers("/h2-console/**").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter)
                )
            )
            // Allow H2 console frames in dev
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin()));

        return http.build();
    }

    /* ───────────── JWT decoder with audience validation ───────────── */
    @Bean
    public JwtDecoder jwtDecoder() {
        if (localMode) {
            return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("local-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("permissions", List.of())
                .claim("scope", "")
                .build();
        }

        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> combined = new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);

        decoder.setJwtValidator(combined);
        return decoder;
    }

    /* ───────────── Map Auth0 permissions → Spring Security authorities ───────────── */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        // Auth0 stores scopes in "scope" claim and RBAC permissions in "permissions"
        converter.setAuthoritiesClaimName("permissions");
        converter.setAuthorityPrefix("PERMISSION_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var scopeAuthorities = new JwtGrantedAuthoritiesConverter();
            scopeAuthorities.setAuthorityPrefix("SCOPE_");
            var permissionAuthorities = new JwtGrantedAuthoritiesConverter();
            permissionAuthorities.setAuthoritiesClaimName("permissions");
            permissionAuthorities.setAuthorityPrefix("PERMISSION_");

            var authorities = new java.util.ArrayList<>(scopeAuthorities.convert(jwt));
            authorities.addAll(permissionAuthorities.convert(jwt));
            return authorities;
        });

        return jwtConverter;
    }

    /* ───────────── CORS (allow frontend on S3 / localhost) ───────────── */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // Restrict to your S3 URL in production
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
