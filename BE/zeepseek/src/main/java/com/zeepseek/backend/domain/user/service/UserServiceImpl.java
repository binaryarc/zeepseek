package com.zeepseek.backend.domain.user.service;

import com.zeepseek.backend.domain.auth.exception.AuthException;
import com.zeepseek.backend.domain.dong.service.DongService;
import com.zeepseek.backend.domain.user.dto.UserDto;
import com.zeepseek.backend.domain.user.dto.UserProfileDto;
import com.zeepseek.backend.domain.user.entity.User;
import com.zeepseek.backend.domain.user.entity.UserPreferences;
import com.zeepseek.backend.domain.user.repository.UserPreferencesRepository;
import com.zeepseek.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final DongService dongService; // 추가된 의존성

    @Value("${VITE_APP_KAKAO_MAP_API_KEY:카카오 맵 API 키}")
    private String kakaoMapApiKey;

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

        // 이미 첫 로그인이 아니면 에러
        if (user.getIsFirst() != 1) {
            throw new AuthException("User is not a first-time user");
        }

        // 사용자 기본 정보 업데이트 (성별, 나이를 User 테이블에 저장)
        if (profileDto.getGender() != null) {
            user.setGender(profileDto.getGender());
        }
        if (profileDto.getAge() != null) {
            user.setAge(profileDto.getAge());
        }

        // 첫 로그인 플래그 변경 (프로필 설정 완료 시 0으로 변경)
        user.setIsFirst(0);
        User updatedUser = userRepository.save(user);

        // UserPreferences 객체 생성 또는 조회
        UserPreferences userPreferences = userPreferencesRepository.findById(userId)
                .orElse(UserPreferences.builder().user(user).build());

        // 1. 목적지 정보 처리
        processDestination(userPreferences, profileDto.getLocation());

        // 2. 선호도 정보 처리
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

    /**
     * 목적지 정보 처리
     */
    private void processDestination(UserPreferences userPreferences, String location) {
        if (location == null || location.isEmpty()) {
            return;
        }

        // 전체 주소는 destination 필드에 저장
        userPreferences.setDestination(location);

        try {
            // 매우 기본적인 주소 파싱 (예시)
            // "서울특별시 강남구 테헤란로 152 10층" 형태 가정
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
                // 나머지 부분을 상세 주소로 설정
                StringBuilder detailAddress = new StringBuilder();
                for (int i = 4; i < addressParts.length; i++) {
                    detailAddress.append(addressParts[i]).append(" ");
                }
                userPreferences.setDetailAddress(detailAddress.toString().trim());
            }

            // 카카오 맵 API를 이용하여 우편번호, 위도, 경도 설정
            fetchCoordinatesAndZipCodeFromKakao(userPreferences, location);

        } catch (Exception e) {
            log.warn("주소 파싱 중 오류 발생: {}", e.getMessage());
            // 파싱에 실패해도 전체 주소는 저장됨
        }

        log.info("목적지 정보 설정: {}", location);
    }

    /**
     * 카카오 맵 API를 이용하여 주소로부터 좌표(위도, 경도), 우편번호, 동ID를 가져옴
     */
    private void fetchCoordinatesAndZipCodeFromKakao(UserPreferences userPreferences, String address) {
        String kakaoApiUrl = "https://dapi.kakao.com/v2/local/search/address.json";

        try {
            // 주소 인코딩
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);

            // WebClient를 사용하여 카카오 API 호출
            WebClient webClient = WebClient.builder().build();

            log.info("카카오 맵 API 호출 시작 - 주소: {}", address);

            Map<String, Object> response = webClient.get()
                    .uri(kakaoApiUrl + "?query=" + encodedAddress)
                    .header("Authorization", "KakaoAK " + kakaoMapApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("카카오 맵 API 응답 수신 - 응답: {}", response);

            // 응답 데이터에서 위도, 경도, 우편번호, 법정동 코드 추출
            if (response != null) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                if (documents != null && !documents.isEmpty()) {
                    Map<String, Object> firstResult = documents.get(0);

                    // 주소 정보 (우편번호, 법정동 코드 포함)
                    Map<String, Object> addressInfo = (Map<String, Object>) firstResult.get("address");
                    String zipCode = "";
                    Integer dongId = null;

                    if (addressInfo != null) {
                        // 우편번호 추출
                        if (addressInfo.get("zip_code") != null) {
                            zipCode = (String) addressInfo.get("zip_code");
                        }

                        // 법정동 코드 추출 (b_code)
                        if (addressInfo.get("b_code") != null) {
                            String bCode = (String) addressInfo.get("b_code");
                            if (bCode != null && bCode.length() >= 8) {
                                // 10자리 법정동 코드에서 뒤 2자리를 제외한 8자리만 사용
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
                        // 로드뷰 주소(road_address)에서 우편번호 확인 시도
                        Map<String, Object> roadAddressInfo = (Map<String, Object>) firstResult.get("road_address");
                        if (roadAddressInfo != null && roadAddressInfo.get("zone_no") != null) {
                            zipCode = (String) roadAddressInfo.get("zone_no");
                        }
                    }

                    // 좌표 정보 (x: 경도, y: 위도)
                    Double longitude = Double.parseDouble((String) firstResult.get("x"));
                    Double latitude = Double.parseDouble((String) firstResult.get("y"));

                    // UserPreferences에 정보 설정
                    if (zipCode != null && !zipCode.isEmpty()) {
                        userPreferences.setZipCode(zipCode);
                    } else {
                        userPreferences.setZipCode("00000"); // 우편번호가 없는 경우 기본값
                    }

                    userPreferences.setLatitude(latitude);
                    userPreferences.setLongitude(longitude);

                    // 동ID 설정 (기존 코드)
                    if (dongId != null) {
                        userPreferences.setDongId(dongId);
                        log.info("법정동 코드 기반 동ID 설정 완료: {}", dongId);
                    }

                    // 추가: 좌표 기반 행정동 정보 조회 (역방향 지오코딩)
                    fetchAdministrativeDongFromCoordinates(userPreferences, longitude, latitude);

                    log.info("카카오 API로부터 가져온 정보 - 위도: {}, 경도: {}, 우편번호: {}, 동ID: {}",
                            latitude, longitude, zipCode, userPreferences.getDongId());
                    return;
                } else {
                    log.warn("카카오 API 응답에 주소 정보가 없음 - 응답: {}", response);
                }
            }

            // API 응답에서 데이터를 추출하지 못한 경우 지역에 따른 기본값 설정
            log.warn("카카오 API에서 주소 정보를 찾을 수 없어 기본값 사용");
            setDefaultCoordinates(userPreferences, address);

        } catch (Exception e) {
            log.error("카카오 맵 API 호출 중 오류 발생: {}", e.getMessage(), e);
            // 에러가 발생하면 기본값 설정
            setDefaultCoordinates(userPreferences, address);
        }
    }

    /**
     * 좌표로부터 행정동 정보를 가져와 dong_id를 설정하는 메소드
     */
    private void fetchAdministrativeDongFromCoordinates(UserPreferences userPreferences, Double longitude, Double latitude) {
        String kakaoReverseGeoUrl = "https://dapi.kakao.com/v2/local/geo/coord2address.json";

        try {
            // 이미 좌표가 있는지 확인
            if (longitude == null || latitude == null) {
                log.warn("좌표 정보가 없어 행정동 조회를 건너뜁니다.");
                return;
            }

            // WebClient를 사용하여 카카오 API 호출
            WebClient webClient = WebClient.builder().build();

            log.info("카카오 역방향 지오코딩 API 호출 - 경도: {}, 위도: {}", longitude, latitude);

            Map<String, Object> response = webClient.get()
                    .uri(kakaoReverseGeoUrl + "?x=" + longitude + "&y=" + latitude)
                    .header("Authorization", "KakaoAK " + kakaoMapApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("카카오 역방향 지오코딩 API 응답 수신 - 응답: {}", response);

            // 응답 데이터에서 행정동 정보 추출
            if (response != null) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                if (documents != null && !documents.isEmpty()) {
                    Map<String, Object> firstResult = documents.get(0);

                    // 지번 주소 정보
                    Map<String, Object> address = (Map<String, Object>) firstResult.get("address");

                    if (address != null) {
                        // 행정동명 추출 (region_3depth_name은 행정동명을 포함)
                        String dongName = (String) address.get("region_3depth_name");

                        if (dongName != null && !dongName.isEmpty()) {
                            log.info("카카오 API에서 행정동명 추출: {}", dongName);

                            // DongService를 통해 동 이름으로 dong_id 조회
                            Integer dongId = dongService.findDongIdByName(dongName);

                            if (dongId != null && dongId > 0) {
                                userPreferences.setDongId(dongId);
                                log.info("행정동 코드 설정 완료 - 동명: {}, 동ID: {}", dongName, dongId);
                                return;
                            } else {
                                log.warn("동 이름({})에 해당하는 dong_id를 찾을 수 없습니다.", dongName);
                            }
                        }
                    }
                }
            }

            log.warn("카카오 API에서 행정동 정보를 찾을 수 없어 기존 값 유지");

        } catch (Exception e) {
            log.error("카카오 역방향 지오코딩 API 호출 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * API 호출 실패 시 주소 키워드에 따른 기본 좌표 및 동ID 설정
     */
    private void setDefaultCoordinates(UserPreferences userPreferences, String address) {
        // 주소 키워드에 따른 기본값 설정
        if (address.contains("부산") && address.contains("강서구") && address.contains("명지")) {
            // 부산 명지동 명지국제2로 41 특정 좌표
            userPreferences.setZipCode("46769");
            userPreferences.setLatitude(35.0947817266961);
            userPreferences.setLongitude(128.906874174632);
            userPreferences.setDongId(26350106); // 명지동 동ID
        } else if (address.contains("부산") && address.contains("강서구")) {
            // 부산 강서구
            userPreferences.setZipCode("46702");
            userPreferences.setLatitude(35.2121);
            userPreferences.setLongitude(128.9812);
            userPreferences.setDongId(26350000); // 강서구 동ID (8자리)
        } else if (address.contains("부산")) {
            // 부산 기본 좌표 (부산시청)
            userPreferences.setZipCode("47545");
            userPreferences.setLatitude(35.1798);
            userPreferences.setLongitude(129.0750);
            userPreferences.setDongId(26000000); // 부산 동ID (8자리)
        } else if (address.contains("강남")) {
            userPreferences.setZipCode("06235");
            userPreferences.setLatitude(37.5012);
            userPreferences.setLongitude(127.0396);
            userPreferences.setDongId(11680000); // 강남구 동ID (8자리)
        } else if (address.contains("서울")) {
            userPreferences.setZipCode("04524");
            userPreferences.setLatitude(37.5665);
            userPreferences.setLongitude(126.9780);
            userPreferences.setDongId(11000000); // 서울 동ID (8자리)
        } else {
            // 기본값 (한국 중심부)
            userPreferences.setZipCode("00000");
            userPreferences.setLatitude(36.5);
            userPreferences.setLongitude(127.8);
            // 동ID는 설정하지 않음 (null)
        }

        log.info("기본 정보 설정 - 위도: {}, 경도: {}, 우편번호: {}, 동ID: {}",
                userPreferences.getLatitude(), userPreferences.getLongitude(),
                userPreferences.getZipCode(), userPreferences.getDongId());
    }

    /**
     * 선호도 정보 처리
     */
    private void processPreferences(UserPreferences userPreferences, List<String> selectedPreferences) {
        // 1. 모든 선호도 필드를 0.0f로 초기화
        userPreferences.setSafe(0.0f);
        userPreferences.setLeisure(0.0f);
        userPreferences.setRestaurant(0.0f);
        userPreferences.setHealth(0.0f);
        userPreferences.setConvenience(0.0f);
        userPreferences.setTransport(0.0f);
        userPreferences.setCafe(0.0f);

        // 선호도가 없으면 여기서 종료
        if (selectedPreferences == null || selectedPreferences.isEmpty()) {
            log.info("선택된 선호도가 없습니다.");
            return;
        }

        // 2. 최대 3개 선택으로 제한
        List<String> limitedPreferences = selectedPreferences;
        if (selectedPreferences.size() > 3) {
            log.warn("사용자가 3개 이상의 선호도를 선택했습니다. 상위 3개만 처리합니다.");
            limitedPreferences = selectedPreferences.subList(0, 3);
        }

        log.info("선택된 선호도: {}", limitedPreferences);

        // 3. 선택된 항목만 1.0f로 설정
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

        // 사용자 선호도 정보 조회
        UserPreferences preferences = userPreferencesRepository.findById(userId).orElse(null);

        // 프로필 정보 구성
        UserProfileDto profileDto = UserProfileDto.builder()
                .gender(user.getGender())
                .age(user.getAge())
                .build();

        // 선호도 정보가 있는 경우 추가
        if (preferences != null) {
            // 위치 정보 설정
            profileDto.setLocation(preferences.getDestination());

            // 선호도 정보 설정 (1.0f 값을 가진 항목만 추출)
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

        // UserDto 구성
        return UserDto.builder()
                .idx(user.getIdx())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .isFirst(user.getIsFirst())
                .isSeller(user.getIsSeller())
                .provider(user.getProvider())
                .profileInfo(profileDto) // 프로필 정보 추가
                .build();
    }

    @Override
    public boolean isProfileComplete(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.map(user -> user.getIsFirst() == 0).orElse(false);
    }
}