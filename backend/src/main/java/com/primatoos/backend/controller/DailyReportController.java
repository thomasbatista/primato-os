package com.primatoos.backend.controller;

import com.primatoos.backend.dto.dailyreport.DailyReportCreateRequest;
import com.primatoos.backend.dto.dailyreport.DailyReportPhotoResponse;
import com.primatoos.backend.dto.dailyreport.DailyReportResponse;
import com.primatoos.backend.dto.dailyreport.DailyReportUpdateRequest;
import com.primatoos.backend.service.DailyReportService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/daily-reports")
@RequiredArgsConstructor
public class DailyReportController {

    private final DailyReportService dailyReportService;

    @PostMapping
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<DailyReportResponse> create(Authentication authentication,
                                                        @Valid @RequestBody DailyReportCreateRequest request) {
        DailyReportResponse response = dailyReportService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'WORKER')")
    public ResponseEntity<Page<DailyReportResponse>> findByWorkOrder(
            Authentication authentication,
            @RequestParam Long workOrderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                dailyReportService.findByWorkOrder(workOrderId, authentication.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'WORKER')")
    public ResponseEntity<DailyReportResponse> findById(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(dailyReportService.findById(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<DailyReportResponse> update(Authentication authentication, @PathVariable Long id,
                                                        @Valid @RequestBody DailyReportUpdateRequest request) {
        return ResponseEntity.ok(dailyReportService.update(id, authentication.getName(), request));
    }

    @PatchMapping("/{id}/finalize")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<DailyReportResponse> finalizeReport(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(dailyReportService.finalizeReport(id, authentication.getName()));
    }

    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DailyReportResponse> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(dailyReportService.reopen(id));
    }

    @PostMapping("/{id}/photos")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<DailyReportPhotoResponse> uploadPhoto(Authentication authentication, @PathVariable Long id,
                                                                  @RequestParam("file") MultipartFile file) {
        DailyReportPhotoResponse response = dailyReportService.uploadPhoto(id, authentication.getName(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<Void> deletePhoto(Authentication authentication, @PathVariable Long id,
                                             @PathVariable Long photoId) {
        dailyReportService.deletePhoto(id, photoId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
