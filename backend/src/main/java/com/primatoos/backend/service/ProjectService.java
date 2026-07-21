package com.primatoos.backend.service;

import com.primatoos.backend.dto.ProjectCreateRequest;
import com.primatoos.backend.dto.ProjectResponse;
import com.primatoos.backend.dto.ProjectUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.ProjectMapper;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
                           ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
    }

    public ProjectResponse create(ProjectCreateRequest request) {
        User responsibleUser = findManagerOrThrow(request.responsibleUserId());

        Project project = Project.builder()
                .name(request.name())
                .client(request.client())
                .address(request.address())
                .responsibleUser(responsibleUser)
                .startDate(request.startDate())
                .expectedDeadline(request.expectedDeadline())
                .currentStage(request.currentStage())
                .notes(request.notes())
                .status(ProjectStatus.PLANNING)
                .build();

        return projectMapper.toResponse(projectRepository.save(project));
    }

    public Page<ProjectResponse> findAll(Pageable pageable) {
        return projectRepository.findAll(pageable).map(projectMapper::toResponse);
    }

    public ProjectResponse findById(Long id) {
        return projectMapper.toResponse(findProjectOrThrow(id));
    }

    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        Project project = findProjectOrThrow(id);
        User responsibleUser = findManagerOrThrow(request.responsibleUserId());

        project.setName(request.name());
        project.setClient(request.client());
        project.setAddress(request.address());
        project.setResponsibleUser(responsibleUser);
        project.setStartDate(request.startDate());
        project.setExpectedDeadline(request.expectedDeadline());
        project.setCurrentStage(request.currentStage());
        project.setNotes(request.notes());
        project.setStatus(request.status());

        return projectMapper.toResponse(projectRepository.save(project));
    }

    public void cancel(Long id) {
        Project project = findProjectOrThrow(id);
        project.setStatus(ProjectStatus.CANCELLED);
        projectRepository.save(project);
    }

    private Project findProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Obra não encontrada"));
    }

    private User findManagerOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));

        if (user.getRole() != UserRole.MANAGER) {
            throw new BusinessRuleException("O responsável pela obra deve ser um usuário com papel de gestor");
        }

        return user;
    }
}
