import React, { useState } from 'react';
import './FirstLoginSurvey.css';
import DaumPostcode from 'react-daum-postcode';
import KOROAD from '../../../assets/font/KOROAD_Medium.ttf';

const GENDERS = ['남자', '여자'];
const LOCATIONS = ['멀티캠퍼스 역삼', '강남역', '신촌역', '건대입구역'];
const CONSIDERATIONS = [
  '안전', '편의', '여가', '대중교통', '식당', '카페', '보건', '치킨집'
];

const FirstLoginSurvey = ({ onStart }) => {
  const [gender, setGender] = useState('');
  const [age, setAge] = useState('');
  const [location, setLocation] = useState('');
  const [selectedConsiders, setSelectedConsiders] = useState([]);
  const [isPostcodeOpen, setIsPostcodeOpen] = useState(false);
  
  const handleAddressSelect = (data) => {
    setLocation(data.address); // 또는 data.sigungu + data.bname 등
    setIsPostcodeOpen(false);
  };

  const toggleConsideration = (item) => {
    if (selectedConsiders.includes(item)) {
      setSelectedConsiders(prev => prev.filter(c => c !== item));
    } else if (selectedConsiders.length < 3) {
      setSelectedConsiders(prev => [...prev, item]);
    }
  };

  const handleSubmit = () => {
    if (!gender || !age || !location) {
        alert('성별, 나이, 기준 위치를 모두 선택해주세요.');
        return;
      }
    
      if (selectedConsiders.length === 0) {
        alert('매물 고려사항을 최소 1개 이상 선택해주세요.');
        return;
      }
    const surveyData = { gender, age, location, preferences: selectedConsiders };
    onStart(surveyData); // 이후 추천 로직으로 넘기기
  };

  return (
    <div className="survey-container">
      <div className="survey-box">
        <h2>싸피님, 반가워요!</h2>
        <p>정보를 입력하면 딱 맞는 매물을 추천해드릴게요</p>

        <label>성별:</label>
        <select value={gender} onChange={(e) => setGender(e.target.value)}>
          <option value="">선택</option>
          {GENDERS.map((g, idx) => (
            <option key={idx} value={g}>{g}</option>
          ))}
        </select>

        <label>나이:</label>
        <select value={age} onChange={(e) => setAge(e.target.value)}>
          <option value="">선택</option>
          {[...Array(40)].map((_, i) => {
            const ageNum = i + 18;
            return <option key={i} value={ageNum}>{ageNum}세</option>;
          })}
        </select>

        <label>기준 위치:</label>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
            <input
                type="text"
                value={location}
                readOnly
                placeholder="주소를 검색해주세요"
                style={{
                flex: 1,
                padding: '0.5rem',
                border: '1px solid #ccc',
                borderRadius: '0.5rem',
                fontFamily: KOROAD,
                }}
            />
            <button
                type="button"
                onClick={() => setIsPostcodeOpen(true)}
                style={{
                padding: '0.5rem 0.8rem',
                background: '#f6a94d',
                color: 'black',
                border: 'none',
                borderRadius: '0.5rem',
                cursor: 'pointer',
                fontFamily: KOROAD,
                fontWeight: 'bold',
                }}
            >
                검색
            </button>
            </div>

            {isPostcodeOpen && (
            <div style={{
                position: 'fixed',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                zIndex: 1000,
                border: '1px solid #ccc',
                backgroundColor: '#fff',
                boxShadow: '0 0 10px rgba(0,0,0,0.2)'
            }}>
                <DaumPostcode onComplete={handleAddressSelect} />
                <button onClick={() => setIsPostcodeOpen(false)} style={{
                display: 'block',
                width: '100%',
                padding: '0.5rem',
                background: '#eee',
                border: 'none',
                borderTop: '1px solid #ccc',
                cursor: 'pointer'
                }}>닫기</button>
            </div>
            )}

        <label>매물 고려사항 (최대 3개 선택 가능):</label>
        <div className="consideration-grid">
          {CONSIDERATIONS.map((item, idx) => (
            <label key={idx} className="checkbox-item">
              <input
                type="checkbox"
                checked={selectedConsiders.includes(item)}
                onChange={() => toggleConsideration(item)}
              />
              {item}
            </label>
          ))}
        </div>

        <button className="start-button" onClick={handleSubmit}>시작하기</button>
      </div>
    </div>
  );
};

export default FirstLoginSurvey;
