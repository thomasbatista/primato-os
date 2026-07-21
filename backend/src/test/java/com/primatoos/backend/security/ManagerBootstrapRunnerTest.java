package com.primatoos.backend.security;

import com.primatoos.backend.dto.user.UserCreateRequest;
import com.primatoos.backend.dto.user.UserResponse;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private ManagerBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        runner = new ManagerBootstrapRunner(userRepository, userService);
        ReflectionTestUtils.setField(runner, "bootstrapName", "Administrador");
    }

    @Test
    void shouldSkip_whenUsersAlreadyExist() throws Exception {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "admin@primatoos.test");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "senha12345");
        given(userRepository.count()).willReturn(1L);

        runner.run(null);

        verify(userService, never()).create(any());
    }

    @Test
    void shouldSkip_whenBootstrapCredentialsAreNotConfigured() throws Exception {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "");
        given(userRepository.count()).willReturn(0L);

        runner.run(null);

        verify(userService, never()).create(any());
    }

    @Test
    void shouldCreateFirstManager_whenNoUsersExistAndCredentialsAreConfigured() throws Exception {
        ReflectionTestUtils.setField(runner, "bootstrapEmail", "admin@primatoos.test");
        ReflectionTestUtils.setField(runner, "bootstrapPassword", "senha12345");
        given(userRepository.count()).willReturn(0L);
        given(userService.create(any(UserCreateRequest.class))).willReturn(
                new UserResponse(1L, "Administrador", "admin@primatoos.test", UserRole.MANAGER, null));

        runner.run(null);

        ArgumentCaptor<UserCreateRequest> captor = ArgumentCaptor.forClass(UserCreateRequest.class);
        verify(userService).create(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("admin@primatoos.test");
        assertThat(captor.getValue().role()).isEqualTo(UserRole.MANAGER);
    }
}
