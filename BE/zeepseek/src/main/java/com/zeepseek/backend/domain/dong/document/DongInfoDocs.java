package com.zeepseek.backend.domain.dong.document;

import org.springframework.data.annotation.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Document(collection = "dong_info")
public class DongInfoDocs {

    @Id
    private int dongId;

    private String name;
    private String guName;

    private Float safe;
    private Float leisure;
    private Float restaurant;
    private Float health;
    private Float mart;
    private Float convenience;
    private Float transport;

    private LocalDateTime updatedAt;

    // GPT API를 통한 요약 정보
    private String summary;

    // 댓글 갯수를 관리 (댓글 추가 시 1씩 증가, 삭제 시 1씩 감소)
    private int commentCount = 0;

    // 동네 댓글 목록
    private List<DongComment> comments = new ArrayList<>();

    @Getter
    @Setter
    public static class DongComment {
        // 댓글의 id (댓글 추가 시 현재 commentCount+1 값을 할당)
        private int commentId;
        private String nickname;       // 댓글 작성자의 닉네임
        private String content;        // 댓글 내용
        private LocalDateTime createdAt; // 댓글 작성 시간
    }
}
