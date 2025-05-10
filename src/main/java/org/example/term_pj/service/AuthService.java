package org.example.term_pj.service;

import jakarta.transaction.Transactional;
import org.example.term_pj.dto.request.LoginRequest;
import org.example.term_pj.dto.request.SignupRequest;
import org.example.term_pj.dto.response.JwtResponse;
import org.example.term_pj.dto.response.MessageResponse;
import org.example.term_pj.model.Role;
import org.example.term_pj.model.User;
import org.example.term_pj.repository.RoleRepository;
import org.example.term_pj.repository.UserRepository;
import org.example.term_pj.security.JwtTokenProvider;
import org.example.term_pj.security.services.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder encoder,
            JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * * 로그인 처리
     * @param loginRequest 로그인 요청 객체
     * @return 토큰과 사용자 정보가 포함된 응답
     */
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        // 인증 객체 생성
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        // 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성
        String jwt = jwtTokenProvider.generateJwtToken(authentication);

        // 인증된 사용자 정보 가져오기
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 응답 객체 생성 및 반환
        return new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getRole());
    }

    /**
     * 회원 가입 처리
     * @param signUpRequest 회원가입 요청 객체
     * @return 회원가입 결과 메시지
     */
    @Transactional
    public MessageResponse registerUser(SignupRequest signUpRequest) {
        // 사용자명 중복 확인
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return new MessageResponse("Error: Username is already taken!");
        }

        // 이메일 중복 확인
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new MessageResponse("Error: Email is already in use!");
        }

        Role.ERole roleName = Role.ERole.ROLE_USER;

        // 요청에 역할이 있으면 해당 역할 확인
        if (signUpRequest.getRole() != null && !signUpRequest.getRole().isEmpty()) {
            String requestedRole = signUpRequest.getRole();

            // ADMIN 역할 요청 처리
            if ("admin".equalsIgnoreCase(requestedRole)) {
                roleName = Role.ERole.ROLE_ADMIN;
            }
        }

        // 역할 조회
        Role userRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                passwordEncoder.encode(signUpRequest.getPassword()),
                userRole
        );

        userRepository.save(user);

        return new MessageResponse("User registered successfully!");
    }
}
