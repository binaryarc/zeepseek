import React, {useState} from "react";
import "./AiRecommend.css";
import { fetchAIRecommendedProperties } from "../../../../common/api/api";

const AiRecommend = () => {

  const filters = [
    "안전", "여가", "식당", "보건", "편의", "대중교통", "카페", "치킨집",
  ];

    // 상태를 key-value 형태로 관리
    const [filterValues, setFilterValues] = useState(
      filters.reduce((acc, label) => {
        acc[label] = 50; // 초기값 50
        return acc;
      }, {})
    );

    const [recommendedList, setRecommendedList] = useState([]);
  
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
        safety: filterValues["안전"] / 100,
      };
      
      console.log("request data: ", preferenceData)

      const result = await fetchAIRecommendedProperties(preferenceData);
      if (result) {
        console.log("추천 매물 목록:", result.recommendedProperties);
        setRecommendedList(result.recommendedProperties);
      }
    };
  

  return (
    <div className="ai-filter-container">
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
      <button className="search-btn" onClick={handleRecommendClick}>매물 검색</button>

        {/* 추천 매물 리스트 출력 */}
        {recommendedList.length > 0 && (
        <div className="recommend-results">
          <h4 className="result-title">추천 매물 목록 ({recommendedList.length}건)</h4>
          <ul className="result-list">
            {recommendedList.map((item) => (
              <li key={item.propertyId} className="result-item">
                <div className="item-header">
                  <strong>{item.address}</strong>
                  <span className="price">{item.price}</span>
                </div>
                <div className="item-sub">
                  {item.roomType} / {item.contractType} / {item.roomBathCount} / 관리비: {item.maintenanceFee?.toLocaleString()}원
                </div>
                <div className="item-desc">{item.description}</div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
};

export default AiRecommend;