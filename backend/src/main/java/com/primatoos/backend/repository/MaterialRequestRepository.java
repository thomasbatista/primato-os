package com.primatoos.backend.repository;

import com.primatoos.backend.model.MaterialRequest;
import com.primatoos.backend.model.MaterialRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaterialRequestRepository extends JpaRepository<MaterialRequest, Long> {

    @Query(value = "SELECT nextval('material_request_number_seq')", nativeQuery = true)
    Long nextRequestNumber();

    @Query("SELECT m FROM MaterialRequest m "
            + "WHERE (:projectId IS NULL OR m.project.id = :projectId) "
            + "AND (:workOrderId IS NULL OR m.workOrder.id = :workOrderId) "
            + "AND (:status IS NULL OR m.status = :status)")
    Page<MaterialRequest> search(@Param("projectId") Long projectId, @Param("workOrderId") Long workOrderId,
                                  @Param("status") MaterialRequestStatus status, Pageable pageable);
}
