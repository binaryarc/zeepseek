import React, { useState } from "react";
import title from "../../assets/logo/zeeptitle.png";
import { useNavigate } from "react-router-dom";
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

function Searchbar() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const accessToken = useSelector((state) => state.auth.accessToken);
  const isLoggedIn = !!accessToken;
  const [showDropdown, setShowDropdown] = useState(false);
  const [searchText, setSearchText] = useState("");
  const roomType = useSelector((state) => state.roomList.selectedRoomType);
  const nickname = "크롤링하는 크롱님";

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

  const handleSearch = async () => {
    if (!searchText.trim()) return;

    try {
      const res = await searchProperties(searchText);
      console.log("검색결과", res);
      const properties = res?.properties || [];

      if (properties.length > 0) {
        const first = properties[0];
        const geocoder = new window.kakao.maps.services.Geocoder();

        geocoder.addressSearch(first.address, (result, status) => {
          if (status === window.kakao.maps.services.Status.OK) {
            const { x, y } = result[0];
            const latLng = new window.kakao.maps.LatLng(y, x);
            const map = window.map;
        
            if (map) {
              // ✅ 검색 이동 전 기존 idle 이벤트 제거 (중복 fetch 방지용)
              if (window._idleHandler) {
                window.kakao.maps.event.removeListener(map, "idle", window._idleHandler);
              }
        
              // ✅ 검색 이동 중 플래그 ON
              window.isMovingBySearch = true;
        
              // 🔍 검색어로 동/구 판별 (정확도 높음)
              const isGuOnlySearch = searchText.trim().endsWith("구");
              const level = isGuOnlySearch ? 6 : 4;
              
              map.setLevel(level);
              map.setCenter(latLng);
        
              // ✅ 약간 delay 후 idle 재등록 및 트리거
              setTimeout(() => {
                if (window._idleHandler) {
                  window.kakao.maps.event.addListener(map, "idle", window._idleHandler);
                }
                window.kakao.maps.event.trigger(map, "idle");
              }, 300); // 지도 이동 후 안정화 시간 확보
        
              // ✅ 검색 이동 flag 해제 (조금 더 늦게)
              setTimeout(() => {
                window.isMovingBySearch = false;
              }, 1000);
            }
          }
        });

        // 현재 동 코드를 제거하여 다음 지도 idle 시에 다시 요청될 수 있게 함
        dispatch(setCurrentDongId(null));

        dispatch(
          setCurrentGuAndDongName({
            guName: first.guName,
            dongName: first.dongName,
          })
        );

        // 검색 결과를 rooms에 반영 (덮어쓰기)
        dispatch(fetchRoomList({ keyword: searchText, filter: roomType }));

        dispatch(setSearchLock(true)); // 🔐 검색으로 인해 이동 발생
        console.log(first.guName, first.dongName);
      } else {
        alert("검색 결과가 없습니다.");
      }
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
