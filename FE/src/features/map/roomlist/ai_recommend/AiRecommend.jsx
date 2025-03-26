import React, {useState} from "react";
import "./AiRecommend.css";

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
  
    const handleSliderChange = (label, value) => {
      setFilterValues((prev) => ({
        ...prev,
        [label]: value,
      }));
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
      <button className="search-btn">매물 검색</button>
    </div>
  )
};

export default AiRecommend;