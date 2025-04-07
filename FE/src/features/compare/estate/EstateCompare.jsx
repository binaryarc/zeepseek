// /features/compare/estate/EstateCompare.jsx
import React, { useEffect, useState } from "react";
import "./EstateCompare.css";
import {
  fetchLikedProperties,
  fetchPropertyCompare,
  fetchProPertyScore
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
import defaultImg from "../../../assets/logo/512image.png"

const EstateCompare = () => {
  const [likedProperties, setLikedProperties] = useState([]);
  const [selected1, setSelected1] = useState(null);
  const [selected2, setSelected2] = useState(null);
  const [propertyCompareData, setProPertyCompareData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [estateScores, setEstateScores] = useState({});
  const user = useSelector((state) => state.auth.user);

  // 기존 useState에 추가
  const [lastCompared1, setLastCompared1] = useState(null);
  const [lastCompared2, setLastCompared2] = useState(null);


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
    const fetchPropertyCompareData = async () => {
      if (!selected1 || !selected2) return;
      setLoading(true);
      try {
        // Promise.all([...]) : 배열 구조 분해 할당이라네요ㅎㅎ
        // 여러 개 비동기 작업을 동시에 실행하고, 모든 Promise가 완료될 때까지 기다렸다가 각 결과를 하나의 배열로 반환
        const [data1, data2] = await Promise.all([
          fetchProPertyScore(selected1.propertyId),
          fetchProPertyScore(selected2.propertyId),
        ]);

        // 매물 비교 내용 유지되도록 저장
        setEstateScores({ [selected1.propertyId]: data1, [selected2.propertyId]: data2 });
        setLastCompared1(selected1);
        setLastCompared2(selected2);

        const summaryResult = await fetchPropertyCompare(user.idx, selected1.propertyId, selected2.propertyId);
        setProPertyCompareData(summaryResult?.data?.compareSummary);

        console.log("estate1", selected1)
        console.log("estate2", selected2)
        console.log("summaryResult", summaryResult?.data?.compareSummary)
      } catch (err) {
        console.error('비교 데이터 로딩 실패:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchPropertyCompareData();
  }, [selected1, selected2]);

  const scoreLabels = [
    { label: "편의", key: "convenienceScore" },
    { label: "보건", key: "healthScore" },
    { label: "여가", key: "leisureScore" },
    { label: "카페", key: "cafeScore" },
    { label: "대중교통", key: "transportScore" },
    { label: "식당", key: "restaurantScore" },
    { label: "치킨", key: "chickenScore" },
  ];


  const chartData = scoreLabels.map(({ label, key }) => ({
    subject: label,
    [lastCompared1?.propertyId]: estateScores[lastCompared1?.propertyId]?.[key] || 0,
    [lastCompared2?.propertyId]: estateScores[lastCompared2?.propertyId]?.[key] || 0,
    fullMark: 100,
  }));


  useEffect(() => {
    console.log(chartData)
    console.log("estatescore: ", estateScores)
  }, [chartData])

  return (
    <div className="region-compare-total-container">
      <div className="region-compare-wrapper">
        <div className="region-compare-container">
          {loading && (
          <div className="loading-overlay">
            <div className="spinner"></div>
              비교 데이터를 불러오는 중입니다...
            </div>
          )}
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




          {!loading && lastCompared1 && lastCompared2 && propertyCompareData && (
            <div className="compare-table">
              <ResponsiveContainer width="100%" height={400}>
                <RadarChart outerRadius={130} data={chartData}>
                  <PolarGrid />
                  <PolarAngleAxis dataKey="subject" />
                  <PolarRadiusAxis angle={70} domain={[0, 100]} />
                  <Radar
                    name={lastCompared1.address}
                    dataKey={lastCompared1.propertyId}
                    stroke="#4CAF50"
                    fill="#4CAF50"
                    fillOpacity={0.3}
                  />
                  <Radar
                    name={lastCompared2.address}
                    dataKey={lastCompared2.propertyId}
                    stroke="#673AB7"
                    fill="#673AB7"
                    fillOpacity={0.3}
                  />
                  <Legend
                    verticalAlign="top"
                    align="center"
                    iconType="circle"
                    wrapperStyle={{ fontSize: '14px', marginTop: '-10px' }}
                  />
                </RadarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        <div className="liked-region-box">
          <h4>찜한 매물</h4>
          <div className="liked-list-area">
            <ul>
              {likedProperties.length > 0 ? (
                likedProperties.map((property) => {
                  const isSelected =
                    selected1?.propertyId === property.propertyId ||
                    selected2?.propertyId === property.propertyId;
                  return (
                    <li
                      key={property.propertyId}
                      className={`property-item ${isSelected ? "selected-region" : ""}`}
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
                      <div className="property-image">
                        {/* property.imageUrl이 없을 경우 기본 이미지를 넣을 수 있음 */}
                        <img src={property.imageUrl || defaultImg} alt={property.address} />
                      </div>
                      <div className="property-details">
                        <div className="property-address">{property.address}</div>
                        <div className="property-price">
                          {property.contractType ? `${property.contractType}` : "계약 정보 없음"}
                          &ensp;
                          {property.price ? `${property.price}` : "가격 정보 없음"}
                        </div>
                      </div>
                      <div className="property-selection">
                        {selected1?.propertyId === property.propertyId
                          ? "①"
                          : selected2?.propertyId === property.propertyId
                          ? "②"
                          : ""}
                      </div>
                    </li>
                  );
                })
              ) : (
                <li>찜한 매물이 없습니다.</li>
              )}
            </ul>
          </div>
        </div>
      </div>

      {lastCompared1 && lastCompared2 && propertyCompareData && (
        <div className="summary-box">
          <div className="summary-box-header">
            <img src={zeepai} alt="ai_image" className="zeepai_summary_image" />
            <p className="summary-box-title">ZEEPSEEK AI의 매물 비교 요약</p>
          </div>
          <p>{propertyCompareData}</p>
        </div>
      )}
    </div>
  );
};

export default EstateCompare;
