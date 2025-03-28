// /features/compare/estate/EstateCompare.jsx
import React, { useEffect, useState } from "react";
import "./EstateCompare.css";
import { fetchPropertyCompare } from "../../../common/api/api"; // 비교 API 호출 함수
// import { useSelector } from "react-redux";


const EstateCompare = () => {
  const [compareData, setCompareData] = useState(null);
  const [loading, setLoading] = useState(true);

  // 임시: 비교할 매물 ID와 목적지 좌표 (나중에 Redux 또는 props로)
  const propertyId1 = 6768;
  const propertyId2 = 6772;
  const latitude = 37.5665;
  const longitude = 126.978;

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const result = await fetchPropertyCompare({
          propertyId1,
          propertyId2,
          latitude,
          longitude,
        });
        setCompareData(result);
      } catch (err) {
        console.error("매물 비교 실패:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) return <div className="estate-compare-loading">로딩 중...</div>;
  if (!compareData) return <div>데이터 없음</div>;

  const labels = [
    { key: "convenience", label: "편의 🛍️" },
    { key: "health", label: "보건 🏥" },
    { key: "leisure", label: "여가 🎮" },
    { key: "safe", label: "안전 🛡️" },
    { key: "cafe", label: "카페 ☕" },
    { key: "transport", label: "대중교통 🚇" },
    { key: "restaurant", label: "식당 🍽️" },
    { key: "bar", label: "술집 🍺" },
  ];

  const [p1, p2] = compareData.properties;

  return (
    <div className="estate-compare-container">
      <Navbar />
      <h2>🏡 매물 비교</h2>
      <div className="score-section">
        {labels.map(({ key, label }) => (
          <div className="score-row" key={key}>
            <span className="score-label">{label}</span>
            <div className="score-bars">
              <div className="score-bar left" style={{ width: `${p1[key] * 10}%` }}>
                {p1[key]}
              </div>
              <div className="score-bar right" style={{ width: `${p2[key] * 10}%` }}>
                {p2[key]}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="summary-section">
        <h3>🤖 ZEEPSEEK AI의 매물 비교 요약</h3>
        <p>{compareData.gpt.content}</p>
      </div>
    </div>
  );
};

export default EstateCompare;
