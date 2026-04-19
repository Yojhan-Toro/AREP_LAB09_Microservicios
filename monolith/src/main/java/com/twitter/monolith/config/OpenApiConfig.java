package com.twitter.monolith.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Value("${auth0.domain}")
    private String auth0Domain;

    @Value("${auth0.audience}")
    private String auth0Audience;

    @Bean
    public OpenAPI openAPI() {
        String auth0BaseUrl = "https://" + auth0Domain;

        OAuthFlow authCodeFlow = new OAuthFlow()
            .authorizationUrl(auth0BaseUrl + "/authorize?audience=" + auth0Audience)
            .tokenUrl(auth0BaseUrl + "/oauth/token")
            .scopes(new Scopes()
                .addString("openid", "OpenID Connect")
                .addString("profile", "User profile")
                .addString("email", "User email")
                .addString("read:posts", "Read posts")
                .addString("write:posts", "Create posts")
                .addString("read:profile", "Read own profile")
            );

        SecurityScheme bearerScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Paste a valid JWT access token issued by Auth0");

        SecurityScheme oauth2Scheme = new SecurityScheme()
            .type(SecurityScheme.Type.OAUTH2)
            .description("Auth0 OAuth2 Authorization Code + PKCE")
            .flows(new OAuthFlows().authorizationCode(authCodeFlow));

        return new OpenAPI()
            .info(new Info()
                .title("Twitter-like Monolith API")
                .version("1.0.0")
                .description("""
                    A simplified Twitter-like RESTful API built with Spring Boot and secured with Auth0.
                    
                    **Authentication:** All write operations require a valid JWT Bearer token issued by Auth0.
                    
                    **Public endpoints:** `GET /api/stream` and `GET /api/posts/**` are publicly accessible.
                    
                    **Scopes:** `read:posts`, `write:posts`, `read:profile`
                    """)
                .contact(new Contact().name("Twitter Monolith Team"))
                .license(new License().name("MIT"))
            )
            .components(new Components()
                .addSecuritySchemes("BearerAuth", bearerScheme)
                .addSecuritySchemes("OAuth2", oauth2Scheme)
            )
            .security(List.of(
                new SecurityRequirement().addList("BearerAuth"),
                new SecurityRequirement().addList("OAuth2")
            ));
    }
}
