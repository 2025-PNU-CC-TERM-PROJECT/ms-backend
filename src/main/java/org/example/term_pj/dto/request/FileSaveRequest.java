package org.example.term_pj.dto.request;


import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FileSaveRequest {

    private UUID uuid; // 파일명 겹치는거 예방 (폴더 분기 안해놔서 uuid는 꼭 필요할거같아서 넣었습니다!)

    private String filepath;  //filepath 는 전체경로! 현재는 모든 경로로 저장하게끔 했어요. rootpath 까지

    private String filename;  //원본 파일 이름 (사용자가 정해둔 이름 그대로 저장)

    private String extension; // 확장자 (파일 나중에 불러올 때 필요해가지고 넣었습니다)

    private Long usagehistoryId;

    public FileSaveRequest(UUID uuid,String filepath,String filename,String extension){
        this.extension = extension;
        this.filename = filename;
        this.filepath = filepath;
        this.uuid = uuid;
    }
}
