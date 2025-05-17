package org.example.term_pj.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.term_pj.dto.response.JwtResponse;
import org.example.term_pj.dto.response.UsageHistoryResponse;
import org.example.term_pj.security.services.UserDetailsImpl;
import org.example.term_pj.service.UserHistoryService;
import org.example.term_pj.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final UserHistoryService userHistoryService;

    public UserController( UserService userService, UserHistoryService userHistoryService) {
        this.userService = userService;
        this.userHistoryService = userHistoryService;
    }

    @GetMapping
    public ResponseEntity<?> getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "id", userDetails.getId(),
                "username", userDetails.getUsername(),
                "email", userDetails.getEmail(),
                "role", userDetails.getRole()
        ));
    }


    @GetMapping("/usage-history")
    public ResponseEntity<List<UsageHistoryResponse>> getUserHistory(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        List<UsageHistoryResponse> history = userHistoryService.getUsageHistoryByUserId(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/usage-history/image-meta/{id}")
    public ResponseEntity<UsageHistoryResponse> getImageMeta(@PathVariable Long id, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        UsageHistoryResponse response = userHistoryService.getImageMetaWithResult(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/usage-history/image/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        Resource image = userHistoryService.getImageResourceByHistoryId(id, userId);

        // 파일명에서 확장자 추출,,,,,
        String filename = image.getFilename();
        MediaType contentType;

        if (filename == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        } else if (filename.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG;
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG;
        } else if (filename.endsWith(".webp")) {
            contentType = MediaType.valueOf("image/webp");
        } else {
            contentType = MediaType.APPLICATION_OCTET_STREAM; // 기본값으로,,,
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .body(image);
    }


}
