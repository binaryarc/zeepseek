import React, { useEffect, useState } from 'react';
import './RegionCompare.css';
import { fetchDongDetail, fetchRegionSummary, fetchLikedRegions } from '../../../common/api/api';
import {
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
  Legend,
  ResponsiveContainer,
} from 'recharts';

function RegionCompare() {
  const [selectedRegion1, setSelectedRegion1] = useState(null);
  const [selectedRegion2, setSelectedRegion2] = useState(null);
  const [regionScores, setRegionScores] = useState({});
  const [summary, setSummary] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [likedRegions, setLikedRegions] = useState([]);

  const userId = 2; // 임시로 userId 2로 설정

  useEffect(() => {
    const loadLikedRegions = async () => {
      try {
        const res = await fetchLikedRegions(userId);
        setLikedRegions(res?.data || []);
        console.log(likedRegions)
      } catch (err) {
        console.error('찜한 동네 불러오기 실패:', err);
      }
    };
    loadLikedRegions();
  }, []);

  const filteredRegions = likedRegions.filter((region) =>
    `${region.guName} ${region.name}`.includes(searchText)
  );

  useEffect(() => {
    const fetchCompareData = async () => {
      if (!selectedRegion1 || !selectedRegion2) return;
      setIsLoading(true);
      try {
        // Promise.all([...]) : 배열 구조 분해 할당이라네요ㅎㅎ
        // 여러 개 비동기 작업을 동시에 실행하고, 모든 Promise가 완료될 때까지 기다렸다가 각 결과를 하나의 배열로 반환
        const [data1, data2] = await Promise.all([
          fetchDongDetail(selectedRegion1.dongId),
          fetchDongDetail(selectedRegion2.dongId),
        ]);
        setRegionScores({ [selectedRegion1.dongId]: data1, [selectedRegion2.dongId]: data2 });

        const summaryResult = await fetchRegionSummary(selectedRegion1.dongId, selectedRegion2.dongId);
        setSummary(summaryResult?.data?.compareSummary);
        console.log("region1", selectedRegion1)
        console.log("region2", selectedRegion2)
        console.log("summaryResult", summaryResult?.data?.compareSummary)
      } catch (err) {
        console.error('비교 데이터 로딩 실패:', err);
      } finally {
        setIsLoading(false);
      }
    };
    fetchCompareData();
  }, [selectedRegion1, selectedRegion2]);

  const scoreLabels = [
    { label: '편의', key: 'convenience' },
    { label: '보건', key: 'health' },
    { label: '여가', key: 'leisure' },
    { label: '안전', key: 'safe' },
    { label: '마트', key: 'mart' },
    { label: '대중교통', key: 'transport' },
    { label: '식당', key: 'restaurant' },
  ];

  const chartData = scoreLabels.map(({ label, key }) => ({
    subject: label,
    [selectedRegion1?.dongId]: regionScores[selectedRegion1?.dongId]?.[key] || 0,
    [selectedRegion2?.dongId]: regionScores[selectedRegion2?.dongId]?.[key] || 0,
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
                placeholder="첫 번째 동네 입력"
                value={selectedRegion1 ? `${selectedRegion1.guName} ${selectedRegion1.name}` : ''}
                readOnly
              />
              {selectedRegion1 && (
                <button className="region-clear-button" onClick={() => setSelectedRegion1(null)}>❌</button>
              )}
            </div>
            <div className="region-input-wrapper">
              <input
                type="text"
                placeholder="두 번째 동네 입력"
                value={selectedRegion2 ? `${selectedRegion2.guName} ${selectedRegion2.name}` : ''}
                readOnly
              />
              {selectedRegion2 && (
                <button className="region-clear-button" onClick={() => setSelectedRegion2(null)}>❌</button>
              )}
            </div>
          </div>

          {!isLoading && selectedRegion1 && selectedRegion2 && (
            <div className="compare-table">
              <ResponsiveContainer width="100%" height={400}>
                <RadarChart outerRadius={130} data={chartData}>
                  <PolarGrid />
                  <PolarAngleAxis
                    dataKey="subject"
                    tickSize={20}
                    tick={{ fontweight: "KOROAD_Bold", fontSize: '1.2rem', fill: '#555', dy: 8 }}
                  />
                  <PolarRadiusAxis angle={30} domain={[0, 100]} />
                  <Radar
                    name={`${selectedRegion1.guName} ${selectedRegion1.name}`}
                    dataKey={selectedRegion1.dongId}
                    stroke="#4CAF50"
                    fill="#4CAF50"
                    fillOpacity={0.3}
                  />
                  <Radar
                    name={`${selectedRegion2.guName} ${selectedRegion2.name}`}
                    dataKey={selectedRegion2.dongId}
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
          <div className="liked-search-box">
            <h4>동네 검색</h4>
            <input
              className="search-input"
              type="text"
              placeholder="동네 검색하기"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </div>

          <div className="liked-list-box">
            <h4 className="liked-list-box-title">찜한 동네</h4>
            <ul>
              {filteredRegions.map((region) => {
                const fullName = `${region.guName} ${region.name}`;
                const isSelected =
                  selectedRegion1?.dongId === region.dongId || selectedRegion2?.dongId === region.dongId;
                return (
                  <li
                    key={region.dongId}
                    className={isSelected ? 'selected-region' : ''}
                    onClick={() => {
                      if (isSelected) return;
                      if (!selectedRegion1) setSelectedRegion1(region);
                      else if (!selectedRegion2) setSelectedRegion2(region);
                      else {
                        setSelectedRegion1(region);
                        setSelectedRegion2(null);
                      }
                    }}
                  >
                    {fullName}{' '}
                    {selectedRegion1?.dongId === region.dongId
                      ? '①'
                      : selectedRegion2?.dongId === region.dongId
                      ? '②'
                      : ''}
                  </li>
                );
              })}
            </ul>
          </div>
</div>

      </div>
      <div className="region-ai-summary-container">
        {summary && (
          <div className="summary-box">
            <p className="summary-box-title">ZEEPSEEK AI의 동네 비교 요약</p>
            <p>{summary}</p>
          </div>
        )}
      </div>
    </div>

  );
}

export default RegionCompare;
