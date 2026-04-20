package com.twitter.userservice.repository;

import com.twitter.userservice.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByAuthorAuth0IdOrderByCreatedAtDesc(String auth0Id);
}
