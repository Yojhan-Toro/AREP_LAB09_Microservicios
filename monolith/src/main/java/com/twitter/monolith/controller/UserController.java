package com.twitter.monolith.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.twitter.monolith.dto.ErrorDto;
import com.twitter.monolith.dto.PostDto;
import com.twitter.monolith.dto.UserDto;
import com.twitter.monolith.entity.User;
import com.twitter.monolith.service.PostService;
import com.twitter.monolith.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
//RequiredArgsConstructor
@Tag(name = "User", description = "Authenticated user profile and own posts")
public class UserController {

    private final UserService userService;
    private final PostService postService;

    public UserController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }


    /**
     * GET /api/me — returns the profile of the currently authenticated user.
     * Performs JIT provisioning: if the user has never logged in before,
     * a local record is created from the JWT claims.
     */
    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile (auth required)",
        description = """
            Returns the profile of the authenticated user based on the JWT sub claim.
            On first call, the user is automatically provisioned in the local database
            using the email, nickname, and picture claims from the Auth0 token.
            """,
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – missing or invalid token",
                content = @Content(schema = @Schema(implementation = ErrorDto.class)))
        }
    )
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUser(jwt);
        return ResponseEntity.ok(userService.toDto(user));
    }

    /**
     * GET /api/me/posts — returns all posts created by the current authenticated user.
     */
    @GetMapping("/me/posts")
    @Operation(
        summary = "Get own posts (auth required)",
        description = "Returns all posts created by the currently authenticated user, ordered newest first.",
        security = @SecurityRequirement(name = "BearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(schema = @Schema(implementation = ErrorDto.class)))
        }
    )
    public ResponseEntity<List<PostDto.Response>> getMyPosts(@AuthenticationPrincipal Jwt jwt) {
        List<PostDto.Response> posts = postService.getPostsByUser(jwt.getSubject());
        return ResponseEntity.ok(posts);
    }
}
