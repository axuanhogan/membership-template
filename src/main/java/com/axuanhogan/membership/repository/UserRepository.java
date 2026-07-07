package com.axuanhogan.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.axuanhogan.membership.dao.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByActivationToken(String token);
}
