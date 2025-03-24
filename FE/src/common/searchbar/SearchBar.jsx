import React, { useState } from "react";
import title from "../../assets/logo/zeeptitle.png";
import { useNavigate } from "react-router-dom";
import "./Searchbar.css";
import { FaRegUserCircle } from "react-icons/fa"; // 사람 아이콘
import { FiSearch } from "react-icons/fi"; // 검색 아이콘

function Searchbar() {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(true); // 임시 상태
  const [showDropdown, setShowDropdown] = useState(false);
  const [searchText, setSearchText] = useState("");
  const nickname = "크롤링하는 크롱님";

  const handleToggleDropdown = () => {
    setShowDropdown((prev) => !prev);
  };

  const handleMenuClick = (path) => {
    setShowDropdown(false);
    navigate(path);
  };

  const handleSearch = () => {
    if (searchText.trim()) {
      navigate(`/search?keyword=${encodeURIComponent(searchText)}`);
    }
  };

  return (
    <nav className="search-navbar">
      <img
        src={title}
        alt="zeepseek 로고"
        className="nav-logo"
        onClick={() => navigate("/main")}
      />

      <div className="nav-searchbox">
        <input
          type="text"
          placeholder="지역, 단지, 매물번호를 검색하세요!"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
        />
        <button onClick={handleSearch}>
          <FiSearch size={20} />
        </button>
      </div>

      {isLoggedIn ? (
        <div className="nav-right">
          <div className="nav-menu">
            <span onClick={() => navigate("/map")}>지도</span>
            <span onClick={() => navigate("/post-room")}>방 내놓기</span>
            <span onClick={() => navigate("/wishlist")}>찜</span>
          </div>
          <div className="nav-user-area" onClick={handleToggleDropdown}>
            <FaRegUserCircle size={22} style={{ marginRight: "6px" }} />
            <span className="nav-nickname">{nickname}</span>
            {showDropdown && (
              <div className="nav-dropdown">
                <div onClick={() => handleMenuClick("/mypage")}>마이페이지</div>
                <div onClick={() => handleMenuClick("/wishlist")}>찜한 매물</div>
                <div onClick={() => handleMenuClick("/profile")}>내 정보 수정</div>
                <div onClick={() => setIsLoggedIn(false)}>로그아웃</div>
              </div>
            )}
          </div>
        </div>
      ) : (
        <button className="nav-login-btn" onClick={() => navigate("/login")}>
          로그인
        </button>
      )}
    </nav>
  );
}

export default Searchbar;
