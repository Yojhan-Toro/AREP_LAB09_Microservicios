package com.twitter.streamservice.service;

import com.twitter.streamservice.dto.PostDto;
import com.twitter.streamservice.entity.Post;
import com.twitter.streamservice.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public Page<PostDto.Response> getStream(int page, int size) {
        return postRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toDto);
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
