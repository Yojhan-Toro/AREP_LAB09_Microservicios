package com.twitter.monolith.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.twitter.monolith.dto.StreamDto;
import com.twitter.monolith.service.StreamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/stream")
@Tag(name = "Stream", description = "Public global post feed")
public class StreamController {

    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping
    @Operation(
        summary = "Get the public stream (no auth required)",
        description = "Returns a paginated list of all posts ordered by newest first.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Stream retrieved successfully")
        }
    )
    public ResponseEntity<StreamDto> getStream(
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Number of posts per page", example = "20")
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(streamService.getPublicStream(page, size));
    }
}
