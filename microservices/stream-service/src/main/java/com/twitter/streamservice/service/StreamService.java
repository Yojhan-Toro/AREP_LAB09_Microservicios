package com.twitter.streamservice.service;

import com.twitter.streamservice.dto.PostDto;
import com.twitter.streamservice.dto.StreamDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

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
