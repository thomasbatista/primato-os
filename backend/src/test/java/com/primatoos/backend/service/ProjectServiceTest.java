package com.primatoos.backend.service;

import com.primatoos.backend.dto.project.ProjectCreateRequest;
import com.primatoos.backend.dto.project.ProjectResponse;
import com.primatoos.backend.dto.project.ProjectUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.ProjectMapper;
import com.primatoos.backend.mapper.UserMapper;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, userRepository, new ProjectMapper(new UserMapper()));
    }

    @Test
    void shouldCreateProject_whenResponsibleUserIsManager() {
        User manager = User.builder()
                .id(1L).name("Ana").email("ana@primatoos.test").password("hash").role(UserRole.MANAGER).build();

        given(userRepository.findById(1L)).willReturn(Optional.of(manager));
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> invocation.getArgument(0));

        ProjectCreateRequest request = new ProjectCreateRequest(
                "Obra Vila Nova", "Cliente X", null, 1L, null, null, null, null);

        ProjectResponse response = projectService.create(request);

        assertThat(response.name()).isEqualTo("Obra Vila Nova");
        assertThat(response.status()).isEqualTo(ProjectStatus.PLANNING);
        assertThat(response.responsibleUser().id()).isEqualTo(1L);
    }

    @Test
    void shouldThrowBusinessRuleException_whenResponsibleUserIsNotManager() {
        User worker = User.builder()
                .id(2L).name("Joao").email("joao@primatoos.test").password("hash").role(UserRole.WORKER).build();

        given(userRepository.findById(2L)).willReturn(Optional.of(worker));

        ProjectCreateRequest request = new ProjectCreateRequest(
                "Obra Centro", "Cliente Y", null, 2L, null, null, null, null);

        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenResponsibleUserDoesNotExist() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        ProjectCreateRequest request = new ProjectCreateRequest(
                "Obra Sul", "Cliente Z", null, 99L, null, null, null, null);

        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenProjectDoesNotExistOnUpdate() {
        given(projectRepository.findById(1L)).willReturn(Optional.empty());

        ProjectUpdateRequest request = new ProjectUpdateRequest(
                "Obra", "Cliente", null, 1L, null, null, null, null, ProjectStatus.PLANNING);

        assertThatThrownBy(() -> projectService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowBusinessRuleException_whenUpdatingResponsibleUserToNonManager() {
        Project project = Project.builder()
                .id(1L).name("Obra").client("Cliente").status(ProjectStatus.PLANNING).build();
        User worker = User.builder()
                .id(3L).name("Carlos").email("carlos@primatoos.test").password("hash").role(UserRole.WORKER).build();

        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(userRepository.findById(3L)).willReturn(Optional.of(worker));

        ProjectUpdateRequest request = new ProjectUpdateRequest(
                "Obra", "Cliente", null, 3L, null, null, null, null, ProjectStatus.IN_PROGRESS);

        assertThatThrownBy(() -> projectService.update(1L, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(projectRepository, never()).save(any());
    }

    @Test
    void shouldCancelProject_whenProjectExists() {
        Project project = Project.builder()
                .id(1L).name("Obra").client("Cliente").status(ProjectStatus.IN_PROGRESS).build();

        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(projectRepository.save(any(Project.class))).willAnswer(invocation -> invocation.getArgument(0));

        projectService.cancel(1L);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CANCELLED);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenCancellingNonExistentProject() {
        given(projectRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.cancel(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
