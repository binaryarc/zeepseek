import React, { useState } from "react";
import DaumPostcode from "react-daum-postcode";
import { useSelector, useDispatch } from "react-redux";
import { postSurvey, patchSurvey } from "../../common/api/api";
import { setUser } from "../../store/slices/authSlice";
import "./SurveyPopup.css";
import AlertModal from "./AlertModal";
import { fetchRandomNickname } from "../../common/api/api";
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

const SurveyPopup = ({ onClose, initialData = {}, mode = "first" }) => {
  const [gender, setGender] = useState(
    initialData.gender === 1 ? "남자" : initialData.gender === 0 ? "여자" : ""
  );
  const [age, setAge] = useState(
    initialData.age ? String(initialData.age) : ""
  );
  const [location, setLocation] = useState(initialData.location || "");
  const [selectedConsiders, setSelectedConsiders] = useState(
    initialData.preferences?.map((pref) => {
      const map = {
        safe: "안전",
        convenience: "편의",
        leisure: "여가",
        transport: "대중교통",
        restaurant: "식당",
        cafe: "카페",
        health: "보건",
        chicken: "치킨집",
      };
      return map[pref];
    }) || []
  );
  const [isPostcodeOpen, setIsPostcodeOpen] = useState(false);
  const dispatch = useDispatch();
  const accessToken = useSelector((state) => state.auth.accessToken);
  const user = useSelector((state) => state.auth.user);
  const [nickname, setNickname] = useState(user?.nickname || "");

  const isFormValid = gender && age && location && selectedConsiders.length > 0;
  const [showAlert, setShowAlert] = useState(false);
  const handleAddressSelect = (data) => {
    setLocation(data.address);
    setIsPostcodeOpen(false);
  };

  const toggleConsideration = (item) => {
    if (selectedConsiders.includes(item)) {
      setSelectedConsiders((prev) => prev.filter((c) => c !== item));
    } else if (selectedConsiders.length < 3) {
      setSelectedConsiders((prev) => [...prev, item]);
    }
  };

  const handleRandomNickname = async () => {
    try {
      const random = await fetchRandomNickname();
      setNickname(random);
      dispatch(setUser({ ...user, nickname: random })); // 🧠 Redux에도 반영
    } catch {
      alert("랜덤 닉네임을 가져오지 못했어요.");
    }
  };

  const handleSubmit = async () => {
    if (!gender || !age || !location) {
      return alert("성별, 나이, 위치를 모두 입력해주세요.");
    }
    if (selectedConsiders.length === 0) {
      return alert("최소 1개의 고려사항을 선택해주세요.");
    }
    if (!location.startsWith("서울")) {
      return alert("서울지역만 입력 가능합니다.");
    }

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
      location,
      preferences: selectedConsiders
        .map((item) => preferenceMap[item])
        .filter(Boolean),
      nickname: nickname,
    };

    try {
      let response;
      if (mode === "edit") {
        // 수정일 때는 patch 요청 (auth/{user.idx})
        response = await patchSurvey(user.idx, surveyData, accessToken);
      } else {
        // 최초 입력일 때는 post 요청
        response = await postSurvey(surveyData, accessToken);
      }

      if (response.success) {
        dispatch(setUser(response.data));
        setShowAlert(true)
      } else {
        alert("설문 제출에 실패했습니다.");
      }
    } catch {
      alert("서버와의 연결에 실패했습니다.");
    }
  };

  return (
    <div className="survey-overlay">
      <div className="survey-box">
        <h2>{mode === "edit" ? "정보를 수정해볼까요?" : "반가워요!"}</h2>
        <p>
          {mode === "edit"
            ? "회원님의 정보에 맞게 집을 추천해드릴게요!"
            : "정보를 입력하면 딱 맞는 매물을 추천해드릴게요😉"}
        </p>
        <label>닉네임</label>
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <input
            type="text"
            value={nickname}
            onChange={(e) => {
              setNickname(e.target.value);
              dispatch(setUser({ ...user, nickname: e.target.value }));
            }}
            placeholder="닉네임을 입력하세요"
            className="nickname-input"
          />

          <button
            type="button"
            onClick={handleRandomNickname}
            className="surveyserachbtn"
          >
            랜덤 생성
          </button>
        </div>

        <label>성별</label>
        <select value={gender} onChange={(e) => setGender(e.target.value)}>
          <option value="">선택</option>
          {GENDERS.map((g, i) => (
            <option key={i} value={g}>
              {g}
            </option>
          ))}
        </select>

        <label>나이</label>
        <select value={age} onChange={(e) => setAge(e.target.value)}>
          <option value="">선택</option>
          {[...Array(40)].map((_, i) => (
            <option key={i} value={i + 18}>
              {i + 18}세
            </option>
          ))}
        </select>

        <label>기준 위치(직장/학교)</label>
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
              fontSize: "1.1rem",
            }}
          />
          <button
            onClick={() => setIsPostcodeOpen(true)}
            className="surveyserachbtn"
          >
            검색
          </button>
        </div>

        {isPostcodeOpen && (
          <div className="postcode-modal">
            <DaumPostcode onComplete={handleAddressSelect} />
            <button onClick={() => setIsPostcodeOpen(false)}>닫기</button>
          </div>
        )}

        <label>매물 고려사항 (최대 3개)</label>
        <div className="consideration-grid">
          {CONSIDERATIONS.map((item, i) => (
            <label key={i} className="checkbox-item">
              <input
                type="checkbox"
                checked={selectedConsiders.includes(item)}
                onChange={() => toggleConsideration(item)}
              />
              {item}
            </label>
          ))}
        </div>

        <button
          className={`start-button ${!isFormValid ? "disabled" : ""}`}
          onClick={handleSubmit}
          disabled={!isFormValid}
        >
          {mode === "edit" ? "수정하기" : "시작하기"}
        </button>

        {mode === "edit" && (
          <button className="survey-close-btn" onClick={onClose}>
            ✖
          </button>
        )}
      </div>
      {showAlert && (
        <AlertModal
          message={
            mode === "edit"
              ? "정보 수정이 완료되었습니다!"
              : "설문이 완료되었습니다"
          }
          buttonText="확인"
          onClose={() => {
            setShowAlert(false);
            onClose(); // ✅ SurveyPopup 닫기까지
          }}
        />
      )}
    </div>
  );
};

export default SurveyPopup;
