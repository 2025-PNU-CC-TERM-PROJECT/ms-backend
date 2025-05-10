package org.example.term_pj.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "usage_history")
@NoArgsConstructor
@Getter
public class UsageHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime usageTime;

    @Column(nullable = false)
    private String modelType; // 확장 가능하도록 수정 필요함.

    @Column
    private String modelInput; // 모델 입력

    @Column(columnDefinition = "TEXT")
    private String modelOutput; // 모델 출력

    public UsageHistory(User user, String modelType, String modelInput, String modelOutput) {
        this.user = user;
        this.modelType = modelType;
        this.modelInput = modelInput;
        this.usageTime = LocalDateTime.now();
    }



}
