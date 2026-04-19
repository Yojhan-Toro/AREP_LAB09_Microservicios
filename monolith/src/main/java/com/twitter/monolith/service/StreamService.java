package com.twitter.monolith.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.twitter.monolith.dto.PostDto;
import com.twitter.monolith.dto.StreamDto;

@Service
public class StreamService {

    private final PostService postService;

    public StreamService(PostService postService) {
        this.postService = postService;
    }

    public StreamDto getPublicStream(int page, int size) {
        Page<PostDto.Response> pageResult = postService.getStream(page, size);

        return new StreamDto(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.hasNext()
        );
    }
}