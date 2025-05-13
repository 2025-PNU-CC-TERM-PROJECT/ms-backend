package org.example.term_pj.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UsageHistoryRequest {
    private Long id;
    private Long userId;
    private String username;
    private String modelType; //model type 부분 ENUM 으로 해야될 거 같은데 어차피 2개기도 하구,디비 넣는 API 도 하나고,, 굳이???? 싶어서 그냥 String 으로 했어용
//    private LocalDateTime usageTime;
//    코드 보니까 Domain SAVE 시에 localdate 생성하길래 우선 뺐습니다!!>.!!
    private String inputFile;
    private String resultSummary;

    //input FILE 부분은 TYPE 에 따라 달라져서 그냥 빼고 Service 딴에서 넣었습니다!
    public UsageHistoryRequest(Long userId, String username,String modelType,String resultSummary){
        this.userId = userId;
        this.username = username;
        this.modelType = modelType;
        this.resultSummary = resultSummary;
    }
}