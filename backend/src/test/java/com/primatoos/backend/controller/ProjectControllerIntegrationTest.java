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

import java.time.LocalDate;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectControllerIntegrationTest {

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

    private Project seedProject(User manager, String name) {
        return projectRepository.save(Project.builder()
                .name(name).client("Cliente Foto").responsibleUser(manager).status(ProjectStatus.PLANNING)
                .build());
    }

    private void assignWorkerToProject(User manager, Project project, Worker worker) {
        workOrderRepository.save(WorkOrder.builder()
                .orderNumber(workOrderRepository.nextOrderNumber())
                .project(project)
                .date(LocalDate.of(2026, 8, 1))
                .responsibleUser(manager)
                .stage("Fundação")
                .description("Concretar fundação")
                .status(WorkOrderStatus.IN_PROGRESS)
                .assignedWorkers(Set.of(worker))
                .build());
    }

    @Test
    void shouldCreateProject_whenAuthenticatedAsManager() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Teste")
                .email("gestor.projects@primatoos.test")
                .password("unused")
                .role(UserRole.MANAGER)
                .build());

        String token = jwtService.generateToken(manager);

        String body = """
                {"name": "Obra Teste", "client": "Cliente Teste", "responsibleUserId": %d}
                """.formatted(manager.getId());

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Obra Teste"))
                .andExpect(jsonPath("$.status").value("PLANNING"));
    }

    @Test
    void shouldReturnForbidden_whenCreatingProjectAsWorker() throws Exception {
        User worker = userRepository.save(User.builder()
                .name("Colaborador Teste")
                .email("worker.projects@primatoos.test")
                .password("unused")
                .role(UserRole.WORKER)
                .build());

        String token = jwtService.generateToken(worker);

        String body = """
                {"name": "Obra Teste", "client": "Cliente Teste", "responsibleUserId": %d}
                """.formatted(worker.getId());

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnForbidden_whenListingProjectsAsWorker() throws Exception {
        User worker = userRepository.save(User.builder()
                .name("Colaborador Lista")
                .email("worker.list.projects@primatoos.test")
                .password("unused")
                .role(UserRole.WORKER)
                .build());

        String token = jwtService.generateToken(worker);

        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUploadProjectPhoto_whenAuthenticatedAsManager() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto Obra").email("gestor.foto.obra@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        Project project = seedProject(manager, "Obra Com Foto");

        String token = jwtService.generateToken(manager);
        given(storageService.upload(any(), any(), eq("image/jpeg")))
                .willReturn("https://cdn.primatoos.test/projects/" + project.getId() + "/planta.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "planta.jpg", "image/jpeg", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/projects/" + project.getId() + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(
                        "https://cdn.primatoos.test/projects/" + project.getId() + "/planta.jpg"));
    }

    @Test
    void shouldReturnForbidden_whenWorkerUploadsProjectPhoto() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Foto Obra Forb").email("gestor.foto.obra.forb@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Foto Obra").email("worker.foto.obra@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(Worker.builder().name("Colaborador Foto Obra").user(workerUser).build());
        Project project = seedProject(manager, "Obra Sem Upload");
        // even assigned, a worker is read-only on project photos
        assignWorkerToProject(manager, project, worker);

        String token = jwtService.generateToken(workerUser);
        MockMultipartFile file = new MockMultipartFile("file", "planta.jpg", "image/jpeg", "conteudo".getBytes());

        mockMvc.perform(multipart("/api/v1/projects/" + project.getId() + "/photos")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(storageService, never()).upload(any(), any(), any());
    }

    @Test
    void shouldListProjectPhotos_whenWorkerIsAssignedToAWorkOrderInThatProject() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Lista Foto").email("gestor.lista.foto@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Lista Foto").email("worker.lista.foto@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(
                Worker.builder().name("Colaborador Lista Foto").user(workerUser).build());
        Project project = seedProject(manager, "Obra Visivel");
        assignWorkerToProject(manager, project, worker);

        given(storageService.upload(any(), any(), eq("image/jpeg")))
                .willReturn("https://cdn.primatoos.test/projects/" + project.getId() + "/planta.jpg");
        mockMvc.perform(multipart("/api/v1/projects/" + project.getId() + "/photos")
                        .file(new MockMultipartFile("file", "planta.jpg", "image/jpeg", "conteudo".getBytes()))
                        .header("Authorization", "Bearer " + jwtService.generateToken(manager)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/projects/" + project.getId() + "/photos")
                        .header("Authorization", "Bearer " + jwtService.generateToken(workerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].url").value(
                        "https://cdn.primatoos.test/projects/" + project.getId() + "/planta.jpg"));
    }

    @Test
    void shouldReturnForbidden_whenWorkerListsPhotosOfAProjectTheyHaveNoWorkOrderIn() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Outra Obra").email("gestor.outra.obra@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Outra Obra").email("worker.outra.obra@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(
                Worker.builder().name("Colaborador Outra Obra").user(workerUser).build());

        Project assignedProject = seedProject(manager, "Obra Do Colaborador");
        assignWorkerToProject(manager, assignedProject, worker);
        Project strangerProject = seedProject(manager, "Obra Alheia");

        mockMvc.perform(get("/api/v1/projects/" + strangerProject.getId() + "/photos")
                        .header("Authorization", "Bearer " + jwtService.generateToken(workerUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnProject_whenWorkerCallsMineByIdAndIsAssignedInThatProject() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Mine Obra").email("gestor.mine.obra@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Mine Obra").email("worker.mine.obra@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        Worker worker = workerRepository.save(
                Worker.builder().name("Colaborador Mine Obra").user(workerUser).build());
        Project project = seedProject(manager, "Obra Do Worker");
        assignWorkerToProject(manager, project, worker);

        mockMvc.perform(get("/api/v1/projects/mine/" + project.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(workerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()))
                .andExpect(jsonPath("$.name").value("Obra Do Worker"));
    }

    @Test
    void shouldReturnForbidden_whenWorkerCallsMineByIdForAnUnrelatedProject() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Mine Alheia").email("gestor.mine.alheia@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        User workerUser = userRepository.save(User.builder()
                .name("Colaborador Mine Alheia").email("worker.mine.alheia@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());
        workerRepository.save(Worker.builder().name("Colaborador Mine Alheia").user(workerUser).build());
        Project strangerProject = seedProject(manager, "Obra Alheia Mine");

        mockMvc.perform(get("/api/v1/projects/mine/" + strangerProject.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(workerUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturnProject_whenManagerCallsMineById() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Mine Proprio").email("gestor.mine.proprio@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        Project project = seedProject(manager, "Obra Do Gestor");

        mockMvc.perform(get("/api/v1/projects/mine/" + project.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId()));
    }
}
