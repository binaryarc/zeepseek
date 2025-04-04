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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final DongService dongService;

    /**
     * 카카오 로컬 API 키 (spring.security.oauth2.client.registration.kakao.client-id)
     */
    private final String kakaoMapApiKey;

    /**
     * Kakao 로컬 API 호출용 WebClient (Authorization 헤더 자동 포함)
     */
    private final WebClient kakaoWebClient;

    public UserServiceImpl(
            UserRepository userRepository,
            UserPreferencesRepository userPreferencesRepository,
            DongService dongService,
            @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
            String kakaoMapApiKey
    ) {
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.dongService = dongService;
        this.kakaoMapApiKey = kakaoMapApiKey;

        // WebClient 초기화: baseUrl, Authorization 헤더 세팅
        this.kakaoWebClient = WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoMapApiKey)
                .build();
    }

    @Override
    @Transactional
    public UserDto updateUser(Integer userId, UserDto userDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        // 업데이트 가능한 필드만 변경
        if (userDto.getNickname() != null) {
            user.setNickname(userDto.getNickname());
        }
        if (userDto.getGender() != null) {
            user.setGender(userDto.getGender());
        }
        if (userDto.getAge() != null) {
            user.setAge(userDto.getAge());
        }
        if (userDto.getIsSeller() != null) {
            user.setIsSeller(userDto.getIsSeller());
        }

        User updatedUser = userRepository.save(user);

        return UserDto.builder()
                .idx(updatedUser.getIdx())
                .nickname(updatedUser.getNickname())
                .gender(updatedUser.getGender())
                .age(updatedUser.getAge())
                .isFirst(updatedUser.getIsFirst())
                .isSeller(updatedUser.getIsSeller())
                .provider(updatedUser.getProvider())
                .build();
    }

    @Override
    @Transactional
    public UserDto updateProfile(Integer userId, UserProfileDto profileDto) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        // 성별과 나이 업데이트 (있을 경우)
        if (profileDto.getGender() != null) {
            user.setGender(profileDto.getGender());
        }
        if (profileDto.getAge() != null) {
            user.setAge(profileDto.getAge());
        }

        User updatedUser = userRepository.save(user);

        // UserPreferences 객체 생성 혹은 조회
        UserPreferences userPreferences = userPreferencesRepository.findById(userId)
                .orElse(UserPreferences.builder().user(user).build());

        // 1. 목적지 정보 처리
        if (profileDto.getLocation() != null && !profileDto.getLocation().isEmpty()) {
            processDestination(userPreferences, profileDto.getLocation());
        }

        // 2. 선호도 정보 처리
        if (profileDto.getPreferences() != null && !profileDto.getPreferences().isEmpty()) {
            processPreferences(userPreferences, profileDto.getPreferences());
        }

        // 저장
        userPreferencesRepository.save(userPreferences);
        log.info("사용자 프로필 업데이트 완료: userId={}", userId);

        // 업데이트된 프로필 정보 구성
        UserProfileDto updatedProfileDto = UserProfileDto.builder()
                .gender(updatedUser.getGender())
                .age(updatedUser.getAge())
                .location(userPreferences.getDestination())
                .build();

        List<String> selectedPreferences = new ArrayList<>();
        if (userPreferences.getSafe() > 0) selectedPreferences.add("safe");
        if (userPreferences.getLeisure() > 0) selectedPreferences.add("leisure");
        if (userPreferences.getRestaurant() > 0) selectedPreferences.add("restaurant");
        if (userPreferences.getHealth() > 0) selectedPreferences.add("health");
        if (userPreferences.getConvenience() > 0) selectedPreferences.add("convenience");
        if (userPreferences.getTransport() > 0) selectedPreferences.add("transport");
        if (userPreferences.getCafe() > 0) selectedPreferences.add("cafe");
        updatedProfileDto.setPreferences(selectedPreferences);

        // 반환
        return UserDto.builder()
                .idx(updatedUser.getIdx())
                .nickname(updatedUser.getNickname())
                .gender(updatedUser.getGender())
                .age(updatedUser.getAge())
                .isFirst(updatedUser.getIsFirst())
                .isSeller(updatedUser.getIsSeller())
                .provider(updatedUser.getProvider())
                .profileInfo(updatedProfileDto)
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        userRepository.findById(userId)
                .ifPresent(userRepository::delete);
    }

    @Override
    @Transactional
    public UserDto processFirstLoginData(Integer userId, UserProfileDto profileDto) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        // 이미 첫 로그인이 아닐 때 예외
        if (user.getIsFirst() != 1) {
            throw new AuthException("User is not a first-time user");
        }

        // 사용자 기본 정보 업데이트
        if (profileDto.getGender() != null) {
            user.setGender(profileDto.getGender());
        }
        if (profileDto.getAge() != null) {
            user.setAge(profileDto.getAge());
        }

        // 첫 로그인 플래그 변경
        user.setIsFirst(0);
        User updatedUser = userRepository.save(user);

        // UserPreferences 객체 생성 또는 조회
        UserPreferences userPreferences = userPreferencesRepository.findById(userId)
                .orElse(UserPreferences.builder().user(user).build());

        // 1. 목적지 정보 처리
        processDestination(userPreferences, profileDto.getLocation());

        // 2. 선호도 정보 처리
        processPreferences(userPreferences, profileDto.getPreferences());

        userPreferencesRepository.save(userPreferences);
        log.info("사용자 선호도 저장 완료: userId={}", userId);

        // 반환
        return UserDto.builder()
                .idx(updatedUser.getIdx())
                .nickname(updatedUser.getNickname())
                .gender(updatedUser.getGender())
                .age(updatedUser.getAge())
                .isFirst(updatedUser.getIsFirst())
                .isSeller(updatedUser.getIsSeller())
                .provider(updatedUser.getProvider())
                .build();
    }

    /**
     * 목적지 정보 처리 (주소 → 위경도, 우편번호, 동ID)
     */
    private void processDestination(UserPreferences userPreferences, String location) {
        if (location == null || location.isEmpty()) {
            return;
        }

        // 전체 주소 저장
        userPreferences.setDestination(location);

        try {
            // 간단 파싱 (예시)
            String[] addressParts = location.split(" ");
            if (addressParts.length >= 1) {
                userPreferences.setSido(addressParts[0]);
            }
            if (addressParts.length >= 2) {
                userPreferences.setSigungu(addressParts[1]);
            }
            if (addressParts.length >= 3) {
                userPreferences.setRoadName(addressParts[2]);
            }
            if (addressParts.length >= 4) {
                userPreferences.setBuildingInfo(addressParts[3]);
            }
            if (addressParts.length >= 5) {
                StringBuilder detailAddress = new StringBuilder();
                for (int i = 4; i < addressParts.length; i++) {
                    detailAddress.append(addressParts[i]).append(" ");
                }
                userPreferences.setDetailAddress(detailAddress.toString().trim());
            }

            // 카카오 맵 API 호출
            fetchCoordinatesAndZipCodeFromKakao(userPreferences, location);

        } catch (Exception e) {
            log.warn("주소 파싱 중 오류 발생: {}", e.getMessage());
        }

        log.info("목적지 정보 설정: {}", location);
    }

    /**
     * 주소로부터 좌표 & 우편번호 얻기 (search/address.json)
     * - 이후 coord2regioncode로 행정동 가져오기
     */
    private void fetchCoordinatesAndZipCodeFromKakao(UserPreferences userPreferences, String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String requestUrl = "/v2/local/search/address.json?query=" + encodedAddress;

            log.info("카카오 맵 API 호출 시작 - 주소: {}", address);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = kakaoWebClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("카카오 맵 API 응답 수신 - 응답: {}", response);

            if (response != null) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                if (documents != null && !documents.isEmpty()) {
                    // 첫 번째 결과
                    Map<String, Object> firstResult = documents.get(0);

                    // 주소 정보
                    Map<String, Object> addressInfo = (Map<String, Object>) firstResult.get("address");
                    String zipCode = "";
                    if (addressInfo != null && addressInfo.get("zip_code") != null) {
                        zipCode = (String) addressInfo.get("zip_code");
                    } else {
                        // road_address에서 zone_no를 가져올 수도 있음
                        Map<String, Object> roadAddress = (Map<String, Object>) firstResult.get("road_address");
                        if (roadAddress != null && roadAddress.get("zone_no") != null) {
                            zipCode = (String) roadAddress.get("zone_no");
                        }
                    }
                    if (zipCode.isEmpty()) {
                        zipCode = "00000";
                    }
                    userPreferences.setZipCode(zipCode);

                    // 좌표 (x=경도, y=위도)
                    Double longitude = Double.parseDouble((String) firstResult.get("x"));
                    Double latitude = Double.parseDouble((String) firstResult.get("y"));
                    userPreferences.setLongitude(longitude);
                    userPreferences.setLatitude(latitude);

                    // 좌표 → 행정동
                    fetchAdministrativeDongFromCoordinates(userPreferences, longitude, latitude);

                    log.info("카카오 API 결과 - 위도: {}, 경도: {}, 우편번호: {}",
                            latitude, longitude, zipCode);
                    return;
                } else {
                    log.warn("카카오 API 응답에 주소 정보가 없음 - 응답: {}", response);
                }
            }

            // 위 로직에서 못 찾으면 기본값
            log.warn("카카오 API에서 주소 정보를 찾을 수 없어 기본값 사용");
            setDefaultCoordinates(userPreferences, address);

        } catch (Exception e) {
            log.error("카카오 맵 API 호출 중 오류 발생: {}", e.getMessage(), e);
            setDefaultCoordinates(userPreferences, address);
        }
    }

    /**
     * 위도, 경도로 행정동 검색 (coord2regioncode)
     * -> region_type=H (행정동) 우선, 없으면 region_type=B (법정동)
     * -> "region_3depth_name" (예: "역삼2동") 을 DB에 조회해 dong_id 매핑
     */
    private void fetchAdministrativeDongFromCoordinates(UserPreferences userPreferences,
                                                        Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            log.warn("좌표 정보가 없어 행정동 조회를 건너뜁니다.");
            return;
        }

        String requestUrl = "/v2/local/geo/coord2regioncode.json?x=" + longitude + "&y=" + latitude;
        log.info("카카오 좌표->지역코드 API 호출 - URI: {}", requestUrl);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = kakaoWebClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("카카오 좌표->지역코드 API 응답: {}", response);

            if (response == null || response.get("documents") == null) {
                log.warn("API 응답에 documents가 없습니다: {}", response);
                return;
            }

            List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
            if (documents.isEmpty()) {
                log.warn("API 응답에 documents가 비어있습니다");
                return;
            }

            // 1) 행정동(region_type="H") 먼저 시도
            for (Map<String, Object> doc : documents) {
                String regionType = (String) doc.get("region_type");
                String region3depthName = (String) doc.get("region_3depth_name");  // 예: "역삼2동"

                if ("H".equals(regionType) && region3depthName != null) {
                    // DB에서 dong.name = "역삼2동" 인 dong_id 찾기
                    Integer matchedId = dongService.findDongIdByName(region3depthName);
                    if (matchedId != null) {
                        userPreferences.setDongId(matchedId);
                        log.info("행정동 '{}' -> dong_id={} 매핑 성공", region3depthName, matchedId);
                        return; // 행정동 성공 시 바로 종료
                    } else {
                        log.info("행정동 '{}' 를 dong 테이블에서 찾을 수 없음", region3depthName);
                    }
                }
            }

            // 2) 법정동(region_type="B") 시도
            for (Map<String, Object> doc : documents) {
                String regionType = (String) doc.get("region_type");
                String region3depthName = (String) doc.get("region_3depth_name");  // 예: "역삼동"

                if ("B".equals(regionType) && region3depthName != null) {
                    Integer matchedId = dongService.findDongIdByName(region3depthName);
                    if (matchedId != null) {
                        userPreferences.setDongId(matchedId);
                        log.info("법정동 '{}' -> dong_id={} 매핑 성공", region3depthName, matchedId);
                        return;
                    } else {
                        log.info("법정동 '{}' 를 dong 테이블에서 찾을 수 없음", region3depthName);
                    }
                }
            }

            log.warn("행정동/법정동 모두 dong 테이블 매핑에 실패했습니다.");

        } catch (Exception e) {
            log.error("카카오 좌표->지역코드 API 처리 중 예외 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * API로 못찾았을 경우 기본값
     */
    private void setDefaultCoordinates(UserPreferences userPreferences, String address) {
        if (address.contains("부산") && address.contains("강서구") && address.contains("명지")) {
            userPreferences.setZipCode("46769");
            userPreferences.setLatitude(35.0947817266961);
            userPreferences.setLongitude(128.906874174632);
            // dong_id는 따로 처리
        } else if (address.contains("부산") && address.contains("강서구")) {
            userPreferences.setZipCode("46702");
            userPreferences.setLatitude(35.2121);
            userPreferences.setLongitude(128.9812);
        } else if (address.contains("부산")) {
            userPreferences.setZipCode("47545");
            userPreferences.setLatitude(35.1798);
            userPreferences.setLongitude(129.0750);
        } else if (address.contains("강남")) {
            userPreferences.setZipCode("06235");
            userPreferences.setLatitude(37.5012);
            userPreferences.setLongitude(127.0396);
        } else if (address.contains("서울")) {
            userPreferences.setZipCode("04524");
            userPreferences.setLatitude(37.5665);
            userPreferences.setLongitude(126.9780);
        } else {
            // 기타
            userPreferences.setZipCode("00000");
            userPreferences.setLatitude(36.5);
            userPreferences.setLongitude(127.8);
        }

        log.info("기본 정보 설정 - 위도: {}, 경도: {}, 우편번호: {}",
                userPreferences.getLatitude(), userPreferences.getLongitude(),
                userPreferences.getZipCode());
    }

    /**
     * 선호도 정보 처리
     */
    private void processPreferences(UserPreferences userPreferences, List<String> selectedPreferences) {
        // 초기화
        userPreferences.setSafe(0.0f);
        userPreferences.setLeisure(0.0f);
        userPreferences.setRestaurant(0.0f);
        userPreferences.setHealth(0.0f);
        userPreferences.setConvenience(0.0f);
        userPreferences.setTransport(0.0f);
        userPreferences.setCafe(0.0f);

        if (selectedPreferences == null || selectedPreferences.isEmpty()) {
            log.info("선택된 선호도가 없습니다.");
            return;
        }

        // 최대 3개 제한
        List<String> limitedPreferences = selectedPreferences;
        if (selectedPreferences.size() > 3) {
            log.warn("사용자가 3개 이상의 선호도를 선택했습니다. 상위 3개만 처리합니다.");
            limitedPreferences = selectedPreferences.subList(0, 3);
        }

        log.info("선택된 선호도: {}", limitedPreferences);

        for (String preference : limitedPreferences) {
            switch (preference.toLowerCase()) {
                case "safe":
                case "안전":
                    userPreferences.setSafe(1.0f);
                    break;
                case "leisure":
                case "여가":
                    userPreferences.setLeisure(1.0f);
                    break;
                case "restaurant":
                case "식당":
                    userPreferences.setRestaurant(1.0f);
                    break;
                case "health":
                case "건강":
                case "보건":
                    userPreferences.setHealth(1.0f);
                    break;
                case "convenience":
                case "편의":
                    userPreferences.setConvenience(1.0f);
                    break;
                case "transport":
                case "교통":
                case "대중교통":
                    userPreferences.setTransport(1.0f);
                    break;
                case "cafe":
                case "카페":
                    userPreferences.setCafe(1.0f);
                    break;
                default:
                    log.warn("알 수 없는 선호도 유형: {}", preference);
            }
        }
    }

    @Override
    public UserDto getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        UserPreferences preferences = userPreferencesRepository.findById(userId).orElse(null);

        UserProfileDto profileDto = UserProfileDto.builder()
                .gender(user.getGender())
                .age(user.getAge())
                .build();

        if (preferences != null) {
            profileDto.setLocation(preferences.getDestination());

            List<String> selectedPreferences = new ArrayList<>();
            if (preferences.getSafe() > 0) selectedPreferences.add("safe");
            if (preferences.getLeisure() > 0) selectedPreferences.add("leisure");
            if (preferences.getRestaurant() > 0) selectedPreferences.add("restaurant");
            if (preferences.getHealth() > 0) selectedPreferences.add("health");
            if (preferences.getConvenience() > 0) selectedPreferences.add("convenience");
            if (preferences.getTransport() > 0) selectedPreferences.add("transport");
            if (preferences.getCafe() > 0) selectedPreferences.add("cafe");
            profileDto.setPreferences(selectedPreferences);
        }

        return UserDto.builder()
                .idx(user.getIdx())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .isFirst(user.getIsFirst())
                .isSeller(user.getIsSeller())
                .provider(user.getProvider())
                .profileInfo(profileDto)
                .build();
    }

    @Override
    public boolean isProfileComplete(Integer userId) {
        return userRepository.findById(userId)
                .map(user -> user.getIsFirst() == 0)
                .orElse(false);
    }
}
