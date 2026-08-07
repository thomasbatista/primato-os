package com.primatoos.backend.repository;

import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u "
            + "WHERE (:role IS NULL OR u.role = :role) "
            + "AND (:unlinked = false OR u.id NOT IN (SELECT w.user.id FROM Worker w WHERE w.user IS NOT NULL))")
    Page<User> search(@Param("role") UserRole role, @Param("unlinked") boolean unlinked, Pageable pageable);
}
