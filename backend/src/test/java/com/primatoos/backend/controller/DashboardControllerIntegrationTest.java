package com.primatoos.backend.controller;

import com.primatoos.backend.model.Project;
import com.primatoos.backend.model.ProjectStatus;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.repository.ProjectRepository;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldReturnDashboard_whenAuthenticatedAsManager() throws Exception {
        User manager = userRepository.save(User.builder()
                .name("Gestor Dashboard").email("gestor.dashboard@primatoos.test").password("unused")
                .role(UserRole.MANAGER).build());
        projectRepository.save(Project.builder()
                .name("Obra Dashboard").client("Cliente Dashboard").responsibleUser(manager)
                .status(ProjectStatus.IN_PROGRESS).build());

        String token = jwtService.generateToken(manager);

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeProjects.count").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.activeProjects.items").isArray())
                .andExpect(jsonPath("$.todayWorkOrders.count").exists())
                .andExpect(jsonPath("$.pendingDailyReports.count").exists())
                .andExpect(jsonPath("$.openMaterialRequests.count").exists())
                .andExpect(jsonPath("$.materialRequestsAwaitingDelivery.count").exists());
    }

    @Test
    void shouldReturnForbidden_whenAuthenticatedAsWorker() throws Exception {
        User worker = userRepository.save(User.builder()
                .name("Colaborador Dashboard").email("worker.dashboard@primatoos.test").password("unused")
                .role(UserRole.WORKER).build());

        String token = jwtService.generateToken(worker);

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
