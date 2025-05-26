//package org.example.term_pj.service;
//
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.r
//
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class AiModelClient {
//
//    private final WebClient webClient;
//
//    public AiModelClient() {
//        this.webClient = WebClient.builder()
//                .baseUrl("http://34.92.136.163")
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .defaultHeader(HttpHeaders.HOST, "ai-image-serving.ms-models.example.com") // Host 강제 지정
//                .build();
//    }
//
//    public Map<String, Object> predict(String base64EncodedImage) {
//        Map<String, Object> payload = Map.of(
//                "instances", List.of(Map.of("b64", base64EncodedImage))
//        );
//
//        return webClient.post()
//                .uri("/v1/models/mobilenet:predict")
//                .bodyValue(payload)
//                .retrieve()
//                .bodyToMono(Map.class)
//                .block(); // 블로킹 (Spring MVC에서 사용 중이면 block 써도 무방)
//    }
//}
//
