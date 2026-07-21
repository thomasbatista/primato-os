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
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User seedManager(String email) {
        return userRepository.save(User.builder()
                .name("Gestor").email(email).password("unused").role(UserRole.MANAGER).build());
    }

    private User seedWorker(String email) {
        return userRepository.save(User.builder()
                .name("Colaborador").email(email).password("unused").role(UserRole.WORKER).build());
    }

    @Test
    void shouldCreateUser_whenAuthenticatedAsManager() throws Exception {
        User manager = seedManager("gestor.users@primatoos.test");
        String token = jwtService.generateToken(manager);

        String body = """
                {"name": "Novo Colaborador", "email": "novo.colaborador@primatoos.test",
                 "password": "senha12345", "role": "WORKER"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("novo.colaborador@primatoos.test"))
                .andExpect(jsonPath("$.role").value("WORKER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        User manager = seedManager("gestor.invalidemail@primatoos.test");
        String token = jwtService.generateToken(manager);

        String body = """
                {"name": "Fulano", "email": "nao-e-um-email", "password": "senha12345", "role": "WORKER"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_whenPasswordIsTooShort() throws Exception {
        User manager = seedManager("gestor.shortpass@primatoos.test");
        String token = jwtService.generateToken(manager);

        String body = """
                {"name": "Fulano", "email": "fulano@primatoos.test", "password": "123", "role": "WORKER"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {
        User manager = seedManager("gestor.dupe@primatoos.test");
        seedWorker("existente@primatoos.test");
        String token = jwtService.generateToken(manager);

        String body = """
                {"name": "Fulano", "email": "existente@primatoos.test", "password": "senha12345", "role": "WORKER"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnForbidden_whenWorkerCreatesUser() throws Exception {
        User worker = seedWorker("worker.createuser@primatoos.test");
        String token = jwtService.generateToken(worker);

        String body = """
                {"name": "Fulano", "email": "fulano2@primatoos.test", "password": "senha12345", "role": "WORKER"}
                """;

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnForbidden_whenWorkerListsUsers() throws Exception {
        User worker = seedWorker("worker.listusers@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListUsers_whenAuthenticatedAsManager() throws Exception {
        User manager = seedManager("gestor.listusers@primatoos.test");
        String token = jwtService.generateToken(manager);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
