package com.primatoos.backend.security;

import com.primatoos.backend.dto.user.UserCreateRequest;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManagerBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.bootstrap.manager-name}")
    private String bootstrapName;

    @Value("${app.bootstrap.manager-email}")
    private String bootstrapEmail;

    @Value("${app.bootstrap.manager-password}")
    private String bootstrapPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("Nenhum usuário cadastrado e BOOTSTRAP_MANAGER_EMAIL/BOOTSTRAP_MANAGER_PASSWORD não "
                    + "configurados. Defina essas variáveis de ambiente e reinicie a aplicação para criar "
                    + "o primeiro gestor.");
            return;
        }

        userService.create(new UserCreateRequest(bootstrapName, bootstrapEmail, bootstrapPassword, UserRole.MANAGER));
        log.info("Primeiro usuário gestor criado automaticamente: {}", bootstrapEmail);
    }
}
