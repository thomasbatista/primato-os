package com.primatoos.backend.service;

import com.primatoos.backend.dto.auth.LoginRequest;
import com.primatoos.backend.dto.auth.LoginResponse;
import com.primatoos.backend.exception.InvalidCredentialsException;
import com.primatoos.backend.model.User;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(jwtService.generateToken(user));
    }
}
