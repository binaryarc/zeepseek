import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { setSelectedRoomType } from "../../store/slices/roomListSlice";
import React from "react";
import Navbar from "../../common/navbar/Navbar";
import zeepseek from "../../assets/logo/zeep_login.png";
import compare_estate from "../../assets/images/main_png/compare_estate.png";
import recommend_estate from "../../assets/images/main_png/recommend_estate.png";
import oneroom from "../../assets/images/main_png/oneroom.png";
import officetel from "../../assets/images/main_png/officetel.png";
import villa from "../../assets/images/main_png/villa.png";
import Searchbar from "../../common/searchbar/SearchBar";
import "./MainPage.css";
import MainSearchbar from "../../common/searchbar/MainSearchBar";
import { useSelector } from "react-redux";
import MainListingSection from "./recommend/MainListingSection";
import SurveyPopup from "../../common/component/SurveyPopup";
import { useEffect, useState } from "react";
import { FaChevronDown } from "react-icons/fa"; // 위에서 import 추가
import Top5ListingSection from "./recommend/Top5ListingSection";

function MainPage() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const user = useSelector((state) => state.auth.user); // 🔹 로그인 여부 확인
  const [showSurvey, setShowSurvey] = useState(false);

  useEffect(() => {
    if (user?.isFirst === 1) {
      setShowSurvey(true);
    }
  }, [user]);
  
  const [showArrow, setShowArrow] = useState(true);

  const handleScrollToBottom = () => {
    window.scrollTo({
      top: document.body.scrollHeight,
      behavior: "smooth",
    });
  };

  useEffect(() => {
    const handleScroll = () => {
      const listingSection = document.getElementById("listing-section");
      if (!listingSection) return;
  
      const rect = listingSection.getBoundingClientRect();
      setShowArrow(() => {
        const threshold = window.innerHeight * 0.8;
      
        // 추천 섹션이 거의 화면 아래쪽에 위치할 경우 숨김
        if (rect.top < threshold && rect.bottom > 0) return false;
      
        // 추천 섹션이 화면 위/아래로 벗어난 경우 표시
        return true;
      });
    };
  
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const handleMoveToMap = (roomType) => {
    dispatch(setSelectedRoomType(roomType));
    navigate("/map");
  };

  // 🔹 로그인 필요 알림 함수
  const handleProtectedMove = (path) => {
    if (!user) {
      alert("로그인 후 이용해주세요.");
      return;
    } else {
      navigate(path);
    }
  };



  const handleClickRecommendButton = () => {
    if (!user) {
      alert("로그인 후 이용해주세요.");
      return;
    }
    dispatch(setSelectedRoomType("AI 추천")); // ✅ Redux에 탭 상태 저장
    navigate("/map");
  };

  return (
    <div className="main-container">
      <Navbar />
      {showSurvey && <SurveyPopup onClose={() => setShowSurvey(false)} />}
      <img src={zeepseek} alt="zeepseek 로고" className="main-logo" />
      {/* 메인 헤더 */}
      <header className="main-header">
        <MainSearchbar />
      </header>

      {/* 버튼 섹션 */}

      <section className="main-button-section-top">
        <div
          className="main-button-top"
          onClick={() => handleMoveToMap("원룸/투룸")}
        >
          <p className="main-button-text">원룸 / 투룸</p>
          <img src={oneroom} alt="원룸" className="main-png-top" />
        </div>
        <div
          className="main-button-top"
          onClick={() => handleMoveToMap("오피스텔")}
        >
          <p className="main-button-text">오피스텔</p>
          <img src={officetel} alt="오피스텔" className="main-png-top" />
        </div>
        <div
          className="main-button-top"
          onClick={() => handleMoveToMap("주택/빌라")}
        >
          <p className="main-button-text">주택 / 빌라</p>
          <img src={villa} alt="주택/빌라" className="main-png-top" />
        </div>
      </section>

      <section className="main-button-section-bottom">
        <div
          className="main-button-bottom"
          onClick={() => handleClickRecommendButton()}
        >
          <p className="main-button-text">매물 추천 받기</p>
          <img
            src={recommend_estate}
            alt="매물 추천"
            className="main-png-bottom"
          />
        </div>
        <div
          className="main-button-bottom"
          onClick={() => handleProtectedMove("/compare")}
        >
          <p className="main-button-text">동네 비교</p>
          <img
            src={compare_estate}
            alt="동네 비교"
            className="main-png-bottom"
          />
        </div>
      </section>

      {/* 매물 추천 섹션 */}
      <div id="listing-section">
        <MainListingSection />
      </div>

      {/* Top5 매물 섹션 */}
      <div id="listing-section">
        <Top5ListingSection />
      </div>

      {showArrow && (
        <div className="double-arrow" onClick={handleScrollToBottom}>
          <FaChevronDown className="arrow-icon" />
          {/* <FaChevronDown className="arrow-icon" /> */}
        </div>
      )}
    </div>
  );
}

export default MainPage;
