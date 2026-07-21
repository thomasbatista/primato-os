package com.primatoos.backend.repository;

import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    @Query(value = "SELECT nextval('work_order_number_seq')", nativeQuery = true)
    Long nextOrderNumber();

    @Query("SELECT w FROM WorkOrder w "
            + "WHERE (:projectId IS NULL OR w.project.id = :projectId) "
            + "AND (:status IS NULL OR w.status = :status)")
    Page<WorkOrder> search(@Param("projectId") Long projectId, @Param("status") WorkOrderStatus status,
                            Pageable pageable);

    Page<WorkOrder> findByAssignedWorkers_Id(Long workerId, Pageable pageable);
}
