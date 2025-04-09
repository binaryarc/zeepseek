import React, { useState } from "react";
import "./FirstLoginSurvey.css";
import DaumPostcode from "react-daum-postcode";
import { useSelector, useDispatch } from "react-redux";
import { postSurvey } from "../../../common/api/api"; // 경로 맞춰주세요
import { setUser } from "../../../store/slices/authSlice";
import { useNavigate } from "react-router-dom";

const GENDERS = ["남자", "여자"];
const CONSIDERATIONS = [
  "안전",
  "편의",
  "여가",
  "대중교통",
  "식당",
  "카페",
  "보건",
  "치킨집",
];

const FirstLoginSurvey = () => {
  const navigate = useNavigate();
  const [gender, setGender] = useState("");
  const dispatch = useDispatch();
  const [age, setAge] = useState("");
  const [location, setLocation] = useState("");
  const [selectedConsiders, setSelectedConsiders] = useState([]);
  const [isPostcodeOpen, setIsPostcodeOpen] = useState(false);
  const accessToken = useSelector((state) => state.auth.accessToken);
  const user = useSelector((state) => state.auth.user);
  const nickname = user?.nickname || "로그인 유저";
  const handleAddressSelect = (data) => {
    console.log(data);
    // Modified: 주소가 "서울"로 시작하지 않으면 경고 메시지를 띄웁니다.
    if (!data.address.startsWith("서울")) {
      alert("서울지역만 서비스 중입니다!");
      return;
    }
    setLocation(data.address); // 또는 data.sigungu + data.bname 등
    setIsPostcodeOpen(false);
  };

  const toggleConsideration = (item) => {
    if (selectedConsiders.includes(item)) {
      setSelectedConsiders((prev) => prev.filter((c) => c !== item));
    } else if (selectedConsiders.length < 3) {
      setSelectedConsiders((prev) => [...prev, item]);
    }
  };

  const handleSubmit = async () => {
    if (!gender || !age || !location) {
      alert("성별, 나이, 기준 위치를 모두 선택해주세요.");
      return;
    }
    
    // location이 서울로 시작하는지 확인 (서울이 아닐 경우 알림 후 전송 중단)
    if (!location.startsWith("서울")) {
      alert("서울지역만 입력 가능합니다.");
      return;
    }
  
    if (selectedConsiders.length === 0) {
      alert("매물 고려사항을 최소 1개 이상 선택해주세요.");
      return;
    }
  
    // 변환 및 포맷 정리
    const genderValue = gender === "남자" ? 1 : 0;
    const ageValue = parseInt(age);
  
    const preferenceMap = {
      안전: "safe",
      편의: "convenience",
      여가: "leisure",
      대중교통: "transport",
      식당: "restaurant",
      카페: "cafe",
      보건: "health",
      치킨집: "chicken",
    };
  
    const surveyData = {
      gender: genderValue,
      age: ageValue,
      location: location,
      nickname: nickname, // nickname 추가
      preferences: selectedConsiders.map((item) => preferenceMap[item] || item),
    };
  
    try {
      console.log("보낼 surveyData:", JSON.stringify(surveyData));
      const response = await postSurvey(surveyData, accessToken);
      console.log("설문 응답 response", response);
      if (response.success) {
        dispatch(setUser(response.data)); // 유저 정보 업데이트
        navigate("/main");
      } else {
        alert("서베이 제출에 실패했습니다.");
      }
    } catch (error) {
      console.error("서베이 제출 실패:", error);
      alert("서버와의 통신에 실패했습니다.");
    }
  };

  return (
    <div className="survey-container">
      <div className="survey-box">
        <h2>{nickname}님, 반가워요!</h2>
        <p>정보를 입력하면 딱 맞는 매물을 추천해드릴게요</p>

        <label>성별:</label>
        <select value={gender} onChange={(e) => setGender(e.target.value)}>
          <option value="">선택</option>
          {GENDERS.map((g, idx) => (
            <option key={idx} value={g}>
              {g}
            </option>
          ))}
        </select>

        <label>나이:</label>
        <select value={age} onChange={(e) => setAge(e.target.value)}>
          <option value="">선택</option>
          {[...Array(40)].map((_, i) => {
            const ageNum = i + 18;
            return (
              <option key={i} value={ageNum}>
                {ageNum}세
              </option>
            );
          })}
        </select>

        <label>기준 위치:</label>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <input
            type="text"
            value={location}
            readOnly
            placeholder="주소를 검색해주세요"
            style={{
              flex: 1,
              padding: "0.5rem",
              border: "1px solid #ccc",
              borderRadius: "0.5rem",
              fontFamily: "KOROAD",
            }}
          />
          <button
            type="button"
            onClick={() => setIsPostcodeOpen(true)}
            style={{
              padding: "0.5rem 0.8rem",
              background: "#f6a94d",
              color: "black",
              border: "none",
              borderRadius: "0.5rem",
              cursor: "pointer",
              fontFamily: "KOROAD",
              fontWeight: "bold",
            }}
          >
            검색
          </button>
        </div>

        {isPostcodeOpen && (
          <div
            style={{
              position: "fixed",
              top: "50%",
              left: "50%",
              transform: "translate(-50%, -50%)",
              zIndex: 1000,
              border: "1px solid #ccc",
              backgroundColor: "#fff",
              boxShadow: "0 0 10px rgba(0,0,0,0.2)",
            }}
          >
            <DaumPostcode onComplete={handleAddressSelect} />
            <button
              onClick={() => setIsPostcodeOpen(false)}
              style={{
                display: "block",
                width: "100%",
                padding: "0.5rem",
                background: "#eee",
                border: "none",
                borderTop: "1px solid #ccc",
                cursor: "pointer",
              }}
            >
              닫기
            </button>
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

        <button className="start-button" onClick={handleSubmit}>
          시작하기
        </button>
      </div>
    </div>
  );
};

export default FirstLoginSurvey;
