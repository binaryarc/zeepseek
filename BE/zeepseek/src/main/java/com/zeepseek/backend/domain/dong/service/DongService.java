package com.zeepseek.backend.domain.dong.service;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.dto.request.DongCommentRequestDto;
import com.zeepseek.backend.domain.dong.repository.MongoDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DongService {

    private final MongoDongRepository dongRepository;

    /**
     * 동 이름을 포함한 검색 결과 반환
     */
    public List<DongInfoDocs> searchByName(String name) {
        return dongRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * dongId를 기반으로 해당 동의 상세 정보를 반환
     */
    public DongInfoDocs getDongDetail(Integer dongId) {
        return dongRepository.findByDongId(dongId);
    }

    /**
     * dongId를 기반으로 해당 동의 댓글 목록을 반환
     */
    public List<DongInfoDocs.DongComment> getDongComments(Integer dongId) {
        DongInfoDocs doc = dongRepository.findById(dongId).orElse(null);
        if (doc != null && doc.getComments() != null) {
            return doc.getComments();
        }
        return Collections.emptyList();
    }

    /**
     * dongId를 기반으로 댓글 삽입
     * 댓글 삽입 시 commentCount를 증가시키고, 그 값을 댓글 id로 할당합니다.
     */
    public DongInfoDocs addDongComment(Integer dongId, DongCommentRequestDto commentRequest) {
        DongInfoDocs doc = dongRepository.findByDongId(dongId);
        if (doc == null) {
            throw new RuntimeException("Dong not found with id " + dongId);
        }
        DongInfoDocs.DongComment comment = new DongInfoDocs.DongComment();
        comment.setNickname(commentRequest.getNickname());
        comment.setContent(commentRequest.getContent());
        comment.setCreatedAt(LocalDateTime.now());

        if (doc.getComments() == null) {
            doc.setComments(new ArrayList<>());
        }
        // 댓글 삽입 시 commentCount를 증가시키고 그 값을 댓글의 id로 설정
        int newId = doc.getCommentCount() + 1;
        comment.setCommentId(newId);
        doc.getComments().add(comment);
        // 전체 댓글 수 업데이트 (리스트 크기로 설정)
        doc.setCommentCount(doc.getComments().size());
        return dongRepository.save(doc);
    }

    /**
     * dongId와 commentId를 기반으로 댓글 삭제
     * 삭제 후 남은 댓글 갯수를 commentCount에 반영합니다.
     */
    public DongInfoDocs deleteDongCommentById(Integer dongId, int commentId) {
        DongInfoDocs doc = dongRepository.findByDongId(dongId);
        if (doc == null) {
            throw new RuntimeException("Dong not found with id " + dongId);
        }
        if (doc.getComments() != null) {
            List<DongInfoDocs.DongComment> updatedComments = doc.getComments().stream()
                    .filter(comment -> comment.getCommentId() != commentId)
                    .collect(Collectors.toList());
            doc.setComments(updatedComments);
            doc.setCommentCount(updatedComments.size());
            return dongRepository.save(doc);
        }
        return doc;
    }
}
