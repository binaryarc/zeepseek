import React, { useEffect, useState } from 'react';
import './RegionCompare.css';
import { fetchDongDetail, fetchRegionSummary, fetchLikedRegions } from '../../../common/api/api';

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
              <div className="table-header">
                <div className="header-cell label-cell"></div>
                <div className="header-cell">{`${selectedRegion1.guName} ${selectedRegion1.name}`}</div>
                <div className="header-cell">{`${selectedRegion2.guName} ${selectedRegion2.name}`}</div>
              </div>
              {scoreLabels.map(({ label, key }) => (
                <div className="table-row" key={key}>
                  <div className="label-cell">{label}</div>
                  <div className="bar-cell">
                    <div className="score-bar left" style={{ width: `${regionScores[selectedRegion1.dongId]?.[key]}%` }}></div>
                    <div className="score-bar remain" style={{ width: `${100 - regionScores[selectedRegion1.dongId]?.[key]}%` }}></div>
                  </div>
                  <div className="bar-cell">
                    <div className="score-bar left" style={{ width: `${regionScores[selectedRegion2.dongId]?.[key]}%` }}></div>
                    <div className="score-bar remain" style={{ width: `${100 - regionScores[selectedRegion2.dongId]?.[key]}%` }}></div>
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
