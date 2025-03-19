// import { useNavigate } from "react-router-dom";
import React from "react";
import title from "../../assets/logo/zeeptitle.png"
// import zeepseek from "../../assets/logo/zeepseek.png";
import "./MainPage.css";

function MainPage() {
//   const navigate = useNavigate();

  return (
    <div className="main-container">
      {/* 네비게이션 바 */}
      <nav className="main-navbar">
        <img
          src={title}
          alt="zeepseek 로고"
          className="main-logo"
        />
        <button className="main-login-btn">로그인</button>
      </nav>

      {/* 메인 헤더 */}
      <header className="main-header">
        
        <input
          type="text"
          placeholder="지역, 단지, 매물번호를 검색하세요!"
          className="main-search-bar"
        />
      </header>

      {/* 버튼 섹션 */}
      <section className="main-button-section">
        <div className="main-button">원룸 / 투룸</div>
        <div className="main-button">오피스텔</div>
        <div className="main-button">주택 / 빌라</div>
        <div className="main-button">매물 추천 받기</div>
        <div className="main-button">동네 비교</div>
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
