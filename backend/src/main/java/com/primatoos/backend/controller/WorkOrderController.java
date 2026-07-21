package com.primatoos.backend.controller;

import com.primatoos.backend.dto.workorder.WorkOrderCreateRequest;
import com.primatoos.backend.dto.workorder.WorkOrderResponse;
import com.primatoos.backend.dto.workorder.WorkOrderUpdateRequest;
import com.primatoos.backend.model.WorkOrderStatus;
import com.primatoos.backend.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Page<WorkOrderResponse>> findAll(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) WorkOrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(workOrderService.findAll(projectId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody WorkOrderUpdateRequest request) {
        return ResponseEntity.ok(workOrderService.update(id, request));
    }

    @PatchMapping("/{id}/release")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponse> release(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.release(id));
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponse> start(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.start(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.complete(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.cancel(id));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<WorkOrderResponse> duplicate(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderService.duplicate(id));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<Page<WorkOrderResponse>> findMine(Authentication authentication,
                                                              @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(workOrderService.findMyWorkOrders(authentication.getName(), pageable));
    }
}
