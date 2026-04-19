package com.twitter.monolith.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.twitter.monolith.dto.UserDto;
import com.twitter.monolith.entity.User;
import com.twitter.monolith.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the local User for the given JWT, creating one on first login (JIT provisioning).
     */
    @Transactional
    public User getOrCreateUser(Jwt jwt) {
        String auth0Id = jwt.getSubject();

        return userRepository.findByAuth0Id(auth0Id)
                .orElseGet(() -> {
                    log.info("First login for auth0Id={}, provisioning user", auth0Id);

                    User user = new User();
                    user.setAuth0Id(auth0Id);
                    user.setEmail(getClaimOrDefault(jwt, "email", auth0Id + "@unknown.com"));
                    user.setUsername(buildUsername(jwt));
                    user.setPictureUrl(jwt.getClaimAsString("picture"));

                    return userRepository.save(user);
                });
    }

    @Transactional(readOnly = true)
    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getAuth0Id(),
                user.getEmail(),
                user.getUsername(),
                user.getPictureUrl(),
                user.getCreatedAt(),
                user.getPosts().size()
        );
    }

    private String buildUsername(Jwt jwt) {
        String nickname = jwt.getClaimAsString("nickname");
        if (nickname != null && !nickname.isBlank()) return nickname;

        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) return name.replace(" ", "_").toLowerCase();

        String email = jwt.getClaimAsString("email");
        if (email != null && email.contains("@")) return email.split("@")[0];

        return "user_" + jwt.getSubject().replace("|", "_");
    }

    private String getClaimOrDefault(Jwt jwt, String claim, String defaultValue) {
        String value = jwt.getClaimAsString(claim);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}