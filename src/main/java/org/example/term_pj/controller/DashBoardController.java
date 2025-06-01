package org.example.term_pj.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.term_pj.dto.request.FileSaveRequest;
import org.example.term_pj.dto.request.UsageHistoryRequest;

import org.example.term_pj.dto.response.UsageHistoryResponse;
import org.example.term_pj.security.services.UserDetailsImpl;
import org.example.term_pj.service.UserHistoryService;
import org.example.term_pj.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

@RestController
@RequestMapping("/dashboard")
public class DashBoardController {

    private UserService userService;
    private UserHistoryService userHistoryService;

//    @Value("${external.img.fastapi.url}")
//    private String imgfastApiUrl;

    @Value("${external.ai.image.url}")
    private String imageApiUrl;

    @Value("${external.ai.image.host}")
    private String imageHostHeader;

    @Value("${external.ai.text.url}")
    private String textApiUrl;

    @Value("${external.ai.text.host}")
    private String textHostHeader;



    public DashBoardController(UserService userService, UserHistoryService userHistoryService) {
        this.userService = userService;
        this.userHistoryService = userHistoryService;
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


    //**파일 저장 까지 트랜잭션으로 묶어야 되는데 굳이 그정도까지 구현해야되나 싶어서 우선 controller에서 파일 저장!
    // 뒤에 있는 history DB , FILE DB 는 묶어두었습니다.
    @PostMapping("/image-class")
    public ResponseEntity<Map<String, Object>> predict(@RequestParam("file") MultipartFile file) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }
        System.out.println("Content-Type: " + file.getContentType());

        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "유효한 이미지 파일이 아닙니다."));
        }


        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "빈 파일입니다."));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
        UUID uuid = UUID.randomUUID();
        String savedFilename = uuid + "_" + originalFilename;

        // 파일 저장 (선택사항: 기록용)
        String rootPath = new File("").getAbsolutePath();
        File uploadDir = new File(rootPath + "/uploads");
        if (!uploadDir.exists()) uploadDir.mkdirs();

        File destFile = new File(uploadDir, savedFilename);
        // Base64 인코딩
        byte[] fileBytes = file.getBytes();
        String base64Encoded = Base64.getEncoder().encodeToString(fileBytes);
        String cleaned = base64Encoded.replaceAll("\\r|\\n", "");
        file.transferTo(destFile);



        // 요청 JSON 생성
        Map<String, Object> instance = Map.of("b64", cleaned);
        Map<String, Object> requestPayload = Map.of("instances", List.of(instance));
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(requestPayload));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set("Host", imageHostHeader); // <-- Host 헤더 명시적으로 설정
        System.out.println("요청 JSON:\n" + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(requestPayload));
        System.out.println("헤더:\n" + headers.toString());
        String jsonBody = new ObjectMapper().writeValueAsString(requestPayload);

        WebClient client = WebClient.builder()
                .baseUrl(imageApiUrl)
                .defaultHeader(HttpHeaders.HOST, imageHostHeader)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> responseBody = client.post()
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        System.out.println("모델 서버 응답:\n" + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(responseBody));
//        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);


        // 기록 저장
        FileSaveRequest fileSaveRequest = new FileSaveRequest(uuid, destFile.getAbsolutePath(), originalFilename, extension);
        UsageHistoryRequest usageHistoryRequest = new UsageHistoryRequest(userDetails.getId(), userDetails.getUsername(), "IMAGE", responseBody.get("predictions").toString());
        userHistoryService.saveUsageatImage(usageHistoryRequest, fileSaveRequest, file);

        return ResponseEntity.ok(responseBody);
    }
    @PostMapping("/text-summary")
    public ResponseEntity<?> summarizeText(@RequestBody Map<String, String> requestBody) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String text = requestBody.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "요약할 텍스트가 없습니다."));
        }

        // 요청 JSON 구성
        Map<String, Object> instance = Map.of("text", text);
        Map<String, Object> requestPayload = Map.of("instances", List.of(instance));

        System.out.println("요약 요청 JSON:\n" + new ObjectMapper().valueToTree(requestPayload).toPrettyString());

        WebClient client = WebClient.builder()
                .baseUrl(textApiUrl)
                .defaultHeader(HttpHeaders.HOST, textHostHeader)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> responseBody;
        try {
            responseBody = client.post()
                    .bodyValue(requestPayload)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            System.out.println("텍스트 요약 응답:\n" + new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(responseBody));
        } catch (WebClientResponseException e) {
            System.err.println("[텍스트 요약 서버 오류 응답]");
            System.err.println("상태 코드: " + e.getStatusCode());
            System.err.println("응답 바디: " + e.getResponseBodyAsString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "텍스트 요약 모델 오류: " + e.getStatusCode()));
        } catch (Exception e) {
            System.err.println("텍스트 요약 모델 서버 연결 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Map.of("error", "텍스트 요약 모델 연결 실패"));
        }

        String summary = (String) responseBody.get("summary");
        if (summary == null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "모델 서버에서 요약 결과를 반환하지 않았습니다."));
        }

        UsageHistoryRequest historyRequest = new UsageHistoryRequest(userDetails.getId(), userDetails.getUsername(), "summary", summary);
        historyRequest.setInputFile(text);
        userHistoryService.saveUsageatText(historyRequest);

        return ResponseEntity.ok(responseBody);
    }


}
