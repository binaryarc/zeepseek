import React, { useState } from "react";
import title from "../../assets/logo/zeeptitle.png";
import { useNavigate, useLocation } from "react-router-dom";
import "./SearchBar.css";
import { FaRegUserCircle } from "react-icons/fa"; // 사람 아이콘
import { FiSearch } from "react-icons/fi"; // 검색 아이콘
import { useDispatch, useSelector } from "react-redux";
import {
  setCurrentDongId,
  setSearchLock,
  fetchRoomList,
  setCurrentGuAndDongName,
} from "../../store/slices/roomListSlice";
import { searchProperties } from "../../common/api/api";
import { logoutOAuth } from "../../common/api/authApi";
import { logout } from "../../store/slices/authSlice";
import { useEffect } from "react";

function Searchbar() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const accessToken = useSelector((state) => state.auth.accessToken);
  const isLoggedIn = !!accessToken;
  const [showDropdown, setShowDropdown] = useState(false);
  const [searchText, setSearchText] = useState("");
  const roomType = useSelector((state) => state.roomList.selectedRoomType);
  const user = useSelector((state) => state.auth.user);
  const nickname = user?.nickname || "로그인 유저";
  const location = useLocation();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const keyword = params.get("keyword");
    if (keyword) {
      setSearchText(keyword); // input 채우기
      handleSearch(keyword); // 검색 실행
    }
  }, []);

  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  };

  const handleToggleDropdown = () => {
    setShowDropdown((prev) => !prev);
  };

  const handleMenuClick = (path) => {
    setShowDropdown(false);
    navigate(path);
  };

  const handleLogout = async () => {
    try {
      await logoutOAuth(accessToken); // 백엔드에 로그아웃 요청
      dispatch(logout()); // Redux 상태 초기화
      navigate("/main"); // 로그인 페이지로 이동 (선택)
    } catch (err) {
      console.error("로그아웃 실패", err);
    }
  };

  const handleSearch = async (externalKeyword) => {
    const keyword = externalKeyword || searchText;
    if (!searchText.trim()) return;

    try {
      const res = await searchProperties(keyword);
      const properties = res?.properties || [];
      if (properties.length === 0) return alert("검색 결과가 없습니다.");

      const first = properties[0];
      const geocoder = new window.kakao.maps.services.Geocoder();

      geocoder.addressSearch(first.address, (result, status) => {
        if (status !== window.kakao.maps.services.Status.OK) return;

        const { x, y } = result[0];
        const latLng = new window.kakao.maps.LatLng(y, x);
        const map = window.map;
        if (!map) return;

        // ✅ 검색 이동 시작
        window.isMovingBySearch = true;

        const isGuOnlySearch = keyword.trim().endsWith("구");
        const level = isGuOnlySearch ? 6 : 4;

        // 상태 갱신
        dispatch(
          setCurrentGuAndDongName({
            guName: first.guName,
            dongName: isGuOnlySearch ? "" : first.dongName,
          })
        );
        dispatch(setCurrentDongId(null)); // 강제 갱신 유도
        dispatch(setSearchLock(true));

        // 지도 이동
        map.setLevel(level);
        map.setCenter(latLng);
      });

      // ✅ 매물 리스트 요청 (검색 기반)
      dispatch(
        fetchRoomList({
          keyword: keyword,
          filter: roomType,
        })
      );
    } catch (err) {
      console.error("검색 실패:", err);
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
          placeholder="지역, 매물번호를 검색하세요!"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          onKeyDown={handleKeyDown} // ✅ 여기 추가
        />
        <button onClick={handleSearch}>
          <FiSearch size={20} />
        </button>
      </div>

      {isLoggedIn ? (
        <div className="nav-right">
          <div className="nav-menu">
            <span onClick={() => "/map"}>지도</span>
            <span onClick={() => navigate("/post-room")}>방 내놓기</span>
            <span onClick={() => navigate("/wishlist")}>찜</span>
          </div>
          <div className="nav-user-area" onClick={handleToggleDropdown}>
            <FaRegUserCircle size={22} style={{ marginRight: "6px" }} />
            <span className="nav-nickname">{nickname}님</span>
            {showDropdown && (
              <div className="nav-dropdown">
                <div onClick={() => handleMenuClick("/mypage")}>마이페이지</div>
                <div onClick={() => handleMenuClick("/wishlist")}>
                  찜한 매물
                </div>
                <div onClick={() => handleMenuClick("/profile")}>
                  내 정보 수정
                </div>
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

export default Searchbar;
