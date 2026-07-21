package com.primatoos.backend.controller;

import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldCreateWorker_whenAuthenticatedAsManager() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Teste")
                .email("gestor.workers@primatoos.test")
                .password("unused")
                .role(UserRole.MANAGER)
                .build());

        String token = jwtService.generateToken(manager);

        String body = """
                {"name": "Colaborador Teste", "function": "Pedreiro"}
                """;

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Colaborador Teste"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturnForbidden_whenCreatingWorkerAsWorker() throws Exception {
        User worker = userRepository.save(User.builder()
                .name("Colaborador Teste")
                .email("worker.workers@primatoos.test")
                .password("unused")
                .role(UserRole.WORKER)
                .build());

        String token = jwtService.generateToken(worker);

        String body = """
                {"name": "Colaborador Teste"}
                """;

        mockMvc.perform(post("/api/v1/workers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnForbidden_whenListingWorkersAsWorker() throws Exception {
        User worker = userRepository.save(User.builder()
                .name("Colaborador Lista")
                .email("worker.list.workers@primatoos.test")
                .password("unused")
                .role(UserRole.WORKER)
                .build());

        String token = jwtService.generateToken(worker);

        mockMvc.perform(get("/api/v1/workers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
