package com.primatoos.backend.service;

import com.primatoos.backend.dto.user.UserCreateRequest;
import com.primatoos.backend.dto.user.UserResponse;
import com.primatoos.backend.exception.BusinessRuleException;
import com.primatoos.backend.mapper.UserMapper;
import com.primatoos.backend.model.User;
import com.primatoos.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserResponse create(UserCreateRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessRuleException("Este email já está em uso");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }
}
