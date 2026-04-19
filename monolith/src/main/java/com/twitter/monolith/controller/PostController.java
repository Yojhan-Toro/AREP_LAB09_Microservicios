package com.twitter.monolith.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.twitter.monolith.dto.ErrorDto;
import com.twitter.monolith.dto.PostDto;
import com.twitter.monolith.entity.User;
import com.twitter.monolith.service.PostService;
import com.twitter.monolith.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/posts")
//@RequiredArgsConstructor
@Tag(name = "Posts", description = "Create and retrieve posts")
public class PostController {

    private final PostService postService;
    private final UserService userService;

    public PostController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }    

    /* ── GET /api/posts (public) ── */
    @GetMapping
    @Operation(
        summary = "Get all posts (no auth required)",
        description = "Returns the full list of posts without pagination. Use /api/stream for paginated access.",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of posts retrieved successfully")
        }
    )
    public ResponseEntity<List<PostDto.Response>> getAllPosts() {
        var page = postService.getStream(0, Integer.MAX_VALUE);
        return ResponseEntity.ok(page.getContent());
    }

    /* ── GET /api/posts/{id} (public) ── */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get a single post by ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Post found"),
            @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content(schema = @Schema(implementation = ErrorDto.class)))
        }
    )
    public ResponseEntity<PostDto.Response> getPost(
        @Parameter(description = "Post ID", required = true) @PathVariable Long id
    ) {
        return ResponseEntity.ok(postService.getPost(id));
    }

    /* ── POST /api/posts (protected) ── */
    @PostMapping
    @Operation(
        summary = "Create a new post (auth required)",
        description = "Creates a post of up to 140 characters. Requires a valid Auth0 JWT with `write:posts` scope.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "Post created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                content = @Content(schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid token")
        }
    )
    public ResponseEntity<PostDto.Response> createPost(
        @Valid @RequestBody PostDto.CreateRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        User author = userService.getOrCreateUser(jwt);
        PostDto.Response response = postService.createPost(request.getContent(), author);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ── GET /api/posts/user/{auth0Id} (public) ── */
    @GetMapping("/user/{auth0Id}")
    @Operation(
        summary = "Get all posts by a specific user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully")
        }
    )
    public ResponseEntity<List<PostDto.Response>> getPostsByUser(
        @Parameter(description = "Auth0 subject ID of the user") @PathVariable String auth0Id
    ) {
        return ResponseEntity.ok(postService.getPostsByUser(auth0Id));
    }
}
