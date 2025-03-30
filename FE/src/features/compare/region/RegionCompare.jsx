import React, { useEffect, useState } from 'react';
import './RegionCompare.css';
import { fetchDongDetail, fetchRegionSummary } from '../../../common/api/api';

function RegionCompare() {
  const [selectedRegion1, setSelectedRegion1] = useState(null);
  const [selectedRegion2, setSelectedRegion2] = useState(null);
  const [regionScores, setRegionScores] = useState({});
  const [summary, setSummary] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [searchText, setSearchText] = useState('');

  const dummyLikedRegions = ['서초구 반포동', '동작구 사당동', '강남구 역삼동', '은평구 신사동'];
  const filteredRegions = dummyLikedRegions.filter((region) => region.includes(searchText));  //


  useEffect(() => {
    const fetchCompareData = async () => {
      if (!selectedRegion1 || !selectedRegion2) return;
      setIsLoading(true);
      try {
        // Promise.all([...]) : 배열 구조 분해 할당이라네요ㅎㅎ
        // 여러 개 비동기 작업을 동시에 실행하고, 모든 Promise가 완료될 때까지 기다렸다가 각 결과를 하나의 배열로 반환
        const [data1, data2] = await Promise.all([
          fetchDongDetail(11620685),
          fetchDongDetail(11680510),
        ]);
        setRegionScores({ [selectedRegion1]: data1, [selectedRegion2]: data2 });

        const summaryResult = await fetchRegionSummary(selectedRegion1, selectedRegion2);
        setSummary(summaryResult?.result);
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

  return (
    <div className="region-compare-total-container">
      <div className="region-compare-wrapper">
        <div className="region-compare-container">
          <div className="region-input-row">
            <div className="region-input-wrapper">
              <input
                type="text"
                placeholder="첫 번째 동네 입력"
                value={selectedRegion1 || ''}
                onChange={(e) => setSelectedRegion1(e.target.value)}
              />
              {selectedRegion1 && (
                <button className="region-clear-button" onClick={() => setSelectedRegion1(null)}>❌</button>
              )}
            </div>
            <div className="region-input-wrapper">
              <input
                type="text"
                placeholder="두 번째 동네 입력"
                value={selectedRegion2 || ''}
                onChange={(e) => setSelectedRegion2(e.target.value)}
              />
              {selectedRegion2 && (
                <button className="region-clear-button" onClick={() => setSelectedRegion2(null)}>❌</button>
              )}
            </div>
          </div>

          {!isLoading && selectedRegion1 && selectedRegion2 && (
            <div className="compare-table">
              <div className="table-header">
                <div className="header-cell label-cell"></div>
                <div className="header-cell">{selectedRegion1}</div>
                <div className="header-cell">{selectedRegion2}</div>
              </div>
              {scoreLabels.map(({ label, key }) => (
                <div className="table-row" key={key}>
                  <div className="label-cell">{label}</div>
                  <div className="bar-cell">
                    <div className="score-bar left" style={{ width: `${regionScores[selectedRegion1]?.[key]}%` }}></div>
                    <div className="score-bar remain" style={{ width: `${100 - regionScores[selectedRegion1]?.[key]}%` }}></div>
                  </div>
                  <div className="bar-cell">
                    <div className="score-bar left" style={{ width: `${regionScores[selectedRegion2]?.[key]}%` }}></div>
                    <div className="score-bar remain" style={{ width: `${100 - regionScores[selectedRegion2]?.[key]}%` }}></div>
                  </div>
                </div>
              ))}
            </div>
            )}
        </div>

        <div className="liked-region-box">
          <h4>찜한 동네</h4>
          <input
              className="search-input"
              type="text"
              placeholder="동네 검색하기"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
          />
          <ul>
            {filteredRegions.map((region) => {
              const isSelected = region === selectedRegion1 || region === selectedRegion2;
              return (
                <li
                  key={region}
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
                  {region} {region === selectedRegion1 ? '①' : region === selectedRegion2 ? '②' : ''}
                </li>
              );
            })}
          </ul>
        </div>
      </div>
      <div className="region-ai-summary-container">
        {summary && (
          <div className="summary-box">
            <h3>ZEEPSEEK AI의 동네 비교 요약</h3>
            <p>{summary}</p>
          </div>
        )}
      </div>
    </div>

  );
}

export default RegionCompare;
