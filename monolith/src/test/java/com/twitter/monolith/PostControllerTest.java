package com.twitter.monolith;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.monolith.dto.PostDto;
import com.twitter.monolith.entity.Post;
import com.twitter.monolith.entity.User;
import com.twitter.monolith.repository.PostRepository;
import com.twitter.monolith.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setAuth0Id("auth0|testuser123");
        user.setEmail("test@example.com");
        user.setUsername("testuser");

testUser = userRepository.save(user);
    }

    /* ── Public endpoints ── */

    @Test
    @DisplayName("GET /api/posts - returns empty list when no posts exist")
    void getAllPosts_empty() throws Exception {
        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/posts - returns existing posts without auth")
    void getAllPosts_returnsPosts() throws Exception {
        Post post = new Post();
        post.setContent("Hello world!");
        post.setAuthor(testUser);

        postRepository.save(post);

        mockMvc.perform(get("/api/posts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].content", is("Hello world!")))
            .andExpect(jsonPath("$[0].authorUsername", is("testuser")));
    }

    @Test
    @DisplayName("GET /api/posts/{id} - returns 404 for nonexistent post")
    void getPost_notFound() throws Exception {
        mockMvc.perform(get("/api/posts/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    @DisplayName("GET /api/stream - returns paginated stream")
    void getStream_paginated() throws Exception {
        for (int i = 0; i < 5; i++) {
            Post post = new Post();
            post.setContent("Post number " + i);
            post.setAuthor(testUser);

            postRepository.save(post);
        }

        mockMvc.perform(get("/api/stream").param("page", "0").param("size", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.posts", hasSize(3)))
            .andExpect(jsonPath("$.totalElements", is(5)))
            .andExpect(jsonPath("$.totalPages", is(2)))
            .andExpect(jsonPath("$.hasNext", is(true)));
    }

    /* ── Protected endpoints ── */

    @Test
    @DisplayName("POST /api/posts - returns 401 without auth")
    void createPost_unauthorized() throws Exception {
        PostDto.CreateRequest req = new PostDto.CreateRequest("Hello!");
        mockMvc.perform(post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/posts - creates post with valid JWT")
    void createPost_withValidJwt() throws Exception {
        PostDto.CreateRequest req = new PostDto.CreateRequest("My first tweet!");

        mockMvc.perform(post("/api/posts")
                .with(jwt().jwt(buildJwt("auth0|testuser123", "testuser", "test@example.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.content", is("My first tweet!")))
            .andExpect(jsonPath("$.authorUsername", is("testuser")));
    }

    @Test
    @DisplayName("POST /api/posts - returns 400 if content exceeds 140 characters")
    void createPost_tooLong() throws Exception {
        String longContent = "a".repeat(141);
        PostDto.CreateRequest req = new PostDto.CreateRequest(longContent);

        mockMvc.perform(post("/api/posts")
                .with(jwt().jwt(buildJwt("auth0|testuser123", "testuser", "test@example.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("POST /api/posts - returns 400 if content is blank")
    void createPost_blank() throws Exception {
        PostDto.CreateRequest req = new PostDto.CreateRequest("   ");

        mockMvc.perform(post("/api/posts")
                .with(jwt().jwt(buildJwt("auth0|testuser123", "testuser", "test@example.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/me - returns 401 without auth")
    void getMe_unauthorized() throws Exception {
        mockMvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/me - returns user profile with valid JWT")
    void getMe_withValidJwt() throws Exception {
        mockMvc.perform(get("/api/me")
                .with(jwt().jwt(buildJwt("auth0|testuser123", "testuser", "test@example.com"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.auth0Id", is("auth0|testuser123")))
            .andExpect(jsonPath("$.username", is("testuser")))
            .andExpect(jsonPath("$.email", is("test@example.com")));
    }

    @Test
    @DisplayName("GET /api/me - provisions new user on first login")
    void getMe_jitProvisioning() throws Exception {
        mockMvc.perform(get("/api/me")
                .with(jwt().jwt(buildJwt("auth0|newuser999", "newuser", "new@example.com"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.auth0Id", is("auth0|newuser999")))
            .andExpect(jsonPath("$.username", is("newuser")));
    }

    /* ── helper ── */

    private Jwt buildJwt(String sub, String nickname, String email) {
        return Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject(sub)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claim("nickname", nickname)
            .claim("email", email)
            .claim("name", nickname)
            .build();
    }
}
