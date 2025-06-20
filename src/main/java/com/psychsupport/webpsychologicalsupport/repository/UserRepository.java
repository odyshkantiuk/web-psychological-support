package com.psychsupport.webpsychologicalsupport.repository;

import com.psychsupport.webpsychologicalsupport.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByRole(User.Role role);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRoleAndIsVerifiedTrue(User.Role role);
    List<User> findByRoleAndIsVerifiedFalse(User.Role role);
    List<User> findTop5ByOrderByCreatedAtDesc();
}