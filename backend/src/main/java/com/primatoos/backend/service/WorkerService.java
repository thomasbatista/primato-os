package com.primatoos.backend.service;

import com.primatoos.backend.dto.WorkerCreateRequest;
import com.primatoos.backend.dto.WorkerResponse;
import com.primatoos.backend.dto.WorkerUpdateRequest;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.mapper.WorkerMapper;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.repository.WorkerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final WorkerMapper workerMapper;

    public WorkerService(WorkerRepository workerRepository, UserRepository userRepository,
                          WorkerMapper workerMapper) {
        this.workerRepository = workerRepository;
        this.userRepository = userRepository;
        this.workerMapper = workerMapper;
    }

    public WorkerResponse create(WorkerCreateRequest request) {
        User user = resolveWorkerUserOrNull(request.userId(), null);

        Worker worker = Worker.builder()
                .name(request.name())
                .function(request.function())
                .phone(request.phone())
                .user(user)
                .build();

        return workerMapper.toResponse(workerRepository.save(worker));
    }

    public Page<WorkerResponse> findAll(Pageable pageable) {
        return workerRepository.findAll(pageable).map(workerMapper::toResponse);
    }

    public WorkerResponse findById(Long id) {
        return workerMapper.toResponse(findWorkerOrThrow(id));
    }

    public WorkerResponse update(Long id, WorkerUpdateRequest request) {
        Worker worker = findWorkerOrThrow(id);
        User user = resolveWorkerUserOrNull(request.userId(), id);

        worker.setName(request.name());
        worker.setFunction(request.function());
        worker.setPhone(request.phone());
        worker.setUser(user);

        return workerMapper.toResponse(workerRepository.save(worker));
    }

    public WorkerResponse deactivate(Long id) {
        Worker worker = findWorkerOrThrow(id);
        worker.setActive(false);

        return workerMapper.toResponse(workerRepository.save(worker));
    }

    private Worker findWorkerOrThrow(Long id) {
        return workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));
    }

    private User resolveWorkerUserOrNull(Long userId, Long currentWorkerId) {
        if (userId == null) {
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário vinculado não encontrado"));

        if (user.getRole() != UserRole.WORKER) {
            throw new BusinessRuleException("O usuário vinculado deve ter papel de colaborador");
        }

        workerRepository.findByUserId(userId)
                .filter(existing -> !existing.getId().equals(currentWorkerId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Este usuário já está vinculado a outro colaborador");
                });

        return user;
    }
}
