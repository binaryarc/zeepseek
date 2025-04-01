import React, {useState} from "react";
import "./AiRecommend.css";
import { fetchAIRecommendedProperties } from "../../../../common/api/api";
import defaultImage from "../../../../assets/logo/192image.png"
import DongNameMarkers from "../../mainmap/salecountmarkers/DongNameMarkers/DongNameMarkers";
import GuNameMarkers from "../../mainmap/salecountmarkers/GuNameMarkers/GuNameMarkers";

const AiRecommend = () => {

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
          console.log("추천 매물 목록:", result.recommendedProperties);
          setRecommendedList(result.recommendedProperties);
          setIsRecoDone(true);
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
            <h3 className="recommend-title">원하는 매물 조건을 설정하세요</h3>
            {filters.map((label) => (
              <div key={label} className="slider-block">
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
            <button className="recommend-search-btn" onClick={handleRecommendClick}>매물 추천</button>
          </div>
        )}
  
        {isLoading && (
          <div className="loader-container">
            <div className="spinner"></div>
            <p>AI가 매물을 추천 중이에요...</p>
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
                    onMouseEnter={() => {
                      if (item.latitude && item.longitude) {
                        window.setHoverMarker(item.latitude, item.longitude);
                        // 지도 중심 이동 추가
                        if (window.map) {
                          const latlng = new window.kakao.maps.LatLng(item.latitude, item.longitude);
                          window.map.setLevel(5); // ✅ 줌 레벨 5로 설정
                          window.map.setCenter(latlng); // ✅ 중심 이동
                        }
                      }
                    }}
                    onMouseLeave={() => {
                      window.clearHoverMarker();
                    }}
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
      </div>
    );
};

export default AiRecommend;