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

    // Repository & Service 의존성
    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final DongService dongService;

    /**
     * 카카오 맵 API 키: application.yml/properties의
     * spring.security.oauth2.client.registration.kakao.client-id 에서 주입
     */
    private final String kakaoMapApiKey;

    /**
     * Kakao 로컬 API 전용 WebClient:
     * 생성자에서 baseUrl, Authorization 헤더를 세팅해 둠
     */
    private final WebClient kakaoWebClient;

    /**
     * 생성자: @RequiredArgsConstructor 대신 직접 작성하여
     * - UserRepository / UserPreferencesRepository / DongService
     * - 카카오 REST API 키 (kakaoMapApiKey)
     * 를 모두 주입받고,
     * kakaoWebClient를 빌드한다.
     */
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

        // WebClient를 미리 만들어둠: 이 클라이언트를 통해 카카오 API 호출 시 401 해결
        this.kakaoWebClient = WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoMapApiKey)
                .build();
    }

    // -------------------------------
    // 1) 사용자 정보 업데이트 (updateUser)
    // -------------------------------
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

    // -------------------------------
    // 2) 사용자 프로필 업데이트 (updateProfile)
    // -------------------------------
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

        // UserPreferences 객체 생성 또는 조회
        UserPreferences userPreferences = userPreferencesRepository.findById(userId)
                .orElse(UserPreferences.builder().user(user).build());

        // 1. 목적지 정보 처리 (location 값이 있을 경우에만)
        if (profileDto.getLocation() != null && !profileDto.getLocation().isEmpty()) {
            processDestination(userPreferences, profileDto.getLocation());
        }

        // 2. 선호도 정보 처리 (preferences 값이 있을 경우에만)
        if (profileDto.getPreferences() != null && !profileDto.getPreferences().isEmpty()) {
            processPreferences(userPreferences, profileDto.getPreferences());
        }

        // 사용자 선호도 저장
        userPreferencesRepository.save(userPreferences);
        log.info("사용자 프로필 업데이트 완료: userId={}", userId);

        // 업데이트된 프로필 정보 구성
        UserProfileDto updatedProfileDto = UserProfileDto.builder()
                .gender(updatedUser.getGender())
                .age(updatedUser.getAge())
                .location(userPreferences.getDestination())
                .build();

        // 선호도 정보 설정
        List<String> selectedPreferences = new ArrayList<>();
        if (userPreferences.getSafe() > 0) selectedPreferences.add("safe");
        if (userPreferences.getLeisure() > 0) selectedPreferences.add("leisure");
        if (userPreferences.getRestaurant() > 0) selectedPreferences.add("restaurant");
        if (userPreferences.getHealth() > 0) selectedPreferences.add("health");
        if (userPreferences.getConvenience() > 0) selectedPreferences.add("convenience");
        if (userPreferences.getTransport() > 0) selectedPreferences.add("transport");
        if (userPreferences.getCafe() > 0) selectedPreferences.add("cafe");
        updatedProfileDto.setPreferences(selectedPreferences);

        // 업데이트된 사용자 정보 반환 (profileInfo 포함)
        return UserDto.builder()
                .idx(updatedUser.getIdx())
                .nickname(updatedUser.getNickname())
                .gender(updatedUser.getGender())
                .age(updatedUser.getAge())
                .isFirst(updatedUser.getIsFirst())
                .isSeller(updatedUser.getIsSeller())
                .provider(updatedUser.getProvider())
                .profileInfo(updatedProfileDto)  // 프로필 정보 포함
                .build();
    }

    // -------------------------------
    // 3) 사용자 삭제
    // -------------------------------
    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        userRepository.findById(userId)
                .ifPresent(userRepository::delete);
    }

    // -------------------------------
    // 4) 첫 로그인 시 프로필(나이, 성별, 위치, 선호도) 처리
    // -------------------------------
    @Override
    @Transactional
    public UserDto processFirstLoginData(Integer userId, UserProfileDto profileDto) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        // 이미 첫 로그인이 아니면 예외
        if (user.getIsFirst() != 1) {
            throw new AuthException("User is not a first-time user");
        }

        // 사용자 기본 정보 업데이트 (성별, 나이)
        if (profileDto.getGender() != null) {
            user.setGender(profileDto.getGender());
        }
        if (profileDto.getAge() != null) {
            user.setAge(profileDto.getAge());
        }

        // 첫 로그인 플래그를 0으로 변경 (이미 프로필을 입력했으므로)
        user.setIsFirst(0);
        User updatedUser = userRepository.save(user);

        // UserPreferences 객체 생성 또는 조회
        UserPreferences userPreferences = userPreferencesRepository.findById(userId)
                .orElse(UserPreferences.builder().user(user).build());

        // 1. 목적지 정보 (location)
        processDestination(userPreferences, profileDto.getLocation());

        // 2. 선호도 정보
        processPreferences(userPreferences, profileDto.getPreferences());

        // 사용자 선호도 저장
        userPreferencesRepository.save(userPreferences);
        log.info("사용자 선호도 저장 완료: userId={}", userId);

        // 업데이트된 사용자 정보 반환
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

    // -------------------------------
    // 목적지 정보 처리 (주소 파싱 + 카카오 맵 API 호출)
    // -------------------------------
    private void processDestination(UserPreferences userPreferences, String location) {
        if (location == null || location.isEmpty()) {
            return;
        }

        // 전체 주소 설정
        userPreferences.setDestination(location);

        try {
            // 간단한 파싱 예시
            String[] addressParts = location.split(" ");

            if (addressParts.length >= 1) {
                userPreferences.setSido(addressParts[0]); // 시/도
            }
            if (addressParts.length >= 2) {
                userPreferences.setSigungu(addressParts[1]); // 시/군/구
            }
            if (addressParts.length >= 3) {
                userPreferences.setRoadName(addressParts[2]); // 도로명
            }
            if (addressParts.length >= 4) {
                userPreferences.setBuildingInfo(addressParts[3]); // 건물번호
            }
            if (addressParts.length >= 5) {
                // 나머지 부분을 상세 주소로
                StringBuilder detailAddress = new StringBuilder();
                for (int i = 4; i < addressParts.length; i++) {
                    detailAddress.append(addressParts[i]).append(" ");
                }
                userPreferences.setDetailAddress(detailAddress.toString().trim());
            }

            // 카카오 맵 API: 주소 -> 좌표, 우편번호, 동코드
            fetchCoordinatesAndZipCodeFromKakao(userPreferences, location);

        } catch (Exception e) {
            log.warn("주소 파싱 중 오류 발생: {}", e.getMessage());
        }

        log.info("목적지 정보 설정: {}", location);
    }

    // -------------------------------
    // 카카오 맵 API로 주소 -> (위경도, 우편번호, 법정동코드 등) 가져오기
    // -------------------------------
    private void fetchCoordinatesAndZipCodeFromKakao(UserPreferences userPreferences, String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            // /v2/local/search/address.json?query=...
            String requestUrl = "/v2/local/search/address.json?query=" + encodedAddress;

            log.info("카카오 맵 API 호출 시작 - 주소: {}", address);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = kakaoWebClient.get()
                    .uri(requestUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // 동기 처리 예시

            log.info("카카오 맵 API 응답 수신 - 응답: {}", response);

            if (response != null) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                if (documents != null && !documents.isEmpty()) {
                    Map<String, Object> firstResult = documents.get(0);

                    // 주소 정보 (우편번호, b_code 등)
                    Map<String, Object> addressInfo = (Map<String, Object>) firstResult.get("address");
                    String zipCode = "";
                    Integer dongId = null;

                    if (addressInfo != null) {
                        // 우편번호
                        if (addressInfo.get("zip_code") != null) {
                            zipCode = (String) addressInfo.get("zip_code");
                        }
                        // 법정동 코드 (b_code)
                        if (addressInfo.get("b_code") != null) {
                            String bCode = (String) addressInfo.get("b_code");
                            if (bCode != null && bCode.length() >= 8) {
                                String dongIdStr = bCode.substring(0, 8);
                                try {
                                    dongId = Integer.parseInt(dongIdStr);
                                    log.info("법정동 코드 추출 - 전체 코드: {}, 동ID: {}", bCode, dongId);
                                } catch (NumberFormatException e) {
                                    log.warn("법정동 코드 파싱 오류: {}", e.getMessage());
                                }
                            }
                        }
                    } else {
                        // road_address 영역에서 zone_no도 가능
                        Map<String, Object> roadAddressInfo = (Map<String, Object>) firstResult.get("road_address");
                        if (roadAddressInfo != null && roadAddressInfo.get("zone_no") != null) {
                            zipCode = (String) roadAddressInfo.get("zone_no");
                        }
                    }

                    // 좌표: x=경도, y=위도
                    Double longitude = Double.parseDouble((String) firstResult.get("x"));
                    Double latitude = Double.parseDouble((String) firstResult.get("y"));

                    // 세팅
                    if (zipCode != null && !zipCode.isEmpty()) {
                        userPreferences.setZipCode(zipCode);
                    } else {
                        userPreferences.setZipCode("00000");
                    }
                    userPreferences.setLatitude(latitude);
                    userPreferences.setLongitude(longitude);

                    if (dongId != null) {
                        userPreferences.setDongId(dongId);
                        log.info("법정동 코드 기반 동ID 설정 완료: {}", dongId);
                    }

                    // 행정동 정보( region_type="H" )도 필요하면 추가 호출
                    fetchAdministrativeDongFromCoordinates(userPreferences, longitude, latitude);

                    log.info("카카오 API 결과 - 위도: {}, 경도: {}, 우편번호: {}, 동ID: {}",
                            latitude, longitude, userPreferences.getZipCode(), userPreferences.getDongId());
                    return;
                } else {
                    log.warn("카카오 API 응답에 주소 정보가 없음 - 응답: {}", response);
                }
            }

            // 여기까지 못 왔으면 기본값으로 세팅
            log.warn("카카오 API에서 주소 정보를 찾을 수 없어 기본값 사용");
            setDefaultCoordinates(userPreferences, address);

        } catch (Exception e) {
            log.error("카카오 맵 API 호출 중 오류 발생: {}", e.getMessage(), e);
            setDefaultCoordinates(userPreferences, address);
        }
    }

    // -------------------------------
    // 좌표 -> 행정동 코드, 법정동 코드 등을 가져오기 (coord2regioncode)
    // -------------------------------
    private void fetchAdministrativeDongFromCoordinates(UserPreferences userPreferences, Double longitude, Double latitude) {
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

            // 행정동 정보 처리 (region_type="H")
            boolean foundAdminDong = false;
            for (Map<String, Object> doc : documents) {
                String regionType = (String) doc.get("region_type");
                String code = (String) doc.get("code");
                String addressName = (String) doc.get("address_name");
                String region3DepthName = (String) doc.get("region_3depth_name"); // 행정동 이름 가져오기

                if ("H".equals(regionType) && code != null && code.length() >= 8) {
                    try {
                        // 코드의 앞 8자리만 추출하여 사용
                        String dongIdStr = code.substring(0, 8);
                        Integer dongId = Integer.parseInt(dongIdStr);
                        log.info("!!행정동 코드 발견!! - 전체 코드: {}, 추출된 ID: {}, 행정동명: {}",
                                code, dongId, region3DepthName);

                        // 행정동 코드 설정
                        userPreferences.setDongId(dongId);
                        foundAdminDong = true;

                        // 동 이름으로 DB에서 dong_id 조회 시도 (선택적)
                        try {
                            // dongService를 통해 행정동 이름으로 dong_id 조회
                            Integer dongIdFromDb = dongService.findDongIdByName(region3DepthName);
                            if (dongIdFromDb != null) {
                                log.info("동 이름 기반 dong_id 설정: 이름={}, ID={}", region3DepthName, dongIdFromDb);
                                userPreferences.setDongId(dongIdFromDb);
                            }
                        } catch (Exception e) {
                            log.warn("동 이름으로 dong_id 조회 실패: {}", e.getMessage());
                            // 실패 시 이미 설정된 행정동 코드 유지
                        }

                        return; // 행정동 정보 찾았으므로 종료
                    } catch (NumberFormatException e) {
                        log.error("행정동 코드 변환 오류: {}", e.getMessage());
                    }
                }
            }

            // 행정동을 찾지 못한 경우 법정동 정보 사용
            if (!foundAdminDong) {
                for (Map<String, Object> doc : documents) {
                    String regionType = (String) doc.get("region_type");
                    String code = (String) doc.get("code");
                    String region3DepthName = (String) doc.get("region_3depth_name");

                    if ("B".equals(regionType) && code != null && code.length() >= 8) {
                        try {
                            // 코드의 앞 8자리만 추출하여 사용
                            String dongIdStr = code.substring(0, 8);
                            Integer dongId = Integer.parseInt(dongIdStr);
                            log.info("법정동 코드 사용 - 전체 코드: {}, 추출된 ID: {}, 법정동명: {}",
                                    code, dongId, region3DepthName);

                            userPreferences.setDongId(dongId);

                            // 동 이름으로 DB에서 dong_id 조회 시도 (선택적)
                            try {
                                Integer dongIdFromDb = dongService.findDongIdByName(region3DepthName);
                                if (dongIdFromDb != null) {
                                    log.info("동 이름 기반 dong_id 설정: 이름={}, ID={}", region3DepthName, dongIdFromDb);
                                    userPreferences.setDongId(dongIdFromDb);
                                }
                            } catch (Exception e) {
                                log.warn("동 이름으로 dong_id 조회 실패: {}", e.getMessage());
                            }

                            return;
                        } catch (NumberFormatException e) {
                            log.error("법정동 코드 변환 오류: {}", e.getMessage());
                        }
                    }
                }
            }

            log.warn("적절한 동 코드를 찾지 못했습니다.");

        } catch (Exception e) {
            log.error("카카오 좌표->지역코드 API 처리 중 예외 발생: {}", e.getMessage(), e);
        }
    }

    // -------------------------------
    // API 실패 시 기본 좌표/동ID 설정 (테스트용/예시용)
    // -------------------------------
    private void setDefaultCoordinates(UserPreferences userPreferences, String address) {
        if (address.contains("강남")) {
            userPreferences.setZipCode("06235");
            userPreferences.setLatitude(37.5012);
            userPreferences.setLongitude(127.0396);
            userPreferences.setDongId(11680000); // 강남구 대표값
        } else if (address.contains("서울") && address.contains("중구") && address.contains("명동")) {
            userPreferences.setZipCode("04536");
            userPreferences.setLatitude(37.5640);
            userPreferences.setLongitude(126.9830);
            userPreferences.setDongId(11140550); // 명동 대표값
        } else if (address.contains("서울")) {
            userPreferences.setZipCode("04524");
            userPreferences.setLatitude(37.5665);
            userPreferences.setLongitude(126.9780);
            userPreferences.setDongId(11000000); // 서울 대표값
        } else {
            // 기본값 (서울 명동)
            userPreferences.setZipCode("04536");
            userPreferences.setLatitude(37.5640);
            userPreferences.setLongitude(126.9830);
            userPreferences.setDongId(11140550); // 명동
        }

        log.info("기본 정보 설정 - 위도: {}, 경도: {}, 우편번호: {}, 동ID: {}",
                userPreferences.getLatitude(), userPreferences.getLongitude(),
                userPreferences.getZipCode(), userPreferences.getDongId());
    }

    // -------------------------------
    // 5) 선호도 정보 처리
    // -------------------------------
    private void processPreferences(UserPreferences userPreferences, List<String> selectedPreferences) {
        // 초기화
        userPreferences.setSafe(0.0f);
        userPreferences.setLeisure(0.0f);
        userPreferences.setRestaurant(0.0f);
        userPreferences.setHealth(0.0f);
        userPreferences.setConvenience(0.0f);
        userPreferences.setTransport(0.0f);
        userPreferences.setCafe(0.0f);

        // 선호도가 없으면 종료
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

        // 해당 항목만 1.0f로 설정
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

    // -------------------------------
    // 6) 사용자 정보 조회 (getUserById)
    // -------------------------------
    @Override
    public UserDto getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        // 사용자 선호도 정보 조회
        UserPreferences preferences = userPreferencesRepository.findById(userId).orElse(null);

        // 프로필 Dto
        UserProfileDto profileDto = UserProfileDto.builder()
                .gender(user.getGender())
                .age(user.getAge())
                .build();

        // 선호도 정보
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

    // -------------------------------
    // 7) 프로필 완성 여부 확인
    // -------------------------------
    @Override
    public boolean isProfileComplete(Integer userId) {
        return userRepository.findById(userId)
                .map(user -> user.getIsFirst() == 0)
                .orElse(false);
    }
}