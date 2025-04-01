package com.zeepseek.backend.domain.user.service;

import com.zeepseek.backend.domain.auth.exception.AuthException;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    
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
     * 카카오 맵 API를 이용하여 주소로부터 좌표(위도, 경도)와 우편번호를 가져옴
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
            
            // 응답 데이터에서 위도, 경도, 우편번호 추출
            if (response != null) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                if (documents != null && !documents.isEmpty()) {
                    Map<String, Object> firstResult = documents.get(0);
                    
                    // 주소 정보 (우편번호 포함)
                    Map<String, Object> addressInfo = (Map<String, Object>) firstResult.get("address");
                    String zipCode = "";
                    
                    if (addressInfo != null && addressInfo.get("zip_code") != null) {
                        zipCode = (String) addressInfo.get("zip_code");
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
                    
                    log.info("카카오 API로부터 가져온 좌표 정보 - 위도: {}, 경도: {}, 우편번호: {}", 
                            latitude, longitude, zipCode);
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
     * API 호출 실패 시 주소 키워드에 따른 기본 좌표 설정
     */
    private void setDefaultCoordinates(UserPreferences userPreferences, String address) {
        // 주소 키워드에 따른 기본값 설정
        if (address.contains("부산") && address.contains("강서구") && address.contains("명지")) {
            // 부산 명지동 명지국제2로 41 특정 좌표
            userPreferences.setZipCode("46769");  
            userPreferences.setLatitude(35.0947817266961);
            userPreferences.setLongitude(128.906874174632);
        } else if (address.contains("부산") && address.contains("강서구")) {
            // 부산 강서구
            userPreferences.setZipCode("46702");
            userPreferences.setLatitude(35.2121);
            userPreferences.setLongitude(128.9812);
        } else if (address.contains("부산")) {
            // 부산 기본 좌표 (부산시청)
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
            // 기본값 (한국 중심부)
            userPreferences.setZipCode("00000");
            userPreferences.setLatitude(36.5);
            userPreferences.setLongitude(127.8);
        }
        
        log.info("기본 좌표 정보 설정 - 위도: {}, 경도: {}, 우편번호: {}", 
                userPreferences.getLatitude(), userPreferences.getLongitude(), userPreferences.getZipCode());
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

        return UserDto.builder()
                .idx(user.getIdx())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .isFirst(user.getIsFirst())
                .isSeller(user.getIsSeller())
                .provider(user.getProvider())
                .build();
    }

    @Override
    public boolean isProfileComplete(Integer userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.map(user -> user.getIsFirst() == 0).orElse(false);
    }
}