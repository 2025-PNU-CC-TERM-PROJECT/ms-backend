package org.example.term_pj.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
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

@RestController
@RequestMapping("/predict")
public class InferenceController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> predict(@RequestParam("file") MultipartFile file) throws Exception {
//        String ocrApiUrl = "http://ocr-api:9000/ocr";
        String ocrApiUrl = "http:/localhost:9000/ocr";
        // 1. MultipartFile → 임시 파일 저장
        File tempFile = File.createTempFile("upload-", Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        // 2. FileSystemResource로 multipart 구성
        FileSystemResource fileResource = new FileSystemResource(tempFile);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        RestTemplate restTemplate = new RestTemplate();

        // 3. FastAPI OCR 서버 호출
        ResponseEntity<Map> ocrResponse = restTemplate.postForEntity(ocrApiUrl, requestEntity, Map.class);
        Map<String, Object> ocrBody = ocrResponse.getBody();

        // 4. 결과 정제
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("filename", file.getOriginalFilename());
        finalResult.put("store", ocrBody.getOrDefault("store", "Unknown"));
        finalResult.put("date", ocrBody.getOrDefault("date", "Unknown"));
        finalResult.put("total", ocrBody.getOrDefault("total", "Unknown"));

        // 5. 임시 파일 삭제
        if (tempFile.exists()) tempFile.delete();

        return ResponseEntity.ok(finalResult);
    }
}
