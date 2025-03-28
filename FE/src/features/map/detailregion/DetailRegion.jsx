// map/detailregion/DetailRegion.jsx
import "./DetailRegion.css";
import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { fetchDongDetail } from "../../../common/api/api";

const getTop3Scores = (dongData) => {
  const categories = {
    convenience: { label: "편의", icon: "🛍️" },
    transport: { label: "교통", icon: "🚇" },
    leisure: { label: "여가", icon: "🎮" },
    health: { label: "건강", icon: "🏥" },
    restaurant: { label: "식당", icon: "🍽️" },
    mart: { label: "마트", icon: "🛒" },
    safe: { label: "안전", icon: "🛡️" },
  };

  return Object.entries(categories)
    .map(([key, { label, icon }]) => ({
      key,
      label,
      icon,
      value: dongData[key],
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 3);
};


const DetailRegion = () => {
  const dongId = useSelector((state) => state.roomList.currentDongId); // Redux에서 가져오기
  const [dongData, setDongData] = useState(null);


  useEffect(() => {
    if (!dongId) return;

    const loadDongDetail = async () => {
      const data = await fetchDongDetail(dongId);
      setDongData(data);
    };

    loadDongDetail();
  }, [dongId]);

  if (!dongData) {
    return (
      <div className="detail-region-box">
        <div className="spinner-wrapper">
          <div className="spinner"></div>
        </div>
      </div>
    );
  }

  const topScores = getTop3Scores(dongData);

  return (
    <div className="detail-region-box">
      <h3 className="dong-title">
        {dongData.guName} {dongData.name}
      </h3>

      <div className="score-bars">
        {topScores.map(({ label, icon, value }) => (
          <div key={label} className="score-item">
            <span className="score-label">{icon} {label}</span>
            <div className="score-bar-wrapper">
              <div className="score-bar" style={{ width: `${value}%` }} />
            </div>
          </div>
        ))}
      </div>

      <p className="summary-title">📍 동네 요약</p>
      <p className="summary">{dongData.summary}</p>
    </div>
  );
};

export default DetailRegion;