package com.cavsteek.jobapplicationtrackerapi.auth;

import com.cavsteek.jobapplicationtrackerapi.auth.dto.AuthResponse;
import com.cavsteek.jobapplicationtrackerapi.auth.dto.LoginRequest;
import com.cavsteek.jobapplicationtrackerapi.auth.dto.RegisterRequest;
import com.cavsteek.jobapplicationtrackerapi.common.ConflictException;
import com.cavsteek.jobapplicationtrackerapi.user.User;
import com.cavsteek.jobapplicationtrackerapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .build();

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // two requests registered the same email at the same moment
            throw new ConflictException("Email already registered");
        }

        return tokenFor(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalize(req.email()))
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return tokenFor(user);
    }

    private AuthResponse tokenFor(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtProperties.expirationMinutes() * 60);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}