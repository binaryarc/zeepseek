package com.zeepseek.backend.domain.dong.dto.request;

import lombok.Data;

@Data
public class DongCommentRequestDto {
    private String nickname;  // 댓글 작성자 닉네임
    private String content;   // 댓글 내용
}
