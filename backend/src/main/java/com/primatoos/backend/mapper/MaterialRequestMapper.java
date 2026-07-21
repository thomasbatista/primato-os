package com.primatoos.backend.mapper;

import com.primatoos.backend.dto.materialrequest.MaterialRequestItemResponse;
import com.primatoos.backend.dto.materialrequest.MaterialRequestResponse;
import com.primatoos.backend.dto.materialrequest.MaterialRequestSummaryResponse;
import com.primatoos.backend.model.MaterialRequest;
import com.primatoos.backend.model.MaterialRequestItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MaterialRequestMapper {

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final WorkOrderMapper workOrderMapper;

    public MaterialRequestResponse toResponse(MaterialRequest materialRequest) {
        return new MaterialRequestResponse(
                materialRequest.getId(),
                materialRequest.getRequestNumber(),
                projectMapper.toSummary(materialRequest.getProject()),
                materialRequest.getWorkOrder() != null ? workOrderMapper.toSummary(materialRequest.getWorkOrder()) : null,
                materialRequest.getRequestDate(),
                materialRequest.getNeededByDate(),
                userMapper.toSummary(materialRequest.getRequester()),
                materialRequest.getPriority(),
                materialRequest.getJustification(),
                materialRequest.getNotes(),
                materialRequest.getDeliveryLocation(),
                materialRequest.getStatus(),
                materialRequest.getItems().stream().map(this::toItemResponse).toList(),
                materialRequest.getCreatedAt()
        );
    }

    public MaterialRequestSummaryResponse toSummary(MaterialRequest materialRequest) {
        return new MaterialRequestSummaryResponse(
                materialRequest.getId(),
                materialRequest.getRequestNumber(),
                projectMapper.toSummary(materialRequest.getProject()),
                materialRequest.getPriority(),
                materialRequest.getStatus()
        );
    }

    private MaterialRequestItemResponse toItemResponse(MaterialRequestItem item) {
        return new MaterialRequestItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnit(),
                item.getBrand(),
                item.getPhotoReference(),
                item.getNotes(),
                item.getQuantityDelivered()
        );
    }
}
