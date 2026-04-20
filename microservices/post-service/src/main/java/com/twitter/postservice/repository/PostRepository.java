package com.twitter.postservice.repository;

import com.twitter.postservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Post> findByAuthorAuth0IdOrderByCreatedAtDesc(String auth0Id);
}
