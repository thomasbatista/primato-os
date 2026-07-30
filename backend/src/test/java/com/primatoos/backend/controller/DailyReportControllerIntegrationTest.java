package com.primatoos.backend.controller;

import com.primatoos.backend.model.DailyReportPhoto;
import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.model.WorkOrder;
import com.primatoos.backend.model.WorkOrderStatus;
import com.primatoos.backend.model.Worker;
import com.primatoos.backend.repository.DailyReportPhotoRepository;
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

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    private DailyReportPhotoRepository dailyReportPhotoRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private StorageService storageService;

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

    private Long createDraftReport(String workerToken, Long workOrderId) throws Exception {
        String createBody = """
                {"workOrderId": %d, "date": "2026-08-01"}
                """.formatted(workOrderId);

        String response = mockMvc.perform(post("/api/v1/daily-reports")
                        .header("Authorization", "Bearer " + workerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return com.jayway.jsonpath.JsonPath.parse(response).read("$.id", Long.class);
    }

    @Test
    void shouldUploadPhoto_whenAuthenticatedAsAssignedWorker() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto").email("gestor.foto@primatoos.test").password("unused").role(UserRole.MANAGER)
                .build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Foto").email("worker.foto@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador Foto").user(workerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, worker);

        String workerToken = jwtService.generateToken(workerUser);
        Long reportId = createDraftReport(workerToken, workOrder.getId());

        given(storageService.upload(any(), any(), eq("image/jpeg")))
                .willReturn("https://cdn.primatoos.test/daily-reports/" + reportId + "/generated.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/v1/daily-reports/" + reportId + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(
                        "https://cdn.primatoos.test/daily-reports/" + reportId + "/generated.jpg"))
                .andReturn().getResponse().getContentAsString();

        Long photoId = com.jayway.jsonpath.JsonPath.parse(uploadResponse).read("$.id", Long.class);

        // verified directly against the repository, not via a nested GET: within one @Transactional
        // test method every MockMvc call shares a single Hibernate session, so the parent DailyReport's
        // already-loaded (LAZY, non-cascaded) photos collection wouldn't see this new row anyway — a
        // test-transaction artifact, not something that happens across separate real HTTP requests
        assertThat(dailyReportPhotoRepository.findById(photoId))
                .isPresent()
                .get()
                .extracting(DailyReportPhoto::getUrl)
                .isEqualTo("https://cdn.primatoos.test/daily-reports/" + reportId + "/generated.jpg");
    }

    @Test
    void shouldReturnForbidden_whenManagerUploadsPhoto() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto Forbidden").email("gestor.foto.forbidden@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Foto Forbidden").email("worker.foto.forbidden@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(
                Worker.builder().name("Colaborador Foto Forbidden").user(workerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, worker);

        String workerToken = jwtService.generateToken(workerUser);
        Long reportId = createDraftReport(workerToken, workOrder.getId());

        String managerToken = jwtService.generateToken(manager);
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/daily-reports/" + reportId + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(storageService, never()).upload(any(), any(), any());
    }

    @Test
    void shouldReturnForbidden_whenUnassignedWorkerUploadsPhoto() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto Outsider").email("gestor.foto.outsider@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User assignedWorkerUser = userRepository.save(User.builder()
                .name("Colaborador Atribuido").email("worker.foto.assigned@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker assignedWorker = workerRepository.save(
                Worker.builder().name("Colaborador Atribuido").user(assignedWorkerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, assignedWorker);

        String assignedToken = jwtService.generateToken(assignedWorkerUser);
        Long reportId = createDraftReport(assignedToken, workOrder.getId());

        User outsiderUser = userRepository.save(User.builder()
                .name("Outsider Foto").email("worker.foto.outsider@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        workerRepository.save(Worker.builder().name("Outsider Foto").user(outsiderUser).build());
        String outsiderToken = jwtService.generateToken(outsiderUser);

        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/daily-reports/" + reportId + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(storageService, never()).upload(any(), any(), any());
    }

    @Test
    void shouldReturnBadRequest_whenPhotoFileTypeIsInvalid() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto Tipo").email("gestor.foto.tipo@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Foto Tipo").email("worker.foto.tipo@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador Foto Tipo").user(workerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, worker);

        String workerToken = jwtService.generateToken(workerUser);
        Long reportId = createDraftReport(workerToken, workOrder.getId());

        MockMultipartFile file = new MockMultipartFile("file", "documento.pdf", "application/pdf",
                "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/daily-reports/" + reportId + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(storageService, never()).upload(any(), any(), any());
    }

    @Test
    void shouldReturnBadRequest_whenPhotoFileExceedsMaxSize() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto Tamanho").email("gestor.foto.tamanho@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Foto Tamanho").email("worker.foto.tamanho@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(
                Worker.builder().name("Colaborador Foto Tamanho").user(workerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, worker);

        String workerToken = jwtService.generateToken(workerUser);
        Long reportId = createDraftReport(workerToken, workOrder.getId());

        byte[] oversized = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", oversized);

        mockMvc.perform(multipart("/api/v1/daily-reports/" + reportId + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(storageService, never()).upload(any(), any(), any());
    }

    @Test
    void shouldDeletePhoto_whenAuthenticatedAsAssignedWorker() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Delete Foto").email("gestor.deletefoto@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Delete Foto").email("worker.deletefoto@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(
                Worker.builder().name("Colaborador Delete Foto").user(workerUser).build());
        WorkOrder workOrder = createReportableWorkOrder(manager, worker);

        String workerToken = jwtService.generateToken(workerUser);
        Long reportId = createDraftReport(workerToken, workOrder.getId());

        given(storageService.upload(any(), any(), eq("image/jpeg")))
                .willReturn("https://cdn.primatoos.test/daily-reports/" + reportId + "/generated.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

        String uploadResponse = mockMvc.perform(multipart("/api/v1/daily-reports/" + reportId + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long photoId = com.jayway.jsonpath.JsonPath.parse(uploadResponse).read("$.id", Long.class);

        mockMvc.perform(delete("/api/v1/daily-reports/" + reportId + "/photos/" + photoId)
                        .header("Authorization", "Bearer " + workerToken))
                .andExpect(status().isNoContent());

        verify(storageService).delete("https://cdn.primatoos.test/daily-reports/" + reportId + "/generated.jpg");
    }
}
