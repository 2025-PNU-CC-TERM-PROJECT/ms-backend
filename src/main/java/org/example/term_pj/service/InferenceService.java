package org.example.term_pj.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.term_pj.dto.request.FileSaveRequest;
import org.example.term_pj.dto.request.UsageHistoryRequest;
import org.example.term_pj.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class InferenceService {
    private static final Logger logger = LoggerFactory.getLogger(InferenceService.class);

    private final UserHistoryService userHistoryService;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final HttpClient httpClient;

    @Value("${external.ai.image.url}")
    private String imageApiUrl;

    @Value("${external.ai.image.host}")
    private String imageHostHeader;

    @Value("${external.ai.text.url}")
    private String textApiUrl;

    @Value("${external.ai.text.host}")
    private String textHostHeader;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDirPath;

    @Value("${internal.inference.url:}")
    private String internalInferenceUrl;

    public InferenceService(
            UserHistoryService userHistoryService,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder
    ) {
        this.userHistoryService = userHistoryService;
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;
        this.httpClient = HttpClient.create()
                .protocol(HttpProtocol.HTTP11)
                .responseTimeout(Duration.ofSeconds(30));
    }

    public Map<String, Object> classifyImage(MultipartFile file, UserDetailsImpl userDetails) {
        validateImage(file);

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "filename is required");
        String extension = extractExtension(originalFilename);
        UUID uuid = UUID.randomUUID();
        String savedFilename = uuid + "_" + originalFilename;
        File destFile = storeUpload(file, savedFilename);

        String encodedImage = encodeBase64(file);
        Map<String, Object> requestPayload = Map.of(
                "instances", List.of(Map.of("b64", encodedImage))
        );

        Map<String, Object> responseBody = callInferenceOrModel(
                "/image-class",
                Map.of("b64", encodedImage),
                imageApiUrl,
                imageHostHeader,
                requestPayload,
                "image"
        );

        Object predictions = responseBody.get("predictions");
        if (predictions == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "모델 서버에서 예측 결과를 반환하지 않았습니다.");
        }

        FileSaveRequest fileSaveRequest = new FileSaveRequest(uuid, destFile.getAbsolutePath(), originalFilename, extension);
        UsageHistoryRequest usageHistoryRequest = new UsageHistoryRequest(
                userDetails.getId(),
                userDetails.getUsername(),
                "IMAGE",
                predictions.toString()
        );
        userHistoryService.saveUsageatImage(usageHistoryRequest, fileSaveRequest, file);

        return responseBody;
    }

    public Map<String, Object> summarizeText(String text, UserDetailsImpl userDetails) {
        if (text == null || text.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요약할 텍스트가 없습니다.");
        }

        Map<String, Object> requestPayload = Map.of(
                "instances", List.of(Map.of("text", text))
        );

        Map<String, Object> responseBody = callInferenceOrModel(
                "/text-summary",
                Map.of("text", text),
                textApiUrl,
                textHostHeader,
                requestPayload,
                "text"
        );

        Object summary = responseBody.get("summary");
        if (!(summary instanceof String summaryText)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "모델 서버에서 요약 결과를 반환하지 않았습니다.");
        }

        UsageHistoryRequest historyRequest = new UsageHistoryRequest(
                userDetails.getId(),
                userDetails.getUsername(),
                "summary",
                summaryText
        );
        historyRequest.setInputFile(text);
        userHistoryService.saveUsageatText(historyRequest);

        return responseBody;
    }

    private Map<String, Object> callModel(String apiUrl, String hostHeader, Map<String, Object> payload, String modelType) {
        WebClient client = webClientBuilder
                .clone()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.HOST, hostHeader)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return client.post()
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(35));
        } catch (WebClientResponseException e) {
            logger.warn("{} model returned error status={}", modelType, e.getStatusCode());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "모델 서버 오류: " + e.getStatusCode());
        } catch (WebClientRequestException e) {
            logger.warn("{} model request failed: {}", modelType, e.getMessage());
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "모델 서버에 접근할 수 없습니다.");
        } catch (Exception e) {
            logger.error("{} model call failed", modelType, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
        }
    }

    private Map<String, Object> callInferenceOrModel(
            String path,
            Map<String, Object> inferencePayload,
            String modelUrl,
            String hostHeader,
            Map<String, Object> modelPayload,
            String modelType
    ) {
        if (internalInferenceUrl == null || internalInferenceUrl.isBlank()) {
            return callModel(modelUrl, hostHeader, modelPayload, modelType);
        }

        return callInternalInference(path, inferencePayload, modelType);
    }

    private Map<String, Object> callInternalInference(String path, Map<String, Object> payload, String modelType) {
        WebClient client = webClientBuilder
                .clone()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(internalInferenceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return client.post()
                    .uri(path)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(Duration.ofSeconds(35));
        } catch (WebClientResponseException e) {
            logger.warn("internal {} inference returned error status={}", modelType, e.getStatusCode());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "추론 서비스 오류: " + e.getStatusCode());
        } catch (WebClientRequestException e) {
            logger.warn("internal {} inference request failed: {}", modelType, e.getMessage());
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "추론 서비스에 접근할 수 없습니다.");
        } catch (Exception e) {
            logger.error("internal {} inference call failed", modelType, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "빈 파일입니다.");
        }

        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효한 이미지 파일이 아닙니다.");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 읽을 수 없습니다.", e);
        }
    }

    private String encodeBase64(MultipartFile file) {
        try {
            return Base64.getEncoder().encodeToString(file.getBytes()).replaceAll("\\r|\\n", "");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 읽을 수 없습니다.", e);
        }
    }

    private File storeUpload(MultipartFile file, String savedFilename) {
        File uploadDir = new File(uploadDirPath);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "업로드 디렉터리를 생성할 수 없습니다.");
        }

        File destFile = new File(uploadDir, savedFilename);
        try {
            file.transferTo(destFile);
            return destFile;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.", e);
        }
    }

    private String extractExtension(String originalFilename) {
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase();
    }
}
