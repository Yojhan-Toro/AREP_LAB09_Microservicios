package com.twitter.streamservice.controller;

import com.twitter.streamservice.dto.StreamDto;
import com.twitter.streamservice.service.StreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping
    public ResponseEntity<StreamDto> getStream(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(streamService.getPublicStream(page, size));
    }
}
