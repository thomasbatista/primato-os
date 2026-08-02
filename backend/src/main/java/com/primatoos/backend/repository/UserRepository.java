package com.primatoos.backend.repository;

import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Page<User> findByRole(UserRole role, Pageable pageable);
}
