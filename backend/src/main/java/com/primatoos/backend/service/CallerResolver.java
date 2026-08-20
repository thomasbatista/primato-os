package com.primatoos.backend.service;

import com.primatoos.backend.exception.ForbiddenOperationException;
import com.primatoos.backend.exception.ResourceNotFoundException;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated caller's identity. Several services need to branch on whether the
 * caller is a manager or a worker (and to reach that worker's profile) before applying their own
 * ownership rules, so that lookup lives here instead of being repeated in each of them.
 */
@Component
@RequiredArgsConstructor
public class CallerResolver {

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;

    public User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    public boolean isManager(User user) {
        return user.getRole() == UserRole.MANAGER;
    }

    public Worker resolveWorkerOrThrow(String email) {
        return resolveWorkerOrThrow(findUserOrThrow(email));
    }

    // Takes an already-loaded User so callers that branch on role don't pay for a second
    // lookup of the same person.
    public Worker resolveWorkerOrThrow(User user) {
        return workerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Você não está vinculado a um perfil de colaborador"));
    }
}
