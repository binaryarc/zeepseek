import React, { useState } from "react";
import title from "../../assets/logo/zeeptitle.png";
import { useNavigate } from "react-router-dom";
import "./Navbar.css";
import { FaRegUserCircle } from "react-icons/fa"; // 사람 아이콘
import { logoutOAuth } from "../api/authApi";
import { useSelector } from "react-redux";
// import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { logout } from "../../store/slices/authSlice";
// import { setSelectedRoomType } from "../../store/slices/roomListSlice";

function Navbar() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [showDropdown, setShowDropdown] = useState(false);
  const user = useSelector((state) => state.auth.user)
  const nickname = user?.nickname || '로그인 유저';
  const accessToken = useSelector((state) => state.auth.accessToken);
  const isLoggedIn = !!accessToken;

  const handleToggleDropdown = () => {
    setShowDropdown((prev) => !prev);
  };

  const handleMenuClick = (path) => {
    setShowDropdown(false);
    navigate(path);
  };

  const handleLogout = async () => {
    await logoutOAuth(accessToken);      // 백엔드에 로그아웃 요청
    dispatch(logout());                  // Redux 상태 초기화
    navigate("/main");                 // 로그인 페이지로 이동 (선택)
  };


  // const handleClickRecommendButton = () => {
  //   if (!user) {
  //     alert("로그인 후 이용해주세요.");
  //     return;
  //   }
  //   dispatch(setSelectedRoomType("AI 추천")); // ✅ Redux에 탭 상태 저장
  //   navigate("/map");
  // };

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
            <span onClick={() => navigate("/compare")}>동네/매물 비교</span>
          </div>
          <div className="nav-user-area" onClick={handleToggleDropdown}>
            <FaRegUserCircle size={22} style={{ marginRight: "6px" }} />
            <span className="nav-nickname">{nickname}님</span>
            {showDropdown && (
              <div className="nav-dropdown">
                <div onClick={() => handleMenuClick("/mypage")}>마이페이지</div>
                <div onClick={() => handleMenuClick("/zzim")}>
                  찜한 매물
                </div>
                {/* <div onClick={() => handleMenuClick("/profile")}>
                  내 정보 수정
                </div> */}
                <div onClick={handleLogout}>로그아웃</div>
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
