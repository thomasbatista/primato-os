package com.primatoos.backend.controller;

import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.WorkOrderStatus;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.repository.WorkOrderRepository;
import com.primatoos.backend.repository.WorkerRepository;
import com.primatoos.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkOrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldCreateWorkOrder_whenAuthenticatedAsManager() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor OS").email("gestor.wo@primatoos.test").password("unused").role(UserRole.MANAGER)
                .build());
        Project project = projectRepository.save(Project.builder()
                .name("Obra OS").client("Cliente OS").responsibleUser(manager).status(ProjectStatus.PLANNING)
                .build());

        String token = jwtService.generateToken(manager);

        String body = """
                {"projectId": %d, "date": "2026-08-01", "responsibleUserId": %d, "stage": "Fundação", "description": "Concretar fundação"}
                """.formatted(project.getId(), manager.getId());

        mockMvc.perform(post("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.orderNumber").isNumber());
    }

    @Test
    void shouldReturnForbidden_whenCreatingWorkOrderAsWorker() throws Exception {
        User worker = userRepository.save(User.builder()
                .name("Colaborador OS").email("worker.wo@primatoos.test").password("unused").role(UserRole.WORKER)
                .build());

        String token = jwtService.generateToken(worker);

        String body = """
                {"projectId": 1, "date": "2026-08-01", "responsibleUserId": 1, "stage": "Fundação", "description": "Concretar fundação"}
                """;

        mockMvc.perform(post("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnForbidden_whenListingAllWorkOrdersAsWorker() throws Exception {
        User worker = userRepository.save(User.builder()
                .name("Colaborador Lista OS").email("worker.list.wo@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());

        String token = jwtService.generateToken(worker);

        mockMvc.perform(get("/api/v1/work-orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnAssignedWorkOrders_whenWorkerCallsMineEndpoint() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Mine").email("gestor.mine@primatoos.test").password("unused").role(UserRole.MANAGER)
                .build());
        Project project = projectRepository.save(Project.builder()
                .name("Obra Mine").client("Cliente Mine").responsibleUser(manager).status(ProjectStatus.PLANNING)
                .build());

        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Mine").email("worker.mine@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador Mine").user(workerUser).build());

        WorkOrder workOrder = workOrderRepository.save(WorkOrder.builder()
                .orderNumber(workOrderRepository.nextOrderNumber())
                .project(project)
                .date(LocalDate.of(2026, 8, 1))
                .responsibleUser(manager)
                .stage("Fundação")
                .description("Concretar fundação")
                .status(WorkOrderStatus.RELEASED)
                .assignedWorkers(Set.of(worker))
                .build());

        String token = jwtService.generateToken(workerUser);

        mockMvc.perform(get("/api/v1/work-orders/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(workOrder.getId()));
    }

    @Test
    void shouldReturnForbidden_whenManagerCallsMineEndpoint() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Nao Worker").email("gestor.notworker@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());

        String token = jwtService.generateToken(manager);

        mockMvc.perform(get("/api/v1/work-orders/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
