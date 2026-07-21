package com.primatoos.backend.service;

import com.primatoos.backend.dto.user.UserCreateRequest;
import com.primatoos.backend.dto.user.UserResponse;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.mapper.UserMapper;
import com.primatoos.backend.model.User;
import com.primatoos.backend.model.UserRole;
import com.primatoos.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, new UserMapper());
    }

    @Test
    void shouldCreateUser_whenEmailIsNotTaken() {
        given(userRepository.findByEmail("novo@primatoos.test")).willReturn(Optional.empty());
        given(passwordEncoder.encode("senha12345")).willReturn("hashed-password");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        UserCreateRequest request = new UserCreateRequest("Novo Usuario", "novo@primatoos.test", "senha12345",
                UserRole.WORKER);

        UserResponse response = userService.create(request);

        assertThat(response.email()).isEqualTo("novo@primatoos.test");
        assertThat(response.role()).isEqualTo(UserRole.WORKER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-password");
    }

    @Test
    void shouldThrowBusinessRuleException_whenEmailAlreadyInUse() {
        User existing = User.builder()
                .id(1L).name("Existente").email("existe@primatoos.test").password("hash").role(UserRole.MANAGER)
                .build();
        given(userRepository.findByEmail("existe@primatoos.test")).willReturn(Optional.of(existing));

        UserCreateRequest request = new UserCreateRequest("Outro", "existe@primatoos.test", "senha12345",
                UserRole.WORKER);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnPaginatedUsers() {
        User user = User.builder()
                .id(1L).name("Usuario").email("u@primatoos.test").password("hash").role(UserRole.MANAGER).build();
        Pageable pageable = PageRequest.of(0, 10);
        given(userRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(user)));

        Page<UserResponse> page = userService.findAll(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).email()).isEqualTo("u@primatoos.test");
    }
}
