import React, {useState} from "react";
import "./AiRecommend.css";
import { fetchAIRecommendedProperties, fetchNearbyPlaces } from "../../../../common/api/api";
import defaultImage from "../../../../assets/logo/192image.png"
import DongNameMarkers from "../../mainmap/salecountmarkers/DongNameMarkers/DongNameMarkers";
import GuNameMarkers from "../../mainmap/salecountmarkers/GuNameMarkers/GuNameMarkers";
import AiRecommendList from "./AiRecommendList/AiRecommendList";
import zeepai from "../../../../assets/images/zeepai.png"

const AiRecommend = () => {

  const [nearbyMarkers, setNearbyMarkers] = useState([]);     // 매물 주변 시설 위치 마킹
  const [circleOverlay, setCircleOverlay] = useState(null);   // 매물 반경 1km 원 마킹
  const [selectedRoom, setSelectedRoom] = useState(null);     // 모달에 띄울 매물


  const [maxType, setMaxType] = useState(null);

  const filters = [
    "여가", "식당", "보건", "편의", "대중교통", "카페", "치킨집",
  ];

  // 상태를 key-value 형태로 관리
  const [filterValues, setFilterValues] = useState(
    filters.reduce((acc, label) => {
      acc[label] = 50; // 초기값 50
      return acc;
    }, {})
  );

  const [recommendedList, setRecommendedList] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isRecoDone, setIsRecoDone] = useState(false);

  const handleSliderChange = (label, value) => {
    setFilterValues((prev) => ({
      ...prev,
      [label]: value,
    }));
  };

  const handleRecommendClick = async () => {
    const preferenceData = {
      userId: 123,
      transportScore: filterValues["대중교통"] / 100,
      restaurantScore: filterValues["식당"] / 100,
      healthScore: filterValues["보건"] / 100,
      convenienceScore: filterValues["편의"] / 100,
      cafeScore: filterValues["카페"] / 100,
      chickenScore: filterValues["치킨집"] / 100,
      leisureScore: filterValues["여가"] / 100,
    };
    
    console.log("request data: ", preferenceData)

    setIsLoading(true); // 로딩 시작
    try {
      const result = await fetchAIRecommendedProperties(preferenceData);
      if (result) {
        console.log("전체 추천 결과: ", result)
        console.log("추천 매물 목록:", result.recommendedProperties);
        setRecommendedList(result.recommendedProperties);
        setIsRecoDone(true);
        setMaxType(result.maxType)
      }
    } catch (error) {
      console.error("추천 실패:", error);
    } finally {
      setIsLoading(false); // 로딩 종료
    }
  };

  const handleRetry = () => {
    setIsRecoDone(false);
    setRecommendedList([]);
  };


  return (
    <div className="ai-filter-container">
      <DongNameMarkers map={window.map} />
      <GuNameMarkers map={window.map} />
      {!isRecoDone && !isLoading && (
        <div className="slider-section">
          <h3 className="recommend-title">나랑 딱 맞는 매물 찾기</h3>
          {filters.map((label) => (
           <div key={label} className={`slider-block slider-${label}`}>
              <div className="slider-label-row">
                <label>{label}</label>
                <span>{filterValues[label]}</span>
              </div>
              <input
                type="range"
                min="1"
                max="100"
                value={filterValues[label]}
                onChange={(e) => handleSliderChange(label, Number(e.target.value))}
                style={{
                  "--value": `${filterValues[label]}%`,
                }}
              />
            </div>
          ))}
          <button className="recommend-search-btn" onClick={handleRecommendClick}>AI 추천 받기</button>
        </div>
      )}

      {isLoading && (
        <div className="loader-container">
          <img src={zeepai} alt="ai_image" className="zeepai_image" />
          <p>AI가 매물을 추천 중이에요...</p>
          <div className="spinner"></div>
        </div>
      )}

      {isRecoDone && !isLoading && (
        <div className="result-section">
          <button className="recommend-search-btn" onClick={handleRetry}>매물 추천 다시 받기</button>
          <div className="recommend-results">
            <h4 className="result-title">추천 매물 목록 ({recommendedList.length}건)</h4>
            <ul className="result-list">
              {recommendedList.map((item) => (
              <li
                key={item.propertyId}
                className="room-item"
                onMouseEnter={async () => {

                  // 💥 기존 마커들 먼저 제거
                  nearbyMarkers.forEach(marker => marker.setMap(null));
                  setNearbyMarkers([]);
                  if (circleOverlay) {
                    circleOverlay.setMap(null);
                    setCircleOverlay(null);
                  }

                  if (item.latitude && item.longitude) {
                    window.setHoverMarker(item.latitude, item.longitude);
                    if (window.map) {
                      const latlng = new window.kakao.maps.LatLng(item.latitude, item.longitude);
                      window.map.setLevel(5);
                      window.map.setCenter(latlng);

                      // 반경 1km 원 추가
                      const circle = new window.kakao.maps.Circle({
                        center: latlng,
                        radius: 1000,
                        strokeWeight: 2,
                        strokeColor: '#00a0e9',
                        strokeOpacity: 0.8,
                        strokeStyle: 'solid',
                        fillColor: '#00a0e9',
                        fillOpacity: 0.1
                      });
                      circle.setMap(window.map);
                      setCircleOverlay(circle);
                    }

                    const newMarkers = [];
                      try {
                        const response = await fetchNearbyPlaces(maxType, item.longitude, item.latitude);

                        const imageSrc = `/images/icons/${maxType}.png`;
                        const imageSize = new window.kakao.maps.Size(30, 30);
                        const markerImage = new window.kakao.maps.MarkerImage(imageSrc, imageSize);

                        const places = response?.data || [];
                        places.forEach(({ latitude, longitude, name }) => {
                          const marker = new window.kakao.maps.Marker({
                            position: new window.kakao.maps.LatLng(latitude, longitude),
                            map: window.map,
                            title: name,
                            image: markerImage,
                          });
                          newMarkers.push(marker);
                        });
                      } catch (err) {
                        console.error('마커 불러오기 실패:', err);
                      }
                    setNearbyMarkers(newMarkers);
                  } else {
                    console.log("그럼 여기야?")
                  }
                }}
                onMouseLeave={() => {
                  window.clearHoverMarker();
                  nearbyMarkers.forEach(marker => marker.setMap(null));
                  setNearbyMarkers([]);
                  if (circleOverlay) {
                    circleOverlay.setMap(null);
                    setCircleOverlay(null);
                  }
                }}
                onClick={() => setSelectedRoom(item)}
              >
                <img src={item.imageUrl || defaultImage} alt="매물 이미지" />
                <div className="room-info">
                  <p className="room-title">
                    {item.contractType} {item.price}
                  </p>
                  <p className="room-description">{item.description}</p>
                  <p className="room-address">{item.address}</p>
                </div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
      {selectedRoom && (
        <AiRecommendList room={selectedRoom} onClose={() => setSelectedRoom(null)} />
      )}
    </div>
  );
};

export default AiRecommend;