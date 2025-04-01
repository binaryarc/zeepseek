package com.zeepseek.backend.domain.dong.controller;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.dto.request.DongCommentRequestDto;
import com.zeepseek.backend.domain.dong.service.DongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/dong")
@RequiredArgsConstructor
public class DongController {

    private final DongService dongService;

    /**
     * 동 이름으로 검색하는 API
     * 요청 예: GET /api/dongs/search?name=신당
     */
    @GetMapping("/search")
    public ResponseEntity<List<DongInfoDocs>> searchDongs(@RequestParam("name") String name) {
        List<DongInfoDocs> dongs = dongService.searchByName(name);
        return ResponseEntity.ok(dongs);
    }

    /**
     * dongId로 특정 동의 상세 정보를 조회하는 API
     * 요청 예: GET /api/dongs/123
     */
    @GetMapping("/{dongId}")
    public ResponseEntity<DongInfoDocs> getDongDetail(@PathVariable("dongId") Integer dongId) {
        DongInfoDocs dong = dongService.getDongDetail(dongId);
        log.info(dongId.toString());
        return dong != null ? ResponseEntity.ok(dong) : ResponseEntity.notFound().build();
    }

    /**
     * dongId를 기반으로 댓글 목록 조회 API
     * 요청 예: GET /api/v1/dong/123/comment
     */
    @GetMapping("/{dongId}/comment")
    public ResponseEntity<List<DongInfoDocs.DongComment>> getDongComments(@PathVariable("dongId") Integer dongId) {
        List<DongInfoDocs.DongComment> comments = dongService.getDongComments(dongId);
        return ResponseEntity.ok(comments);
    }

    /**
     * dongId를 기반으로 댓글 삽입 API
     * 요청 예: POST /api/v1/dong/123/comment
     * Body: { "nickname": "작성자닉네임", "content": "댓글 내용" }
     */
    @PostMapping("/{dongId}/comment")
    public ResponseEntity<DongInfoDocs> addDongComment(
            @PathVariable("dongId") Integer dongId,
            @RequestBody DongCommentRequestDto commentRequest) {
        DongInfoDocs updatedDong = dongService.addDongComment(dongId, commentRequest);
        return ResponseEntity.ok(updatedDong);
    }

    /**
     * dongId와 commentId를 기반으로 댓글 삭제 API
     * 요청 예: DELETE /api/v1/dong/123/comment?commentId=1
     */
    @DeleteMapping("/{dongId}/comment")
    public ResponseEntity<DongInfoDocs> deleteDongComment(
            @PathVariable("dongId") Integer dongId,
            @RequestParam("commentId") int commentId) {
        DongInfoDocs updatedDong = dongService.deleteDongCommentById(dongId, commentId);
        return ResponseEntity.ok(updatedDong);
    }

    @GetMapping("/all")
    public ResponseEntity<?> findAlldongs() {
        return ResponseEntity.ok(dongService.findAllDongsforZzim());
    }
}
