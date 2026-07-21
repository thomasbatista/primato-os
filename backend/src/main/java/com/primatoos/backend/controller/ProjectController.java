package com.primatoos.backend.controller;

import com.primatoos.backend.dto.ProjectCreateRequest;
import com.primatoos.backend.dto.ProjectResponse;
import com.primatoos.backend.dto.ProjectUpdateRequest;
import com.primatoos.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@PreAuthorize("hasRole('MANAGER')")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(projectService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ProjectUpdateRequest request) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        projectService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
