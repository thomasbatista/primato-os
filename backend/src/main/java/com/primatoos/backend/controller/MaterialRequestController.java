package com.primatoos.backend.controller;

import com.primatoos.backend.dto.materialrequest.MaterialRequestCreateRequest;
import com.primatoos.backend.dto.materialrequest.MaterialRequestFromWorkOrderRequest;
import com.primatoos.backend.dto.materialrequest.MaterialRequestResponse;
import com.primatoos.backend.dto.materialrequest.MaterialRequestUpdateRequest;
import com.primatoos.backend.dto.materialrequest.RegisterDeliveryRequest;
import com.primatoos.backend.model.MaterialRequestStatus;
import com.primatoos.backend.service.MaterialRequestService;
import com.primatoos.backend.service.PdfGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/material-requests")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class MaterialRequestController {

    private final MaterialRequestService materialRequestService;
    private final PdfGeneratorService pdfGeneratorService;

    @PostMapping
    public ResponseEntity<MaterialRequestResponse> create(@Valid @RequestBody MaterialRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialRequestService.create(request));
    }

    @PostMapping("/from-work-order/{workOrderId}")
    public ResponseEntity<MaterialRequestResponse> createFromWorkOrder(
            Authentication authentication,
            @PathVariable Long workOrderId,
            @Valid @RequestBody MaterialRequestFromWorkOrderRequest request) {
        MaterialRequestResponse response = materialRequestService.createFromWorkOrder(workOrderId,
                authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<MaterialRequestResponse>> findAll(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(required = false) MaterialRequestStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(materialRequestService.findAll(projectId, workOrderId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialRequestResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(materialRequestService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialRequestResponse> update(@PathVariable Long id,
                                                            @Valid @RequestBody MaterialRequestUpdateRequest request) {
        return ResponseEntity.ok(materialRequestService.update(id, request));
    }

    @PatchMapping("/{id}/submit")
    public ResponseEntity<MaterialRequestResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(materialRequestService.submit(id));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<MaterialRequestResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(materialRequestService.approve(id));
    }

    @PatchMapping("/{id}/purchase")
    public ResponseEntity<MaterialRequestResponse> markPurchased(@PathVariable Long id) {
        return ResponseEntity.ok(materialRequestService.markPurchased(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<MaterialRequestResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(materialRequestService.cancel(id));
    }

    @PatchMapping("/{id}/deliveries")
    public ResponseEntity<MaterialRequestResponse> registerDelivery(@PathVariable Long id,
                                                                      @Valid @RequestBody RegisterDeliveryRequest request) {
        return ResponseEntity.ok(materialRequestService.registerDelivery(id, request));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<MaterialRequestResponse> duplicate(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialRequestService.duplicate(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        MaterialRequestResponse materialRequest = materialRequestService.findById(id);
        byte[] pdf = pdfGeneratorService.generateMaterialRequestPdf(materialRequest);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"pedido-" + materialRequest.requestNumber() + ".pdf\"")
                .body(pdf);
    }
}
