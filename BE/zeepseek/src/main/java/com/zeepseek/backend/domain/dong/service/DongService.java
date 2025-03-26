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
     * 예: "신당"을 검색하면 "신당동", "신당리" 등 관련 문서 반환
     */
    public List<DongInfoDocs> searchByName(String name) {
        // findByNameContainingIgnoreCase 메서드는 MongoDongRepository에 미리 정의되어 있다고 가정합니다.
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
        doc.getComments().add(comment);
        return dongRepository.save(doc);
    }

    /**
     * dongId와 닉네임을 기반으로 댓글 삭제 (해당 닉네임의 모든 댓글 삭제)
     */
    public DongInfoDocs deleteDongComment(Integer dongId, String nickname) {
        DongInfoDocs doc = dongRepository.findByDongId(dongId);
        if (doc == null) {
            throw new RuntimeException("Dong not found with id " + dongId);
        }
        if (doc.getComments() != null) {
            List<DongInfoDocs.DongComment> updatedComments = doc.getComments().stream()
                    .filter(comment -> !comment.getNickname().equals(nickname))
                    .collect(Collectors.toList());
            doc.setComments(updatedComments);
            return dongRepository.save(doc);
        }
        return doc;
    }


}