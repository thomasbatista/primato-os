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
import com.primatoos.backend.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    @MockitoBean
    private StorageService storageService;

    private WorkOrder seedWorkOrder(User manager, String projectName, Worker... assignedWorkers) {
        Project project = projectRepository.save(Project.builder()
                .name(projectName).client("Cliente Foto OS").responsibleUser(manager)
                .status(ProjectStatus.PLANNING).build());

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

    @Test
    void shouldReturnWorkOrder_whenWorkerCallsMineByIdEndpointAndIsAssigned() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Mine ById").email("gestor.mine.byid@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        Project project = projectRepository.save(Project.builder()
                .name("Obra Mine ById").client("Cliente Mine ById").responsibleUser(manager)
                .status(ProjectStatus.PLANNING).build());

        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Mine ById").email("worker.mine.byid@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador Mine ById").user(workerUser).build());

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

        mockMvc.perform(get("/api/v1/work-orders/mine/" + workOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrder.getId()));
    }

    @Test
    void shouldReturnForbidden_whenWorkerCallsMineByIdEndpointForUnassignedWorkOrder() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Mine ById Forbidden").email("gestor.mine.byid.forbidden@primatoos.test")
                .password("unused").role(UserRole.MANAGER).build());
        Project project = projectRepository.save(Project.builder()
                .name("Obra Mine ById Forbidden").client("Cliente Mine ById Forbidden").responsibleUser(manager)
                .status(ProjectStatus.PLANNING).build());

        WorkOrder workOrder = workOrderRepository.save(WorkOrder.builder()
                .orderNumber(workOrderRepository.nextOrderNumber())
                .project(project)
                .date(LocalDate.of(2026, 8, 1))
                .responsibleUser(manager)
                .stage("Fundação")
                .description("Concretar fundação")
                .status(WorkOrderStatus.RELEASED)
                .build());

        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Sem Vinculo ById").email("worker.mine.byid.novinculo@primatoos.test")
                .password("unused").role(UserRole.WORKER).build());
        workerRepository.save(Worker.builder().name("Colaborador Sem Vinculo ById").user(workerUser).build());

        String token = jwtService.generateToken(workerUser);

        mockMvc.perform(get("/api/v1/work-orders/mine/" + workOrder.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnPdf_whenAuthenticatedAsManager() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor PDF").email("gestor.pdf.wo@primatoos.test").password("unused").role(UserRole.MANAGER)
                .build());
        Project project = projectRepository.save(Project.builder()
                .name("Obra PDF").client("Cliente PDF").responsibleUser(manager).status(ProjectStatus.PLANNING)
                .build());
        WorkOrder workOrder = workOrderRepository.save(WorkOrder.builder()
                .orderNumber(workOrderRepository.nextOrderNumber())
                .project(project)
                .date(LocalDate.of(2026, 8, 1))
                .responsibleUser(manager)
                .stage("Fundação")
                .description("Concretar fundação")
                .status(WorkOrderStatus.RELEASED)
                .build());

        String token = jwtService.generateToken(manager);

        byte[] pdf = mockMvc.perform(get("/api/v1/work-orders/" + workOrder.getId() + "/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void shouldReturnForbidden_whenWorkerRequestsPdf() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor PDF Forbidden").email("gestor.pdf.forbidden.wo@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        Project project = projectRepository.save(Project.builder()
                .name("Obra PDF Forbidden").client("Cliente PDF Forbidden").responsibleUser(manager)
                .status(ProjectStatus.PLANNING).build());
        WorkOrder workOrder = workOrderRepository.save(WorkOrder.builder()
                .orderNumber(workOrderRepository.nextOrderNumber())
                .project(project)
                .date(LocalDate.of(2026, 8, 1))
                .responsibleUser(manager)
                .stage("Fundação")
                .description("Concretar fundação")
                .status(WorkOrderStatus.RELEASED)
                .build());

        User worker = userRepository.save(User.builder()
                .name("Colaborador PDF").email("worker.pdf.wo@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());

        String token = jwtService.generateToken(worker);

        mockMvc.perform(get("/api/v1/work-orders/" + workOrder.getId() + "/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldUploadWorkOrderPhoto_whenAuthenticatedAsManager() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto OS").email("gestor.foto.os@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        WorkOrder workOrder = seedWorkOrder(manager, "Obra Foto OS");

        given(storageService.upload(any(), any(), eq("image/jpeg")))
                .willReturn("https://cdn.primatoos.test/work-orders/" + workOrder.getId() + "/croqui.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "croqui.jpg", "image/jpeg", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/work-orders/" + workOrder.getId() + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtService.generateToken(manager)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(
                        "https://cdn.primatoos.test/work-orders/" + workOrder.getId() + "/croqui.jpg"));
    }

    @Test
    void shouldReturnForbidden_whenWorkerUploadsWorkOrderPhoto() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto OS Forb").email("gestor.foto.os.forb@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Foto OS").email("worker.foto.os@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador Foto OS").user(workerUser).build());
        // assigned, and still read-only on work order photos
        WorkOrder workOrder = seedWorkOrder(manager, "Obra Foto OS Forb", worker);

        MockMultipartFile file = new MockMultipartFile("file", "croqui.jpg", "image/jpeg", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/work-orders/" + workOrder.getId() + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtService.generateToken(workerUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(storageService, never()).upload(any(), any(), any());
    }

    @Test
    void shouldListWorkOrderPhotos_whenWorkerIsAssigned() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Lista Foto OS").email("gestor.lista.foto.os@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Lista Foto OS").email("worker.lista.foto.os@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(
                Worker.builder().name("Colaborador Lista Foto OS").user(workerUser).build());
        WorkOrder workOrder = seedWorkOrder(manager, "Obra Lista Foto OS", worker);

        given(storageService.upload(any(), any(), eq("image/jpeg")))
                .willReturn("https://cdn.primatoos.test/work-orders/" + workOrder.getId() + "/croqui.jpg");
        mockMvc.perform(multipart("/api/v1/work-orders/" + workOrder.getId() + "/photos")
                        .file(new MockMultipartFile("file", "croqui.jpg", "image/jpeg", "conteudo".getBytes()))
                        .header("Authorization", "Bearer " + jwtService.generateToken(manager)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/work-orders/" + workOrder.getId() + "/photos")
                        .header("Authorization", "Bearer " + jwtService.generateToken(workerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].url").value(
                        "https://cdn.primatoos.test/work-orders/" + workOrder.getId() + "/croqui.jpg"));
    }

    @Test
    void shouldReturnForbidden_whenUnassignedWorkerListsWorkOrderPhotos() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto OS Outsider").email("gestor.foto.os.outsider@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User outsiderUser = userRepository.save(User.builder()
                .name("Outsider Foto OS").email("worker.foto.os.outsider@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        workerRepository.save(Worker.builder().name("Outsider Foto OS").user(outsiderUser).build());
        WorkOrder workOrder = seedWorkOrder(manager, "Obra Foto OS Outsider");

        mockMvc.perform(get("/api/v1/work-orders/" + workOrder.getId() + "/photos")
                        .header("Authorization", "Bearer " + jwtService.generateToken(outsiderUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
