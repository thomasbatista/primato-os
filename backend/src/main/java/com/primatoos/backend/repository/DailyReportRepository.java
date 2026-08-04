package com.primatoos.backend.repository;

import com.primatoos.backend.model.DailyReport;
import com.primatoos.backend.model.DailyReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    Page<DailyReport> findByWorkOrderId(Long workOrderId, Pageable pageable);

    Page<DailyReport> findByStatus(DailyReportStatus status, Pageable pageable);

    @Query("SELECT d FROM DailyReport d "
            + "WHERE d.filledByWorker.id = :workerId "
            + "AND (:workOrderId IS NULL OR d.workOrder.id = :workOrderId)")
    Page<DailyReport> findByFilledByWorker(@Param("workerId") Long workerId, @Param("workOrderId") Long workOrderId,
                                            Pageable pageable);
}
