package com.primatoos.backend.repository;

import com.primatoos.backend.model.WorkOrderPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderPhotoRepository extends JpaRepository<WorkOrderPhoto, Long> {

    List<WorkOrderPhoto> findByWorkOrderIdOrderByIdAsc(Long workOrderId);
}
