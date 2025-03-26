package com.zeepseek.backend.domain.dong.document;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Document(collection = "dong_info")
public class DongInfoDocs {

    @Id
    private Integer dongId;

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

    // 동네 댓글 목록
    private List<DongComment> comments;

    @Getter
    @Setter
    public static class DongComment {
        private String nickname;       // 댓글 작성자의 닉네임
        private String content;        // 댓글 내용
        private LocalDateTime createdAt; // 댓글 작성 시간
    }
}
