package com.primatoos.backend.repository;

import com.primatoos.backend.model.Worker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    Optional<Worker> findByUserId(Long userId);

    Page<Worker> findByActive(boolean active, Pageable pageable);
}
