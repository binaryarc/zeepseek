import { useNavigate } from "react-router-dom";
import React from "react";
import Navbar from "../../common/navbar/Navbar";
import zeepseek from "../../assets/logo/zeep_login.png";
import compare_estate from "../../assets/images/main_png/compare_estate.png";
import recommend_estate from "../../assets/images/main_png/recommend_estate.png";
import oneroom from "../../assets/images/main_png/oneroom.png";
import officetel from "../../assets/images/main_png/officetel.png";
import villa from "../../assets/images/main_png/villa.png";
import "./MainPage.css";

function MainPage() {
  const navigate = useNavigate();

  return (
    <div className="main-container">
      <Navbar />
      <img src={zeepseek} alt="zeepseek 로고" className="main-logo" />
      {/* 메인 헤더 */}
      <header className="main-header">
        <input
          type="text"
          placeholder="지역, 단지, 매물번호를 검색하세요!"
          className="main-search-bar"
        />
      </header>

      {/* 버튼 섹션 */}

      <section className="main-button-section-top">
        <div className="main-button-top" onClick={() => navigate("/map")}>
          <p className="main-button-text">원룸 / 투룸</p>
          <img src={oneroom} alt="원룸" className="main-png-top" />
        </div>
        <div className="main-button-top" onClick={() => navigate("/map")}>
          <p className="main-button-text">오피스텔</p>
          <img src={officetel} alt="오피스텔" className="main-png-top" />
        </div>
        <div className="main-button-top" onClick={() => navigate("/map")}>
          <p className="main-button-text">주택 / 빌라</p>
          <img src={villa} alt="주택/빌라" className="main-png-top" />
        </div>
      </section>

      <section className="main-button-section-bottom">
        <div className="main-button-bottom" onClick={() => navigate("/map")}>
          <p className="main-button-text">매물 추천 받기</p>
          <img
            src={recommend_estate}
            alt="매물 추천"
            className="main-png-bottom"
          />
        </div>
        <div className="main-button-bottom">
          <p className="main-button-text">동네 비교</p>
          <img
            src={compare_estate}
            alt="동네 비교"
            className="main-png-bottom"
          />
        </div>
      </section>

      {/* 추천 매물 섹션 */}
      <section className="main-listing-section">
        <h2>ZEEPSEEK AI가 추천하는 매물</h2>
        <p>실시간 인기 매물🔥</p>
        <div className="main-listing-container">
          <div className="main-listing-item">매물 1</div>
          <div className="main-listing-item">매물 2</div>
          <div className="main-listing-item">매물 3</div>
        </div>
      </section>
    </div>
  );
}

export default MainPage;
