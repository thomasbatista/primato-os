package com.primatoos.backend.mapper;

import com.primatoos.backend.dto.ProjectResponse;
import com.primatoos.backend.model.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    private final UserMapper userMapper;

    public ProjectMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

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
