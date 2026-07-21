package com.primatoos.backend.service;

import com.primatoos.backend.dto.LoginRequest;
import com.primatoos.backend.dto.LoginResponse;
import com.primatoos.backend.exception.InvalidCredentialsException;
import com.primatoos.backend.model.User;
import com.primatoos.backend.repository.UserRepository;
import com.primatoos.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(jwtService.generateToken(user));
    }
}
