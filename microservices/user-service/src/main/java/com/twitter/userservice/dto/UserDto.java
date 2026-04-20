package com.twitter.userservice.dto;

import java.time.Instant;

public class UserDto {
    private Long id;
    private String auth0Id;
    private String email;
    private String username;
    private String pictureUrl;
    private Instant createdAt;
    private int postCount;

    public UserDto() {}

    public UserDto(Long id, String auth0Id, String email, String username,
                   String pictureUrl, Instant createdAt, int postCount) {
        this.id = id;
        this.auth0Id = auth0Id;
        this.email = email;
        this.username = username;
        this.pictureUrl = pictureUrl;
        this.createdAt = createdAt;
        this.postCount = postCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAuth0Id() { return auth0Id; }
    public void setAuth0Id(String auth0Id) { this.auth0Id = auth0Id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public int getPostCount() { return postCount; }
    public void setPostCount(int postCount) { this.postCount = postCount; }
}
