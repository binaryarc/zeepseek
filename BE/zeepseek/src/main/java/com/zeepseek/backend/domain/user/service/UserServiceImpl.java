package com.zeepseek.backend.domain.user.service;

import com.zeepseek.backend.domain.auth.exception.AuthException;
import com.zeepseek.backend.domain.dong.service.DongService;
import com.zeepseek.backend.domain.user.dto.UserDto;
import com.zeepseek.backend.domain.user.dto.UserProfileDto;
import com.zeepseek.backend.domain.user.entity.User;
import com.zeepseek.backend.domain.user.entity.UserPreferences;
import com.zeepseek.backend.domain.user.repository.UserPreferencesRepository;
import com.zeepseek.backend.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    /* ------------------------------------------------------------------ */
    /* 의존성 및 WebClient                                                  */
    /* ------------------------------------------------------------------ */

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final DongService dongService;
    private final NicknameService nicknameService;
    private final String kakaoMapApiKey;
    private final WebClient kakaoWebClient;

    public UserServiceImpl(
            UserRepository userRepository,
            UserPreferencesRepository userPreferencesRepository,
            DongService dongService,
            NicknameService nicknameService,
            @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
            String kakaoMapApiKey
    ) {
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.dongService = dongService;
        this.kakaoMapApiKey = kakaoMapApiKey;
        this.nicknameService = nicknameService;
        this.kakaoWebClient = WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoMapApiKey)
                .build();
    }

    /* ------------------------------------------------------------------ */
    /* 1) 사용자 정보 업데이트                                              */
    /* ------------------------------------------------------------------ */

    @Override @Transactional
    public UserDto updateUser(Integer userId, UserDto dto) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
        if (dto.getNickname() != null) u.setNickname(dto.getNickname());
        if (dto.getGender()   != null) u.setGender(dto.getGender());
        if (dto.getAge()      != null) u.setAge(dto.getAge());
        if (dto.getIsSeller() != null) u.setIsSeller(dto.getIsSeller());

        User saved = userRepository.save(u);
        return UserDto.builder()
                .idx(saved.getIdx()).nickname(saved.getNickname())
                .gender(saved.getGender()).age(saved.getAge())
                .isFirst(saved.getIsFirst()).isSeller(saved.getIsSeller())
                .provider(saved.getProvider())
                .build();
    }

    /* ------------------------------------------------------------------ */
    /* 2) 사용자 프로필 업데이트                                            */
    /* ------------------------------------------------------------------ */

    @Override @Transactional
    public UserDto updateProfile(Integer userId, UserProfileDto dto) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
        if (dto.getGender() != null) u.setGender(dto.getGender());
        if (dto.getAge()    != null) u.setAge(dto.getAge());

        // 닉네임 업데이트 로직 추가 - survey 엔드포인트와 동일하게 처리
        if (dto.getNickname() != null && !dto.getNickname().trim().isEmpty()) {
            u.setNickname(dto.getNickname());
            log.info("Nickname updated for user {}: {}", userId, dto.getNickname());
        }

        User savedUser = userRepository.save(u);

        UserPreferences pref = userPreferencesRepository.findById(userId)
                .orElse(UserPreferences.builder().user(u).build());

        if (dto.getLocation()    != null && !dto.getLocation().isEmpty())
            processDestination(pref, dto.getLocation());
        if (dto.getPreferences() != null && !dto.getPreferences().isEmpty())
            processPreferences(pref, dto.getPreferences());

        userPreferencesRepository.save(pref);

        // 반환 DTO 구성
        UserProfileDto profile = UserProfileDto.builder()
                .gender(savedUser.getGender()).age(savedUser.getAge())
                .location(pref.getDestination()).nickname(savedUser.getNickname()).build(); // 닉네임 추가

        List<String> sel = new ArrayList<>();
        if (pref.getSafe()        > 0) sel.add("safe");
        if (pref.getLeisure()     > 0) sel.add("leisure");
        if (pref.getRestaurant()  > 0) sel.add("restaurant");
        if (pref.getHealth()      > 0) sel.add("health");
        if (pref.getConvenience() > 0) sel.add("convenience");
        if (pref.getTransport()   > 0) sel.add("transport");
        if (pref.getCafe()        > 0) sel.add("cafe");
        profile.setPreferences(sel);

        return UserDto.builder()
                .idx(savedUser.getIdx()).nickname(savedUser.getNickname())
                .gender(savedUser.getGender()).age(savedUser.getAge())
                .isFirst(savedUser.getIsFirst()).isSeller(savedUser.getIsSeller())
                .provider(savedUser.getProvider()).profileInfo(profile)
                .build();
    }

    /* ------------------------------------------------------------------ */
    /* 3) 사용자 삭제                                                      */
    /* ------------------------------------------------------------------ */

    @Override @Transactional
    public void deleteUser(Integer userId) {
        userRepository.findById(userId).ifPresent(userRepository::delete);
    }

    /* ------------------------------------------------------------------ */
    /* 4) 첫 로그인 시 프로필 처리                                         */
    /* ------------------------------------------------------------------ */

    @Override @Transactional
    public UserDto processFirstLoginData(Integer userId, UserProfileDto dto) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
        if (u.getIsFirst() != 1) throw new AuthException("Not first login");

        if (dto.getGender() != null) u.setGender(dto.getGender());
        if (dto.getAge()    != null) u.setAge(dto.getAge());

        // 클라이언트에서 보낸 닉네임이 있으면 업데이트 (랜덤 생성된 닉네임 포함)
        if (dto.getNickname() != null && !dto.getNickname().trim().isEmpty()) {
            u.setNickname(dto.getNickname());
            log.info("Nickname updated for user {}: {}", userId, dto.getNickname());
        }

        u.setIsFirst(0);
        User saved = userRepository.save(u);

        UserPreferences pref = userPreferencesRepository.findById(userId)
                .orElse(UserPreferences.builder().user(u).build());

        processDestination(pref, dto.getLocation());
        processPreferences (pref, dto.getPreferences());
        userPreferencesRepository.save(pref);

        // 프로필 정보(UserProfileDto) 생성
        UserProfileDto profileDto = UserProfileDto.builder()
                .gender(saved.getGender())
                .age(saved.getAge())
                .location(pref.getDestination())
                .nickname(saved.getNickname())
                .build();

        // 선호도 정보 설정
        List<String> preferences = new ArrayList<>();
        if (pref.getSafe()        > 0) preferences.add("safe");
        if (pref.getLeisure()     > 0) preferences.add("leisure");
        if (pref.getRestaurant()  > 0) preferences.add("restaurant");
        if (pref.getHealth()      > 0) preferences.add("health");
        if (pref.getConvenience() > 0) preferences.add("convenience");
        if (pref.getTransport()   > 0) preferences.add("transport");
        if (pref.getCafe()        > 0) preferences.add("cafe");
        profileDto.setPreferences(preferences);

        return UserDto.builder()
                .idx(saved.getIdx()).nickname(saved.getNickname())
                .gender(saved.getGender()).age(saved.getAge())
                .isFirst(saved.getIsFirst()).isSeller(saved.getIsSeller())
                .provider(saved.getProvider()).profileInfo(profileDto)
                .build();
    }

    /* ------------------------------------------------------------------ */
    /* 목적지 정보 처리                                                    */
    /* ------------------------------------------------------------------ */

    private void processDestination(UserPreferences pref, String addr) {
        if (addr == null || addr.isBlank()) return;
        pref.setDestination(addr);

        try {
            String[] p = addr.split(" ");
            if (p.length >= 1) pref.setSido(p[0]);
            if (p.length >= 2) pref.setSigungu(p[1]);
            if (p.length >= 3) pref.setRoadName(p[2]);
            if (p.length >= 4) pref.setBuildingInfo(p[3]);
            if (p.length >= 5) {
                StringBuilder sb = new StringBuilder();
                for (int i = 4; i < p.length; i++) sb.append(p[i]).append(" ");
                pref.setDetailAddress(sb.toString().trim());
            }
        } catch (Exception e) {
            log.warn("주소 파싱 오류: {}", e.getMessage());
        }

        fetchCoordinatesAndZipCodeFromKakao(pref, addr);
    }

    /* ------------------------------------------------------------------ */
    /* Kakao 주소 검색 → 위경도·우편번호·dong_id                           */
    /* ------------------------------------------------------------------ */

    @SuppressWarnings("unchecked")
    private void fetchCoordinatesAndZipCodeFromKakao(UserPreferences pref, String addr) {

        Map<String, Object> resp = kakaoWebClient.get()
                .uri(b -> b.path("/v2/local/search/address.json")
                        .queryParam("query", addr)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> docs = resp != null
                ? (List<Map<String, Object>>) resp.get("documents")
                : Collections.emptyList();

        if (docs == null || docs.isEmpty()) {
            setDefaultCoordinates(pref, addr);
            return;
        }

        Map<String, Object> first = docs.get(0);
        Map<String, Object> addressInfo = (Map<String, Object>) first.get("address");
        Map<String, Object> roadInfo    = (Map<String, Object>) first.get("road_address");

        pref.setLongitude(Double.parseDouble((String) first.get("x")));
        pref.setLatitude (Double.parseDouble((String) first.get("y")));

        String zip = addressInfo != null && addressInfo.get("zip_code") != null
                ? (String) addressInfo.get("zip_code")
                : roadInfo != null && roadInfo.get("zone_no") != null
                ? (String) roadInfo.get("zone_no")
                : "00000";
        pref.setZipCode(zip);

        /* dong_id 매핑: 행정동 이름 → DB */
        String dongName = null;
        if (addressInfo != null && addressInfo.get("region_3depth_h_name") != null)
            dongName = (String) addressInfo.get("region_3depth_h_name");
        else if (addressInfo != null)
            dongName = (String) addressInfo.get("region_3depth_name");

        boolean mapped = false;
        if (dongName != null) {
            try {
                Integer id = dongService.findDongIdByName(dongName);
                if (id != null) {
                    pref.setDongId(id);
                    mapped = true;
                }
            } catch (Exception e) {
                log.warn("dong_id 조회 실패: {}", e.getMessage());
            }
        }

        /* 🔄 h_code fallback (행정동 코드) */
        if (!mapped && addressInfo != null && addressInfo.get("h_code") != null) {
            try {
                String h = (String) addressInfo.get("h_code"); // 예: 1168065000
                pref.setDongId(Integer.parseInt(h.substring(0, 8))); // 앞 8자리 사용
            } catch (Exception e) {
                log.warn("h_code 파싱 실패: {}", e.getMessage());
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* Kakao 실패 시 기본값(서울 명동)                                     */
    /* ------------------------------------------------------------------ */

    private void setDefaultCoordinates(UserPreferences p, String a) {
        p.setZipCode("04536");
        p.setLatitude(37.5640);
        p.setLongitude(126.9830);
        p.setDongId(11140550);
    }

    /* ------------------------------------------------------------------ */
    /* 5) 선호도 처리                                                     */
    /* ------------------------------------------------------------------ */

    private void processPreferences(UserPreferences p, List<String> sel) {
        p.setSafe(0f); p.setLeisure(0f); p.setRestaurant(0f);
        p.setHealth(0f); p.setConvenience(0f);
        p.setTransport(0f); p.setCafe(0f);

        if (sel == null || sel.isEmpty()) return;
        if (sel.size() > 3) sel = sel.subList(0, 3);

        for (String s : sel) switch (s.toLowerCase()) {
            case "safe": case "안전":           p.setSafe(1f); break;
            case "leisure": case "여가":        p.setLeisure(1f); break;
            case "restaurant": case "식당":     p.setRestaurant(1f); break;
            case "health": case "건강": case "보건":
                p.setHealth(1f); break;
            case "convenience": case "편의":    p.setConvenience(1f); break;
            case "transport": case "교통": case "대중교통":
                p.setTransport(1f); break;
            case "cafe": case "카페":           p.setCafe(1f); break;
            default: log.warn("알 수 없는 선호도: {}", s);
        }
    }

    /* ------------------------------------------------------------------ */
    /* 6) 사용자 정보 조회                                                 */
    /* ------------------------------------------------------------------ */

    @Override
    public UserDto getUserById(Integer id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new AuthException("User not found"));

        UserPreferences p = userPreferencesRepository.findById(id).orElse(null);

        UserProfileDto prof = UserProfileDto.builder()
                .gender(u.getGender()).age(u.getAge()).build();

        if (p != null) {
            prof.setLocation(p.getDestination());
            List<String> l = new ArrayList<>();
            if (p.getSafe()        > 0) l.add("safe");
            if (p.getLeisure()     > 0) l.add("leisure");
            if (p.getRestaurant()  > 0) l.add("restaurant");
            if (p.getHealth()      > 0) l.add("health");
            if (p.getConvenience() > 0) l.add("convenience");
            if (p.getTransport()   > 0) l.add("transport");
            if (p.getCafe()        > 0) l.add("cafe");
            prof.setPreferences(l);
        }

        return UserDto.builder()
                .idx(u.getIdx()).nickname(u.getNickname())
                .gender(u.getGender()).age(u.getAge())
                .isFirst(u.getIsFirst()).isSeller(u.getIsSeller())
                .provider(u.getProvider()).profileInfo(prof)
                .build();
    }

    /* ------------------------------------------------------------------ */
    /* 7) 프로필 완성 여부                                                 */
    /* ------------------------------------------------------------------ */

    @Override
    public boolean isProfileComplete(Integer id) {
        return userRepository.findById(id)
                .map(u -> u.getIsFirst() == 0).orElse(false);
    }

    public UserPreferences findByUserId(Integer userId) {
        return userPreferencesRepository.findById(userId).orElse(null);
    }
}