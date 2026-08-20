package com.primatoos.backend.service;

import com.primatoos.backend.dto.project.ProjectCreateRequest;
import com.primatoos.backend.dto.project.ProjectPhotoResponse;
import com.primatoos.backend.dto.project.ProjectResponse;
import com.primatoos.backend.dto.project.ProjectUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ForbiddenOperationException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.ProjectMapper;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.ProjectPhoto;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.ProjectPhotoRepository;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectPhotoRepository projectPhotoRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final StorageService storageService;
    private final PhotoUploadSupport photoUploadSupport;
    private final CallerResolver callerResolver;

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

    public Page<ProjectResponse> findAll(ProjectStatus status, Pageable pageable) {
        Page<Project> projects = status != null
                ? projectRepository.findByStatus(status, pageable)
                : projectRepository.findAll(pageable);
        return projects.map(projectMapper::toResponse);
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

    public ProjectResponse findByIdForCaller(Long id, String email) {
        Project project = findProjectOrThrow(id);
        enforceViewPermission(email, project);

        return projectMapper.toResponse(project);
    }

    public ProjectPhotoResponse uploadPhoto(Long projectId, MultipartFile file) {
        Project project = findProjectOrThrow(projectId);

        String extension = photoUploadSupport.validateAndGetExtension(file);
        String key = "projects/" + projectId + "/" + UUID.randomUUID() + "." + extension;
        String url = storageService.upload(key, photoUploadSupport.readBytes(file), file.getContentType());

        ProjectPhoto photo = ProjectPhoto.builder()
                .project(project)
                .url(url)
                .build();

        return projectMapper.toPhotoResponse(projectPhotoRepository.save(photo));
    }

    public List<ProjectPhotoResponse> findPhotos(Long projectId, String email) {
        Project project = findProjectOrThrow(projectId);
        enforceViewPermission(email, project);

        return projectPhotoRepository.findByProjectIdOrderByIdAsc(projectId).stream()
                .map(projectMapper::toPhotoResponse)
                .toList();
    }

    public void deletePhoto(Long projectId, Long photoId) {
        findProjectOrThrow(projectId);

        ProjectPhoto photo = projectPhotoRepository.findById(photoId)
                .filter(candidate -> candidate.getProject().getId().equals(projectId))
                .orElseThrow(() -> new ResourceNotFoundException("Foto não encontrada"));

        storageService.delete(photo.getUrl());
        projectPhotoRepository.delete(photo);
    }

    // A worker may read a project when they are assigned to at least one of its work orders,
    // in any status. That is exactly the boundary /work-orders/mine already draws — a worker
    // who can open an OS should not be denied the obra whose name that OS already shows them.
    private void enforceViewPermission(String email, Project project) {
        User caller = callerResolver.findUserOrThrow(email);

        if (callerResolver.isManager(caller)) {
            return;
        }

        Worker worker = callerResolver.resolveWorkerOrThrow(caller);

        if (!workOrderRepository.existsByProject_IdAndAssignedWorkers_Id(project.getId(), worker.getId())) {
            throw new ForbiddenOperationException("Você não está atribuído a nenhuma ordem de serviço desta obra");
        }
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
