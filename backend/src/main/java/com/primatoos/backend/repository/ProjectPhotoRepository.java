package com.primatoos.backend.repository;

import com.primatoos.backend.model.ProjectPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectPhotoRepository extends JpaRepository<ProjectPhoto, Long> {

    List<ProjectPhoto> findByProjectIdOrderByIdAsc(Long projectId);
}
