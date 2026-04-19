package com.twitter.monolith;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.twitter.monolith.entity.User;
import com.twitter.monolith.repository.UserRepository;
import com.twitter.monolith.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Test
    @DisplayName("Returns existing user when auth0Id already in DB")
    void getOrCreateUser_existingUser() {

        User existing = new User();
        existing.setAuth0Id("auth0|abc");
        existing.setEmail("existing@example.com");
        existing.setUsername("existing");

        when(userRepository.findByAuth0Id("auth0|abc")).thenReturn(Optional.of(existing));

        Jwt jwt = buildJwt("auth0|abc", "existing", "existing@example.com");
        User result = userService.getOrCreateUser(jwt);

        assertThat(result.getAuth0Id()).isEqualTo("auth0|abc");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creates new user on first login (JIT provisioning)")
    void getOrCreateUser_newUser() {
        when(userRepository.findByAuth0Id("auth0|new")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Jwt jwt = buildJwt("auth0|new", "newuser", "new@example.com");
        User result = userService.getOrCreateUser(jwt);

        assertThat(result.getAuth0Id()).isEqualTo("auth0|new");
        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Falls back to name claim when nickname is absent")
    void getOrCreateUser_usesNameAsFallback() {
        when(userRepository.findByAuth0Id("auth0|xyz")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("auth0|xyz")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("name", "John Doe")
            .claim("email", "john@example.com")
            .build();

        User result = userService.getOrCreateUser(jwt);

        assertThat(result.getUsername()).isEqualTo("john_doe");
    }

    private Jwt buildJwt(String sub, String nickname, String email) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(sub)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("nickname", nickname)
            .claim("email", email)
            .build();
    }
}