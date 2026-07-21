package com.primatoos.backend.controller;

import com.primatoos.backend.dto.worker.WorkerCreateRequest;
import com.primatoos.backend.dto.worker.WorkerResponse;
import com.primatoos.backend.dto.worker.WorkerUpdateRequest;
import com.primatoos.backend.service.WorkerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workers")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @PostMapping
    public ResponseEntity<WorkerResponse> create(@Valid @RequestBody WorkerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workerService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<WorkerResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(workerService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(workerService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkerResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody WorkerUpdateRequest request) {
        return ResponseEntity.ok(workerService.update(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<WorkerResponse> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(workerService.deactivate(id));
    }
}
