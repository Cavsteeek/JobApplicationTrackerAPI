package com.cavsteek.jobapplicationtrackerapi.auth;

import com.cavsteek.jobapplicationtrackerapi.auth.dto.AuthResponse;
import com.cavsteek.jobapplicationtrackerapi.auth.dto.LoginRequest;
import com.cavsteek.jobapplicationtrackerapi.auth.dto.RegisterRequest;
import com.cavsteek.jobapplicationtrackerapi.common.ConflictException;
import com.cavsteek.jobapplicationtrackerapi.user.User;
import com.cavsteek.jobapplicationtrackerapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService,
                new JwtProperties("irrelevant-in-this-test", 60));
    }

    // ---------- register ----------

    @Test
    void register_normalizesEmail_hashesPassword_andReturnsToken() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("token-123");

        AuthResponse response = authService.register(new RegisterRequest("  New@Example.com ", "password123"));

        assertThat(response.accessToken()).isEqualTo("token-123");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("HASHED");
    }

    @Test
    void register_whenEmailExists_throwsConflict() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("taken@example.com", "password123")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenDatabaseRejectsDuplicate_throwsConflict() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> authService.register(new RegisterRequest("race@example.com", "password123")))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- login ----------

    @Test
    void login_withValidCredentials_returnsToken() {
        User user = User.builder().id(1L).email("a@example.com").passwordHash("HASH").build();
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "HASH")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token-abc");

        AuthResponse response = authService.login(new LoginRequest("A@Example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("token-abc");
    }

    @Test
    void login_withWrongPassword_throwsBadCredentials() {
        User user = User.builder().id(1L).email("a@example.com").passwordHash("HASH").build();
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("a@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_withUnknownEmail_throwsBadCredentials() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService);
    }
}