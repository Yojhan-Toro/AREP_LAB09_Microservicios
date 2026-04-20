package com.twitter.streamservice.dto;

import java.util.List;

public class StreamDto {
    private List<PostDto.Response> posts;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    public StreamDto() {}

    public StreamDto(List<PostDto.Response> posts, int page, int size,
                     long totalElements, int totalPages, boolean hasNext) {
        this.posts = posts;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }

    public List<PostDto.Response> getPosts() { return posts; }
    public void setPosts(List<PostDto.Response> posts) { this.posts = posts; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
}
