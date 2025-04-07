package com.zeepseek.backend.domain.user.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class NicknameServiceImpl implements NicknameService {
    // A 그룹 단어 목록 (형용사)
    private static final List<String> GROUP_A = Arrays.asList(
            "엄청난", "미친", "당당한", "귀여운", "행복한", "재치있는", "예쁜",
            "멋진", "신비한", "화려한", "똑똑한", "재밌는", "끔찍한", "잘생긴"
    );

    // B 그룹 단어 목록 (명사)
    private static final List<String> GROUP_B = Arrays.asList(
            "땅콩", "옥수수", "쿼카", "고양이", "강아지", "흰긴수염고래",
            "펭귄", "여우", "다람쥐", "코끼리", "기린","파키케팔로사우르스"
    );

    private final Random random = new Random();

    @Override
    public String generateRandomNickname() {
        // A 그룹에서 랜덤 단어 선택
        String wordA = GROUP_A.get(random.nextInt(GROUP_A.size()));

        // B 그룹에서 랜덤 단어 선택
        String wordB = GROUP_B.get(random.nextInt(GROUP_B.size()));

        // 두 단어 조합
        String nickname = wordA + " " + wordB;
        log.info("Generated random nickname: {}", nickname);

        return nickname;
    }
}
