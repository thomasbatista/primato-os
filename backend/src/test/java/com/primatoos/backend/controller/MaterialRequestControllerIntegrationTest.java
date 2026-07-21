package com.primatoos.backend.controller;

import com.primatoos.backend.model.MaterialRequest;
import com.primatoos.backend.model.MaterialRequestItem;
import com.primatoos.backend.model.MaterialRequestPriority;
import com.primatoos.backend.model.MaterialRequestStatus;
import com.primatoos.backend.model.MaterialRequestUnit;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.repository.MaterialRequestRepository;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MaterialRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MaterialRequestRepository materialRequestRepository;

    @Autowired
    private JwtService jwtService;

    private String createBody(Long projectId, Long requesterId) {
        return """
                {"projectId": %d, "requestDate": "2026-08-01", "requesterId": %d, "priority": "MEDIUM",
                 "items": [{"name": "Cimento", "quantity": 10, "unit": "BAG"}]}
                """.formatted(projectId, requesterId);
    }

    private String deliveryBody(Long itemId) {
        return """
                {"items": [{"materialRequestItemId": %d, "quantityDelivered": 1}]}
                """.formatted(itemId);
    }

    private String fromWorkOrderBody() {
        return """
                {"priority": "MEDIUM", "items": [{"name": "Cimento", "quantity": 10, "unit": "BAG"}]}
                """;
    }

    private User seedWorkerUser(String email) {
        return userRepository.save(User.builder()
                .name("Colaborador MR").email(email).password("unused").role(UserRole.WORKER).build());
    }

    @Test
    void shouldCreateMaterialRequest_whenAuthenticatedAsManager() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor MR").email("gestor.mr@primatoos.test").password("unused").role(UserRole.MANAGER)
                .build());
        Project project = projectRepository.save(Project.builder()
                .name("Obra MR").client("Cliente MR").responsibleUser(manager).status(ProjectStatus.PLANNING)
                .build());

        String token = jwtService.generateToken(manager);

        mockMvc.perform(post("/api/v1/material-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(project.getId(), manager.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.requestNumber").isNumber());
    }

    @Test
    void shouldDeriveDeliveredStatus_whenAllItemsFullyDelivered() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Entrega").email("gestor.entrega@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        Project project = projectRepository.save(Project.builder()
                .name("Obra Entrega").client("Cliente Entrega").responsibleUser(manager).status(ProjectStatus.PLANNING)
                .build());

        MaterialRequestItem item = MaterialRequestItem.builder()
                .name("Cimento").quantity(BigDecimal.TEN).unit(MaterialRequestUnit.BAG)
                .quantityDelivered(BigDecimal.ZERO).build();

        MaterialRequest materialRequest = MaterialRequest.builder()
                .requestNumber(materialRequestRepository.nextRequestNumber())
                .project(project)
                .requestDate(LocalDate.of(2026, 8, 1))
                .requester(manager)
                .priority(MaterialRequestPriority.MEDIUM)
                .status(MaterialRequestStatus.PURCHASED)
                .build();
        item.setMaterialRequest(materialRequest);
        materialRequest.getItems().add(item);
        materialRequest = materialRequestRepository.save(materialRequest);

        String token = jwtService.generateToken(manager);
        Long itemId = materialRequest.getItems().get(0).getId();

        mockMvc.perform(patch("/api/v1/material-requests/" + materialRequest.getId() + "/deliveries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": [{"materialRequestItemId": %d, "quantityDelivered": 10}]}
                                """.formatted(itemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void shouldReturnForbidden_whenWorkerCreates() throws Exception {
        User worker = seedWorkerUser("worker.create@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(post("/api/v1/material-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, 1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnForbidden_whenWorkerCreatesFromWorkOrder() throws Exception {
        User worker = seedWorkerUser("worker.fromwo@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(post("/api/v1/material-requests/from-work-order/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fromWorkOrderBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbidden_whenWorkerLists() throws Exception {
        User worker = seedWorkerUser("worker.list@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(get("/api/v1/material-requests")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbidden_whenWorkerGetsById() throws Exception {
        User worker = seedWorkerUser("worker.getbyid@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(get("/api/v1/material-requests/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbidden_whenWorkerUpdates() throws Exception {
        User worker = seedWorkerUser("worker.update@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(put("/api/v1/material-requests/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbidden_whenWorkerSubmits() throws Exception {
        User worker = seedWorkerUser("worker.submit@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(patch("/api/v1/material-requests/1/submit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbidden_whenWorkerApproves() throws Exception {
        User worker = seedWorkerUser("worker.approve@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(patch("/api/v1/material-requests/1/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbidden_whenWorkerMarksPurchased() throws Exception {
        User worker = seedWorkerUser("worker.purchase@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(patch("/api/v1/material-requests/1/purchase")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbidden_whenWorkerCancels() throws Exception {
        User worker = seedWorkerUser("worker.cancel@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(patch("/api/v1/material-requests/1/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbidden_whenWorkerRegistersDelivery() throws Exception {
        User worker = seedWorkerUser("worker.deliveries@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(patch("/api/v1/material-requests/1/deliveries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryBody(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbidden_whenWorkerDuplicates() throws Exception {
        User worker = seedWorkerUser("worker.duplicate@primatoos.test");
        String token = jwtService.generateToken(worker);

        mockMvc.perform(post("/api/v1/material-requests/1/duplicate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
