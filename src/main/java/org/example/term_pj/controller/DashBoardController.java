package org.example.term_pj.controller;

import org.example.term_pj.security.services.UserDetailsImpl;
import org.example.term_pj.service.InferenceService;
import org.example.term_pj.service.UserService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashBoardController {

    private final UserService userService;
    private final InferenceService inferenceService;

    public DashBoardController(UserService userService, InferenceService inferenceService) {
        this.userService = userService;
        this.inferenceService = inferenceService;
    }

    @GetMapping("/usage-stats")
    public ResponseEntity<?> getUsageStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // user 테이블에서 누적 count 조회
        Map<String, Object> usageStats = userService.getUsageStats(userDetails.getId());

        return ResponseEntity.ok(usageStats);
    }

    @PostMapping("/image-class")
    public ResponseEntity<Map<String, Object>> predict(@RequestParam("file") MultipartFile file, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(inferenceService.classifyImage(file, userDetails));
    }


    @PostMapping("/text-summary")
    public ResponseEntity<?> summarizeText(@RequestBody Map<String, String> requestBody, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(inferenceService.summarizeText(requestBody.get("text"), userDetails));
    }


}
