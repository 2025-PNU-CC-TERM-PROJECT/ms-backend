package org.example.term_pj.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class UsageHistoryResponse {
    private Long id;
    private Long userId;
    private String username;
    private String modelType;
    private LocalDateTime usageTime;
    private String inputFile;
    private String resultSummary;
}
