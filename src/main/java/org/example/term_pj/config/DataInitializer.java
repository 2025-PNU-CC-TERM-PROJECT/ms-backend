package org.example.term_pj.config;

import jakarta.annotation.PostConstruct;
import org.example.term_pj.model.Role;
import org.example.term_pj.repository.RoleRepository;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {
    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    public void init() {
        // ROLE_USER 생성
        if (roleRepository.findByName(Role.ERole.ROLE_USER).isEmpty()) {
            Role userRole = new Role(Role.ERole.ROLE_USER);
            roleRepository.save(userRole);
        }

        // ROLE_ADMIN 생성
        if (roleRepository.findByName(Role.ERole.ROLE_ADMIN).isEmpty()) {
            Role adminRole = new Role(Role.ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);
        }
    }
}