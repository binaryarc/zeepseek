// /features/compare/estate/EstateCompare.jsx
import React, { useEffect, useState } from "react";
import "./EstateCompare.css";
import {
  fetchLikedProperties,
  fetchPropertyCompare,
} from "../../../common/api/api";
import { useSelector } from "react-redux";
import {
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
  Legend,
  ResponsiveContainer,
} from "recharts";
import zeepai from "../../../assets/images/zeepai.png";

const EstateCompare = () => {
  const [likedProperties, setLikedProperties] = useState([]);
  const [selected1, setSelected1] = useState(null);
  const [selected2, setSelected2] = useState(null);
  const [compareData, setCompareData] = useState(null);
  const [loading, setLoading] = useState(false);
  const user = useSelector((state) => state.auth.user);

  useEffect(() => {
    if (!user?.idx) return;
    const loadLiked = async () => {
      try {
        const res = await fetchLikedProperties(user.idx);
        setLikedProperties(res);
      } catch (err) {
        console.error("찜한 매물 불러오기 실패:", err);
      }
    };
    loadLiked();
  }, [user?.idx]);

  useEffect(() => {
    const fetchComparison = async () => {
      if (!selected1 || !selected2) return;
      setLoading(true);
      try {
        const result = await fetchPropertyCompare({
          prop1: selected1.propertyId,
          prop2: selected2.propertyId,
        });
        setCompareData(result);
      } catch (err) {
        console.error("매물 비교 실패:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchComparison();
  }, [selected1, selected2]);

  const scoreLabels = [
    { label: "편의", key: "convenience" },
    { label: "보건", key: "health" },
    { label: "여가", key: "leisure" },
    { label: "안전", key: "safe" },
    { label: "카페", key: "cafe" },
    { label: "대중교통", key: "transport" },
    { label: "식당", key: "restaurant" },
    { label: "술집", key: "bar" },
  ];

  const chartData = scoreLabels.map(({ label, key }) => ({
    subject: label,
    [selected1?.propertyId]: compareData?.properties?.[0]?.[key] || 0,
    [selected2?.propertyId]: compareData?.properties?.[1]?.[key] || 0,
    fullMark: 100,
  }));

  return (
    <div className="region-compare-total-container">
      <div className="region-compare-wrapper">
        <div className="region-compare-container">
          <div className="region-input-row">
            <div className="region-input-wrapper">
              <input
                type="text"
                placeholder="매물 ① 선택"
                value={selected1 ? selected1.address : ""}
                readOnly
              />
              {selected1 && (
                <button
                  className="region-clear-button"
                  onClick={() => setSelected1(null)}
                >
                  ❌
                </button>
              )}
            </div>
            <div className="region-input-wrapper">
              <input
                type="text"
                placeholder="매물 ② 선택"
                value={selected2 ? selected2.address : ""}
                readOnly
              />
              {selected2 && (
                <button
                  className="region-clear-button"
                  onClick={() => setSelected2(null)}
                >
                  ❌
                </button>
              )}
            </div>
          </div>

          {!loading && selected1 && selected2 && compareData && (
            <div className="compare-table">
              <ResponsiveContainer width="100%" height={400}>
                <RadarChart outerRadius={130} data={chartData}>
                  <PolarGrid />
                  <PolarAngleAxis dataKey="subject" />
                  <PolarRadiusAxis angle={30} domain={[0, 100]} />
                  <Radar
                    name={selected1.address}
                    dataKey={selected1.propertyId}
                    stroke="#4CAF50"
                    fill="#4CAF50"
                    fillOpacity={0.3}
                  />
                  <Radar
                    name={selected2.address}
                    dataKey={selected2.propertyId}
                    stroke="#673AB7"
                    fill="#673AB7"
                    fillOpacity={0.3}
                  />
                  <Legend />
                </RadarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="liked-region-box">
          <h4>찜한 매물</h4>
          <ul>
            {likedProperties.length > 0 ? (
              likedProperties.map((property) => {
                const isSelected =
                  selected1?.propertyId === property.propertyId ||
                  selected2?.propertyId === property.propertyId;
                return (
                  <li
                    key={property.propertyId}
                    className={isSelected ? "selected-region" : ""}
                    onClick={() => {
                      if (isSelected) return;
                      if (!selected1) setSelected1(property);
                      else if (!selected2) setSelected2(property);
                      else {
                        setSelected1(property);
                        setSelected2(null);
                      }
                    }}
                  >
                    {property.address}{" "}
                    {selected1?.propertyId === property.propertyId
                      ? "①"
                      : selected2?.propertyId === property.propertyId
                      ? "②"
                      : ""}
                  </li>
                );
              })
            ) : (
              <li>찜한 매물이 없습니다.</li>
            )}
          </ul>
        </div>
      </div>

      {compareData?.compareSummary && (
        <div className="summary-box">
          <div className="summary-box-header">
            <img src={zeepai} alt="ai_image" className="zeepai_summary_image" />
            <p className="summary-box-title">ZEEPSEEK AI의 매물 비교 요약</p>
          </div>
          <p>{compareData.compareSummary}</p>
        </div>
      )}
    </div>
  );
};

export default EstateCompare;
