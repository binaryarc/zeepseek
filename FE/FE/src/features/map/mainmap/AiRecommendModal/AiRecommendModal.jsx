import React, { useState } from 'react';
import './AiRecommendModal.css';
import { fetchAIRecommendedProperties } from '../../../../common/api/api';

function AIRecommendModal({ onClose }) {

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
      // safety: filterValues["안전"] / 100,
    };

    setIsLoading(true); // 로딩 시작
    try {
      const result = await fetchAIRecommendedProperties(preferenceData);
      if (result) {
        setRecommendedList(result.recommendedProperties);
      }
    } catch (error) {
      console.error("추천 실패:", error);
    } finally {
      setIsLoading(false); // 로딩 종료
    }
  };


  return (
    <div className="ai-modal-overlay">
      <div className="ai-modal-container">
        <button className="ai-modal-close" onClick={onClose}>X</button>

        <div className="ai-modal-content">
          {/* 왼쪽: 조건 슬라이더 */}
          <div className="ai-slider-section">
            <h3>원하는 조건을 설정하세요</h3>
            {filters.map((label) => (
              <div key={label} className="slider-item">
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
                />
              </div>
            ))}
            <button className="recommend-search-btn" onClick={handleRecommendClick}>
              매물 추천
            </button>
          </div>

          {/* 오른쪽: 지도와 매물 카드 */}
          <div className="ai-recommend-map-section">
            {/* 카카오맵 자리 */}
            <div className="ai-map-placeholder">카카오맵 영역</div>

            {/* 카드 리스트 */}
            <div className="ai-recommend-card-list">
              {isLoading ? (
                <p>로딩 중...</p>
              ) : (
                recommendedList.map((item) => (
                  <div className="ai-recommend-card" key={item.propertyId}>
                    <img src={item.imageUrl || 'https://via.placeholder.com/100'} alt="room" />
                    <div>
                      <p><strong>{item.price}</strong></p>
                      <p>{item.address}</p>
                    </div>
                  </div>
                ))
              )}
            </div>

          </div>
        </div>
      </div>
    </div>
  );
}

export default AIRecommendModal;
