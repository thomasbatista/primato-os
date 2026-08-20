package com.primatoos.backend.controller;

import com.primatoos.backend.dto.project.ProjectCreateRequest;
import com.primatoos.backend.dto.project.ProjectPhotoResponse;
import com.primatoos.backend.dto.project.ProjectResponse;
import com.primatoos.backend.dto.project.ProjectUpdateRequest;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> findAll(@RequestParam(required = false) ProjectStatus status,
                                                           @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(projectService.findAll(status, pageable));
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

    // Workers have no other way into a Project — GET /{id} above is manager-only. The service
    // checks they are assigned to a work order in this obra before answering.
    @GetMapping("/mine/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'WORKER')")
    public ResponseEntity<ProjectResponse> findMineById(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(projectService.findByIdForCaller(id, authentication.getName()));
    }

    @PostMapping("/{id}/photos")
    public ResponseEntity<ProjectPhotoResponse> uploadPhoto(@PathVariable Long id,
                                                              @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.uploadPhoto(id, file));
    }

    @GetMapping("/{id}/photos")
    @PreAuthorize("hasAnyRole('MANAGER', 'WORKER')")
    public ResponseEntity<List<ProjectPhotoResponse>> findPhotos(Authentication authentication,
                                                                   @PathVariable Long id) {
        return ResponseEntity.ok(projectService.findPhotos(id, authentication.getName()));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        projectService.deletePhoto(id, photoId);
        return ResponseEntity.noContent().build();
    }
}
