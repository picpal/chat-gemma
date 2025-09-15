package com.chatgemma.repository;

import com.chatgemma.entity.User;
import com.chatgemma.entity.User.Role;
import com.chatgemma.entity.User.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByStatus(Status status);

    List<User> findByRole(Role role);

    List<User> findByStatusAndRole(Status status, Role role);

    @Query("SELECT u FROM User u WHERE u.status = 'PENDING' ORDER BY u.createdAt ASC")
    List<User> findPendingUsersOrderByCreatedAt();

    boolean existsByUsernameOrEmail(String username, String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'APPROVED'")
    long countActiveUsers();

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    long countByStatus(Status status);
}