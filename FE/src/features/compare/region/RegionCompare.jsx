import React, { useEffect, useState } from 'react';
import './RegionCompare.css';
import { fetchRegionScore, fetchRegionSummary } from '../../../common/api/api';

function RegionCompare() {
  const [selectedRegion1, setSelectedRegion1] = useState(null);
  const [selectedRegion2, setSelectedRegion2] = useState(null);
  const [regionScores, setRegionScores] = useState({});
  const [summary, setSummary] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const dummyLikedRegions = ['서초구 반포동', '동작구 사당동', '강남구 역삼동', '은평구 신사동'];

  const handleRegionSelect = async (index, regionName) => {
    if (index === 1) setSelectedRegion1(regionName);
    else setSelectedRegion2(regionName);
  };

  useEffect(() => {
    const fetchCompareData = async () => {
      if (!selectedRegion1 || !selectedRegion2) return;

      setIsLoading(true);
      try {
        const [score1, score2] = await Promise.all([
          fetchRegionScore(selectedRegion1),
          fetchRegionScore(selectedRegion2),
        ]);
        setRegionScores({
          [selectedRegion1]: score1,
          [selectedRegion2]: score2,
        });

        const summaryResult = await fetchRegionSummary(selectedRegion1, selectedRegion2);
        setSummary(summaryResult?.summary || '');
      } catch (err) {
        console.error('비교 데이터 로딩 실패:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchCompareData();
  }, [selectedRegion1, selectedRegion2]);

  const scoreLabels = ['편의', '보건', '여가', '안전', '카페', '대중교통', '식당', '술집'];

  return (
    <div className="region-compare-container">


      <div className="region-select-section">
        <div className="region-input">
          <input
            type="text"
            placeholder="첫 번쩨 동네 입력"
            value={selectedRegion1 || ''}
            onChange={(e) => setSelectedRegion1(e.target.value)}
          />
          <input
            type="text"
            placeholder="두 번쩨 동네 입력"
            value={selectedRegion2 || ''}
            onChange={(e) => setSelectedRegion2(e.target.value)}
          />
        </div>

        <div className="liked-regions">
          <h4>찜한 동네</h4>
          <ul>
            {dummyLikedRegions.map((region) => (
              <li key={region} onClick={() => handleRegionSelect(1, region)}>
                {region}
              </li>
            ))}
          </ul>
        </div>
      </div>

      {isLoading ? (
        <p className="loading-text">동네 정보를 불러오는 중입니다...</p>
      ) : (
        selectedRegion1 &&
        selectedRegion2 &&
        regionScores[selectedRegion1] &&
        regionScores[selectedRegion2] && (
          <>
            <div className="compare-chart">
              {scoreLabels.map((label) => (
                <div className="score-row" key={label}>
                  <span className="score-label">{label}</span>
                  <div className="bar-wrapper">
                    <div
                      className="score-bar left"
                      style={{ width: `${regionScores[selectedRegion1][label]}%` }}
                    ></div>
                    <div
                      className="score-bar right"
                      style={{ width: `${regionScores[selectedRegion2][label]}%` }}
                    ></div>
                  </div>
                </div>
              ))}
            </div>

            <div className="summary-box">
              <h3>ZEEPSEEK AI의 동네 비교 요약</h3>
              <p>{summary}</p>
            </div>
          </>
        )
      )}
    </div>
  );
}

export default RegionCompare;
