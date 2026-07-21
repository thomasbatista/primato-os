package com.primatoos.backend.repository;

import com.primatoos.backend.model.DailyReport;
import com.primatoos.backend.model.DailyReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyReportRepository extends JpaRepository<DailyReport, Long> {

    Page<DailyReport> findByWorkOrderId(Long workOrderId, Pageable pageable);

    Page<DailyReport> findByStatus(DailyReportStatus status, Pageable pageable);
}
