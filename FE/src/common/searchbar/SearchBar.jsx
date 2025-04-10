import React, { useState, useEffect } from "react";
import "./SearchBar.css";
import title from "../../assets/logo/zeeptitle.png";
import { useNavigate } from "react-router-dom";
import { FaRegUserCircle } from "react-icons/fa"; // 사람 아이콘
import { FiSearch } from "react-icons/fi"; // 검색 아이콘
import { useDispatch, useSelector } from "react-redux";
import {
  setCurrentDongId,
  fetchRoomList,
  setCurrentGuAndDongName,
  setKeyword,
} from "../../store/slices/roomListSlice";
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
  const user = useSelector((state) => state.auth.user);
  const keywordFromRedux = useSelector((state) => state.roomList.keyword);
  const mapReady = useSelector((state) => state.roomList.mapReady);
  const { currentDongName } = useSelector((state) => state.roomList);

  useEffect(() => {
    if (keywordFromRedux) {
      if (!mapReady) return;
      setSearchText(keywordFromRedux);
      handleSearch(keywordFromRedux);
    }
  }, [keywordFromRedux, mapReady]);

  const handleKeyDown = (e) => {
    if (e.key === "Enter") {
      dispatch(setKeyword(searchText));
      // handleSearch는 useEffect에서 실행되므로 여기서는 setKeyword만 실행함
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
    await logoutOAuth(accessToken);
    dispatch(logout());
    navigate("/main");
  };

  const handleSearch = async (externalKeyword) => {
    // Modified: externalKeyword가 없거나 빈 문자열이면 currentDongName을 사용
    const keywordInput = externalKeyword || searchText;
    const actualKeyword = keywordInput && keywordInput.trim() !== "" ? keywordInput : currentDongName || "";
    if (!actualKeyword.trim()) return;

    const result = await dispatch(
      fetchRoomList({ keyword: actualKeyword, filter: roomType, userId: user?.idx ?? null })
    );
    const properties = result.payload;
    if (!properties || properties.length === 0) {
      alert(`"${actualKeyword}"에 대한 검색 결과가 없습니다.`);
      // Modified: 검색 결과가 없으면 Redux에 저장된 keyword 초기화
      dispatch(setKeyword(""));
      return;
    }

    const first = properties[0];
    const geocoder = new window.kakao.maps.services.Geocoder();
    geocoder.addressSearch(first.address, (result, status) => {
      if (status !== window.kakao.maps.services.Status.OK) return;
      const { x, y } = result[0];
      const latLng = new window.kakao.maps.LatLng(y, x);
      const map = window.map;
      if (!map) return;
      window.isMovingBySearch = true;
      const isGuOnlySearch = actualKeyword.trim().endsWith("구");
      const level = isGuOnlySearch ? 6 : 4;
      dispatch(
        setCurrentGuAndDongName({
          guName: first.guName,
          dongName: isGuOnlySearch ? "" : first.dongName,
        })
      );
      dispatch(setCurrentDongId(first.dongId));
      map.setLevel(level);
      map.setCenter(latLng);
    });
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
          placeholder="서울 내 지역, 매물번호를 검색하세요!"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          onKeyDown={handleKeyDown}
        />
        {/* Modified: 돋보기 버튼 클릭 시 handleSearch를 호출하도록 변경 */}
        <button onClick={() => handleSearch()}>
          <FiSearch size={20} />
        </button>
      </div>
      {isLoggedIn ? (
        <div className="nav-right">
          <div className="nav-menu">
            <span onClick={() => navigate("/map")}>지도</span>
            <span onClick={() => navigate("/zzim")}>찜</span>
            <span onClick={() => navigate("/compare")}>동네비교</span>
          </div>
          <div className="nav-user-area" onClick={handleToggleDropdown}>
            <FaRegUserCircle size={22} style={{ marginRight: "6px" }} />
            <span className="nav-nickname">{user?.nickname || "로그인 유저"}님</span>
            {showDropdown && (
              <div className="nav-dropdown">
                <div onClick={() => handleMenuClick("/mypage")}>마이페이지</div>
                <div onClick={() => handleMenuClick("/zzim")}>찜한 매물</div>
                <div onClick={() => handleMenuClick("/profile")}>내 정보 수정</div>
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
};

export default Searchbar;
