package com.primatoos.backend.mapper;

import com.primatoos.backend.dto.project.ProjectResponse;
import com.primatoos.backend.model.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final UserMapper userMapper;

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getClient(),
                project.getAddress(),
                userMapper.toSummary(project.getResponsibleUser()),
                project.getStartDate(),
                project.getExpectedDeadline(),
                project.getCurrentStage(),
                project.getStatus(),
                project.getNotes(),
                project.getCreatedAt()
        );
    }
}
