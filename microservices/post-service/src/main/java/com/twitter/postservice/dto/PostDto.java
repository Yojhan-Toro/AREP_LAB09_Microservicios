package com.twitter.postservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class PostDto {

    public static class CreateRequest {

        @NotBlank(message = "Content must not be blank")
        @Size(max = 140, message = "Content must not exceed 140 characters")
        private String content;

        public CreateRequest() {}

        public CreateRequest(String content) { this.content = content; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class Response {
        private Long id;
        private String content;
        private String authorUsername;
        private String authorId;
        private String authorPicture;
        private Instant createdAt;

        public Response() {}

        public Response(Long id, String content, String authorUsername,
                        String authorId, String authorPicture, Instant createdAt) {
            this.id = id;
            this.content = content;
            this.authorUsername = authorUsername;
            this.authorId = authorId;
            this.authorPicture = authorPicture;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getAuthorUsername() { return authorUsername; }
        public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }

        public String getAuthorPicture() { return authorPicture; }
        public void setAuthorPicture(String authorPicture) { this.authorPicture = authorPicture; }

        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
}
