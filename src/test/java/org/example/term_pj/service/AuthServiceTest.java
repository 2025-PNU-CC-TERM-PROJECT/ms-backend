package org.example.term_pj.service;

import org.example.term_pj.dto.request.SignupRequest;
import org.example.term_pj.model.Role;
import org.example.term_pj.model.User;
import org.example.term_pj.repository.RoleRepository;
import org.example.term_pj.repository.UsageHistoryRepository;
import org.example.term_pj.repository.UserRepository;
import org.example.term_pj.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void registerUserAlwaysAssignsUserRoleEvenWhenAdminRoleRequested() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UsageHistoryRepository usageHistoryRepository = mock(UsageHistoryRepository.class);

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName(Role.ERole.ROLE_USER)).thenReturn(Optional.of(new Role(Role.ERole.ROLE_USER)));
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");

        AuthService authService = new AuthService(
                authenticationManager,
                userRepository,
                roleRepository,
                jwtTokenProvider,
                passwordEncoder,
                usageHistoryRepository
        );

        SignupRequest request = new SignupRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("Password1");
        request.setRole("admin");

        authService.registerUser(request);

        verify(roleRepository).findByName(Role.ERole.ROLE_USER);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole().getName()).isEqualTo(Role.ERole.ROLE_USER);
    }

    @Test
    void registerUserAlwaysAssignsUserRoleEvenForPrivilegedEmail() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UsageHistoryRepository usageHistoryRepository = mock(UsageHistoryRepository.class);

        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@pusan.ac.kr")).thenReturn(false);
        when(roleRepository.findByName(Role.ERole.ROLE_USER)).thenReturn(Optional.of(new Role(Role.ERole.ROLE_USER)));
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        AuthService authService = new AuthService(
                authenticationManager,
                userRepository,
                roleRepository,
                jwtTokenProvider,
                passwordEncoder,
                usageHistoryRepository
        );

        SignupRequest request = new SignupRequest();
        request.setUsername("admin");
        request.setEmail("admin@pusan.ac.kr");
        request.setPassword("Password1");
        request.setRole("admin");

        authService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole().getName()).isEqualTo(Role.ERole.ROLE_USER);
    }
}
