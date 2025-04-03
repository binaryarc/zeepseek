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

const EstateCompare = () => {
  const [likedProperties, setLikedProperties] = useState([]);
  const [selected1, setSelected1] = useState(null);
  const [selected2, setSelected2] = useState(null);
  const [propertyCompareData, setProPertyCompareData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [estateScores, setEstateScores] = useState({});
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

  // useEffect(() => {
  //   const fetchPropertyComparison = async () => {
  //     if (!selected1 || !selected2) return;
  //     setLoading(true);
  //     try {
  //       console.log(selected1.propertyId, selected2.propertyId)
  //       const result = await fetchPropertyCompare(selected1.propertyId, selected2.propertyId);
  //       setProPertyCompareData(result);
  //     } catch (err) {
  //       console.error("매물 비교 실패:", err);
  //     } finally {
  //       setLoading(false);
  //     }
  //   };
  //   fetchPropertyComparison();
  // }, [selected1, selected2]);


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
          setEstateScores({ [selected1.propertyId]: data1, [selected2.propertyId]: data2 });
  
          const summaryResult = await fetchPropertyCompare(selected1.propertyId, selected2.propertyId);
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
    [selected1?.propertyId]: estateScores[selected1?.propertyId]?.[key] || 0,
    [selected2?.propertyId]: estateScores[selected2?.propertyId]?.[key] || 0,
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

          {!loading && selected1 && selected2 && propertyCompareData && (
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

      {propertyCompareData && (
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
