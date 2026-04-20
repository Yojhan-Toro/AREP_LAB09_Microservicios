package com.twitter.postservice.service;

import com.twitter.postservice.dto.PostDto;
import com.twitter.postservice.entity.Post;
import com.twitter.postservice.entity.User;
import com.twitter.postservice.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostDto.Response createPost(String content, User author) {
        Post post = new Post();
        post.setContent(content);
        post.setAuthor(author);
        return toDto(postRepository.save(post));
    }

    @Transactional(readOnly = true)
    public Page<PostDto.Response> getStream(int page, int size) {
        return postRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public PostDto.Response getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post not found: " + id));
        return toDto(post);
    }

    @Transactional(readOnly = true)
    public List<PostDto.Response> getPostsByUser(String auth0Id) {
        return postRepository
                .findByAuthorAuth0IdOrderByCreatedAtDesc(auth0Id)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private PostDto.Response toDto(Post post) {
        return new PostDto.Response(
                post.getId(),
                post.getContent(),
                post.getAuthor().getUsername(),
                post.getAuthor().getAuth0Id(),
                post.getAuthor().getPictureUrl(),
                post.getCreatedAt()
        );
    }
}
