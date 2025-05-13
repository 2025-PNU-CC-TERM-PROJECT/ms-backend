package org.example.term_pj.controller;


import org.example.term_pj.dto.request.FileSaveRequest;
import org.example.term_pj.dto.request.UsageHistoryRequest;

import org.example.term_pj.security.services.UserDetailsImpl;
import org.example.term_pj.service.UserHistoryService;
import org.example.term_pj.service.UserService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
public class DashBoardController {

    private UserService userService;
    private UserHistoryService userHistoryService;

    public DashBoardController(UserService userService, UserHistoryService userHistoryService) {
        this.userService = userService;
        this.userHistoryService = userHistoryService;
    }



    //**파일 저장 까지 트랜잭션으로 묶어야 되는데 굳이 그정도까지 구현해야되나 싶어서 우선 controller에서 파일 저장!
    // 뒤에 있는 history DB , FILE DB 는 묶어두었습니다.
    @PostMapping("/image-class")
    public ResponseEntity<Map<String, Object>> predict(@RequestParam("file") MultipartFile file) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String fastApiUrl = "http://localhost:9000/img";

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "빈 파일입니다."));
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
        UUID uuid = UUID.randomUUID();
        String savedFilename = uuid + "_" + originalFilename;

        String rootPath = new File("").getAbsolutePath();
        File uploadDir = new File(rootPath+"/uploads"); // 여기서 rootpath는 우선 local에서 개발용으로 해둠!!
        if (!uploadDir.exists()) uploadDir.mkdirs();

        File destFile = new File(uploadDir, savedFilename);
        file.transferTo(destFile);

        FileSystemResource fileResource = new FileSystemResource(destFile);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.postForEntity(fastApiUrl, requestEntity, Map.class);

        Map<String, Object> responseBody = response.getBody();


        FileSaveRequest  fileSaveRequest = new FileSaveRequest(uuid,destFile.getAbsolutePath(),originalFilename,extension);
        UsageHistoryRequest usageHistoryRequest = new UsageHistoryRequest(userDetails.getId(),userDetails.getUsername(),"IMAGE",responseBody.get("predictions").toString());
        userHistoryService.saveUsageatImage(usageHistoryRequest, fileSaveRequest, file);

        return ResponseEntity.ok(responseBody);
    }


    @PostMapping("/text-summary")
    public ResponseEntity<?> summarizeText(@RequestBody Map<String, String> requestBody) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();


        //**인증까지 다 넣어야되나 싶기는 한데 우선 구현은 해놨습니다!
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        String text = requestBody.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "요약할 텍스트가 없습니다."));
        }

        String fastApiUrl = "http://localhost:9001/summarize";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> payload = new HashMap<>();
        payload.put("text", text);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.postForEntity(fastApiUrl, requestEntity, Map.class);
        Map<String, Object> responseBody = response.getBody();
        String summary = (String) responseBody.get("summary");

        if (!response.toString().isEmpty()) {
            UsageHistoryRequest historyRequest = new  UsageHistoryRequest(userDetails.getId(),userDetails.getUsername(),"summary",summary);
            historyRequest.setInputFile(text);
            userHistoryService.saveUsageatText(historyRequest);
        }
        return ResponseEntity.ok(response.getBody());
    }


}
