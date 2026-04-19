package com.twitter.monolith.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.twitter.monolith.dto.PostDto;
import com.twitter.monolith.entity.Post;
import com.twitter.monolith.entity.User;
import com.twitter.monolith.repository.PostRepository;

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

        Post saved = postRepository.save(post);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<PostDto.Response> getStream(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<PostDto.Response> getPostsByUser(String auth0Id) {
        return postRepository
                .findByAuthorAuth0IdOrderByCreatedAtDesc(auth0Id)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostDto.Response getPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() ->
                        new jakarta.persistence.EntityNotFoundException("Post not found: " + id));
        return toDto(post);
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