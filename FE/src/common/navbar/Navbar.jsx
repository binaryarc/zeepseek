import React, { useState } from "react";
import title from "../../assets/logo/zeeptitle.png";
import { useNavigate } from "react-router-dom";
import "./Navbar.css";
import { FaRegUserCircle } from "react-icons/fa"; // 사람 아이콘

function Navbar() {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(true); // 임시 상태
  const [showDropdown, setShowDropdown] = useState(false);
  const nickname = "크롤링하는 크롱님";

  const handleToggleDropdown = () => {
    setShowDropdown((prev) => !prev);
  };

  const handleMenuClick = (path) => {
    setShowDropdown(false);
    navigate(path);
  };

  return (
    <nav className="nav-navbar">
      <img
        src={title}
        alt="zeepseek 로고"
        className="nav-logo"
        onClick={() => navigate("/main")}
      />

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
                <div onClick={() => handleMenuClick("/wishlist")}>
                  찜한 매물
                </div>
                <div onClick={() => handleMenuClick("/profile")}>
                  내 정보 수정
                </div>
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

export default Navbar;
