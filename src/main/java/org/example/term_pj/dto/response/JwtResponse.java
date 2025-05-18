package org.example.term_pj.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String role;  // 단일 역할
    private int usageCount;       // 총 사용횟수
    private int imageCount;       // 이미지 분류 사용 횟수
    private int textCount;        // 텍스트 요약 사용 횟수


    public JwtResponse(String accessToken, Long id, String username, String email, String role,int usageCount, int imageCount, int textCount) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.usageCount = usageCount;
        this.imageCount = imageCount;
        this.textCount = textCount ;
    }
}