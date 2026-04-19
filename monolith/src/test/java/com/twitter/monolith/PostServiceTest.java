package com.twitter.monolith;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.twitter.monolith.dto.PostDto;
import com.twitter.monolith.entity.Post;
import com.twitter.monolith.entity.User;
import com.twitter.monolith.repository.PostRepository;
import com.twitter.monolith.service.PostService;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @InjectMocks PostService postService;

    private User author;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(1L);
        author.setAuth0Id("auth0|abc");
        author.setUsername("testuser");
        author.setEmail("test@example.com");
    }

    @Test
    @DisplayName("createPost - saves and returns mapped DTO")
    void createPost_success() {
        Post saved = new Post();
        saved.setId(1L);
        saved.setContent("Hello!");
        saved.setAuthor(author);

        when(postRepository.save(any(Post.class))).thenReturn(saved);

        PostDto.Response result = postService.createPost("Hello!", author);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("Hello!");
        assertThat(result.getAuthorUsername()).isEqualTo("testuser");
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("getStream - returns page mapped to DTOs")
    void getStream_returnsMappedPage() {
        Post post = new Post();
        post.setId(1L);
        post.setContent("Tweet");
        post.setAuthor(author);

        Page<Post> page = new PageImpl<>(List.of(post), PageRequest.of(0, 20), 1);
        when(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);

        Page<PostDto.Response> result = postService.getStream(0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Tweet");
    }

    @Test
    @DisplayName("getPost - throws EntityNotFoundException when not found")
    void getPost_notFound() {
        when(postRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(99L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getPostsByUser - returns posts filtered by auth0Id")
    void getPostsByUser_success() {
        Post post = new Post();
        post.setId(2L);
        post.setContent("My post");
        post.setAuthor(author);

        when(postRepository.findByAuthorAuth0IdOrderByCreatedAtDesc("auth0|abc"))
            .thenReturn(List.of(post));

        List<PostDto.Response> result = postService.getPostsByUser("auth0|abc");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthorId()).isEqualTo("auth0|abc");
    }
}