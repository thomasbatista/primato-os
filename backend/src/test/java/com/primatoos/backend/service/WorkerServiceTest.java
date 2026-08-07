package com.primatoos.backend.service;

import com.primatoos.backend.dto.worker.WorkerCreateRequest;
import com.primatoos.backend.dto.worker.WorkerResponse;
import com.primatoos.backend.dto.worker.WorkerUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.UserMapper;
import com.primatoos.backend.mapper.WorkerMapper;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Mock
    private UserRepository userRepository;

    private WorkerService workerService;

    @BeforeEach
    void setUp() {
        workerService = new WorkerService(workerRepository, userRepository, new WorkerMapper(new UserMapper()));
    }

    @Test
    void shouldCreateWorker_withoutLinkedUser() {
        given(workerRepository.save(any(Worker.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkerCreateRequest request = new WorkerCreateRequest("Carlos", "Pedreiro", "11999999999", null);

        WorkerResponse response = workerService.create(request);

        assertThat(response.name()).isEqualTo("Carlos");
        assertThat(response.active()).isTrue();
        assertThat(response.user()).isNull();
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldCreateWorker_whenLinkedUserHasWorkerRole() {
        User user = User.builder()
                .id(5L).name("Beto").email("beto@primatoos.test").password("hash").role(UserRole.WORKER).build();

        given(userRepository.findById(5L)).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(5L)).willReturn(Optional.empty());
        given(workerRepository.save(any(Worker.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkerCreateRequest request = new WorkerCreateRequest("Beto", "Eletricista", null, 5L);

        WorkerResponse response = workerService.create(request);

        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(5L);
    }

    @Test
    void shouldThrowBusinessRuleException_whenLinkedUserIsNotWorkerRole() {
        User manager = User.builder()
                .id(6L).name("Ana").email("ana@primatoos.test").password("hash").role(UserRole.MANAGER).build();

        given(userRepository.findById(6L)).willReturn(Optional.of(manager));

        WorkerCreateRequest request = new WorkerCreateRequest("Ana", null, null, 6L);

        assertThatThrownBy(() -> workerService.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(workerRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLinkedUserDoesNotExist() {
        given(userRepository.findById(77L)).willReturn(Optional.empty());

        WorkerCreateRequest request = new WorkerCreateRequest("Fulano", null, null, 77L);

        assertThatThrownBy(() -> workerService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowBusinessRuleException_whenLinkedUserAlreadyLinkedToAnotherWorker() {
        User user = User.builder()
                .id(7L).name("Joao").email("joao@primatoos.test").password("hash").role(UserRole.WORKER).build();
        Worker existingWorker = Worker.builder().id(100L).name("Joao Antigo").user(user).build();

        given(userRepository.findById(7L)).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(7L)).willReturn(Optional.of(existingWorker));

        WorkerCreateRequest request = new WorkerCreateRequest("Joao Novo", null, null, 7L);

        assertThatThrownBy(() -> workerService.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(workerRepository, never()).save(any());
    }

    @Test
    void shouldAllowUpdate_whenKeepingSameLinkedUser() {
        User user = User.builder()
                .id(8L).name("Rita").email("rita@primatoos.test").password("hash").role(UserRole.WORKER).build();
        Worker worker = Worker.builder().id(200L).name("Rita").user(user).build();

        given(workerRepository.findById(200L)).willReturn(Optional.of(worker));
        given(userRepository.findById(8L)).willReturn(Optional.of(user));
        given(workerRepository.findByUserId(8L)).willReturn(Optional.of(worker));
        given(workerRepository.save(any(Worker.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkerUpdateRequest request = new WorkerUpdateRequest("Rita Silva", "Pintora", "11888888888", 8L);

        WorkerResponse response = workerService.update(200L, request);

        assertThat(response.name()).isEqualTo("Rita Silva");
        assertThat(response.user().id()).isEqualTo(8L);
    }

    @Test
    void shouldDeactivateWorker_whenWorkerExists() {
        Worker worker = Worker.builder().id(300L).name("Marcos").build();

        given(workerRepository.findById(300L)).willReturn(Optional.of(worker));
        given(workerRepository.save(any(Worker.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkerResponse response = workerService.deactivate(300L);

        assertThat(response.active()).isFalse();
    }

    @Test
    void shouldThrowResourceNotFoundException_whenDeactivatingNonExistentWorker() {
        given(workerRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workerService.deactivate(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReactivateWorker_whenWorkerExists() {
        Worker worker = Worker.builder().id(301L).name("Marcos").active(false).build();

        given(workerRepository.findById(301L)).willReturn(Optional.of(worker));
        given(workerRepository.save(any(Worker.class))).willAnswer(invocation -> invocation.getArgument(0));

        WorkerResponse response = workerService.reactivate(301L);

        assertThat(response.active()).isTrue();
    }

    @Test
    void shouldThrowResourceNotFoundException_whenReactivatingNonExistentWorker() {
        given(workerRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workerService.reactivate(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnAllWorkers_whenActiveFilterIsNotProvided() {
        Worker worker = Worker.builder().id(1L).name("Colaborador").active(true).build();
        Pageable pageable = PageRequest.of(0, 10);
        given(workerRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(worker)));

        Page<WorkerResponse> page = workerService.findAll(null, pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(workerRepository, never()).findByActive(anyBoolean(), any());
    }

    @Test
    void shouldReturnOnlyActiveWorkers_whenActiveFilterIsTrue() {
        Worker worker = Worker.builder().id(2L).name("Colaborador Ativo").active(true).build();
        Pageable pageable = PageRequest.of(0, 10);
        given(workerRepository.findByActive(true, pageable)).willReturn(new PageImpl<>(List.of(worker)));

        Page<WorkerResponse> page = workerService.findAll(true, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).active()).isTrue();
        verify(workerRepository, never()).findAll(pageable);
    }
}
