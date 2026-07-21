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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DailyReportControllerIntegrationTest {

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

    private WorkOrder createReportableWorkOrder(User manager, Worker... assignedWorkers) {
        Project project = projectRepository.save(Project.builder()
                .name("Obra RDO").client("Cliente RDO").responsibleUser(manager).status(ProjectStatus.PLANNING)
                .build());

        return workOrderRepository.save(WorkOrder.builder()
                .orderNumber(workOrderRepository.nextOrderNumber())
                .project(project)
                .date(LocalDate.of(2026, 8, 1))
                .responsibleUser(manager)
                .stage("Fundação")
                .description("Concretar fundação")
                .status(WorkOrderStatus.IN_PROGRESS)
                .assignedWorkers(Set.of(assignedWorkers))
                .build());
    }

    @Test
    void shouldCreateDailyReport_whenWorkerIsAssignedToWorkOrder() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor RDO").email("gestor.rdo@primatoos.test").password("unused").role(UserRole.MANAGER)
                .build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador RDO").email("worker.rdo@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador RDO").user(workerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, worker);

        String token = jwtService.generateToken(workerUser);

        String body = """
                {"workOrderId": %d, "date": "2026-08-01"}
                """.formatted(workOrder.getId());

        mockMvc.perform(post("/api/v1/daily-reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.filledByWorker.id").value(worker.getId()));
    }

    @Test
    void shouldReturnForbidden_whenWorkerReportsOnUnassignedWorkOrder() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Outsider").email("gestor.outsider@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User outsiderUser = userRepository.save(User.builder()
                .name("Outsider").email("outsider.rdo@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        workerRepository.save(Worker.builder().name("Outsider").user(outsiderUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager);

        String token = jwtService.generateToken(outsiderUser);

        String body = """
                {"workOrderId": %d, "date": "2026-08-01"}
                """.formatted(workOrder.getId());

        mockMvc.perform(post("/api/v1/daily-reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnForbidden_whenManagerTriesToCreateDailyReport() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Cria").email("gestor.cria@primatoos.test").password("unused").role(UserRole.MANAGER)
                .build());
        WorkOrder workOrder = createReportableWorkOrder(manager);

        String token = jwtService.generateToken(manager);

        String body = """
                {"workOrderId": %d, "date": "2026-08-01"}
                """.formatted(workOrder.getId());

        mockMvc.perform(post("/api/v1/daily-reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireReopenBeforeEditing_whenReportIsFinalized() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Finaliza").email("gestor.finaliza@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Finaliza").email("worker.finaliza@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador Finaliza").user(workerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, worker);

        String workerToken = jwtService.generateToken(workerUser);
        String managerToken = jwtService.generateToken(manager);

        String createBody = """
                {"workOrderId": %d, "date": "2026-08-01"}
                """.formatted(workOrder.getId());

        String response = mockMvc.perform(post("/api/v1/daily-reports")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reportId = com.jayway.jsonpath.JsonPath.parse(response).read("$.id", Long.class);

        mockMvc.perform(patch("/api/v1/daily-reports/" + reportId + "/finalize")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZED"));

        String updateBody = """
                {"date": "2026-08-02"}
                """;

        mockMvc.perform(put("/api/v1/daily-reports/" + reportId)
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/daily-reports/" + reportId + "/reopen")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(put("/api/v1/daily-reports/" + reportId)
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-02"));
    }

    @Test
    void shouldReturnForbidden_whenWorkerReopens() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Reopen").email("gestor.reopen@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Reopen").email("worker.reopen@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador Reopen").user(workerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, worker);

        String workerToken = jwtService.generateToken(workerUser);

        String createBody = """
                {"workOrderId": %d, "date": "2026-08-01"}
                """.formatted(workOrder.getId());

        String response = mockMvc.perform(post("/api/v1/daily-reports")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reportId = com.jayway.jsonpath.JsonPath.parse(response).read("$.id", Long.class);

        mockMvc.perform(patch("/api/v1/daily-reports/" + reportId + "/reopen")
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowManagerToViewDailyReport_byId() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor View").email("gestor.view@primatoos.test").password("unused").role(UserRole.MANAGER)
                .build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador View").email("worker.view@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador View").user(workerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, worker);

        String workerToken = jwtService.generateToken(workerUser);
        String managerToken = jwtService.generateToken(manager);

        String createBody = """
                {"workOrderId": %d, "date": "2026-08-01"}
                """.formatted(workOrder.getId());

        String response = mockMvc.perform(post("/api/v1/daily-reports")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long reportId = com.jayway.jsonpath.JsonPath.parse(response).read("$.id", Long.class);

        mockMvc.perform(get("/api/v1/daily-reports/" + reportId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId));
    }
}
