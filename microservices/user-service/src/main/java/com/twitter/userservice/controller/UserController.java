package com.twitter.userservice.controller;

import com.twitter.userservice.dto.PostDto;
import com.twitter.userservice.dto.UserDto;
import com.twitter.userservice.entity.User;
import com.twitter.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUser(jwt);
        return ResponseEntity.ok(userService.toDto(user));
    }

    @GetMapping("/me/posts")
    public ResponseEntity<List<PostDto.Response>> getMyPosts(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getPostsByUser(jwt.getSubject()));
    }
}
