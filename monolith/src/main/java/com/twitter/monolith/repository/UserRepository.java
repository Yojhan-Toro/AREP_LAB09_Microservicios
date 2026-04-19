package com.twitter.monolith.repository;

import com.twitter.monolith.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAuth0Id(String auth0Id);
    boolean existsByAuth0Id(String auth0Id);
}
