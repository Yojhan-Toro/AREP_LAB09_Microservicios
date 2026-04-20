package com.twitter.postservice.controller;

import com.twitter.postservice.dto.PostDto;
import com.twitter.postservice.entity.User;
import com.twitter.postservice.service.PostService;
import com.twitter.postservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    public PostController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<PostDto.Response>> getAllPosts() {
        return ResponseEntity.ok(postService.getStream(0, Integer.MAX_VALUE).getContent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDto.Response> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    @PostMapping
    public ResponseEntity<PostDto.Response> createPost(
            @Valid @RequestBody PostDto.CreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        User author = userService.getOrCreateUser(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request.getContent(), author));
    }

    @GetMapping("/user/{auth0Id}")
    public ResponseEntity<List<PostDto.Response>> getPostsByUser(@PathVariable String auth0Id) {
        return ResponseEntity.ok(postService.getPostsByUser(auth0Id));
    }
}
