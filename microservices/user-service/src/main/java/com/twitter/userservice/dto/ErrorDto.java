package com.twitter.userservice.dto;

import java.time.Instant;

public class ErrorDto {
    private int status;
    private String message;
    private Instant timestamp;

    public ErrorDto() {}

    public ErrorDto(int status, String message, Instant timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
