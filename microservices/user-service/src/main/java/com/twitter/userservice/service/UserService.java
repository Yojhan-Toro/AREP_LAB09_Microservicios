package com.twitter.userservice.service;

import com.twitter.userservice.dto.PostDto;
import com.twitter.userservice.dto.UserDto;
import com.twitter.userservice.entity.User;
import com.twitter.userservice.repository.PostRepository;
import com.twitter.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public UserService(UserRepository userRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public User getOrCreateUser(Jwt jwt) {
        String auth0Id = jwt.getSubject();
        return userRepository.findByAuth0Id(auth0Id).orElseGet(() -> {
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

    @Transactional(readOnly = true)
    public List<PostDto.Response> getPostsByUser(String auth0Id) {
        return postRepository
                .findByAuthorAuth0IdOrderByCreatedAtDesc(auth0Id)
                .stream()
                .map(p -> new PostDto.Response(
                        p.getId(),
                        p.getContent(),
                        p.getAuthor().getUsername(),
                        p.getAuthor().getAuth0Id(),
                        p.getAuthor().getPictureUrl(),
                        p.getCreatedAt()
                ))
                .toList();
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
