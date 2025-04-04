import React, { useState } from "react";
import "./MainSearchBar.css";
import { FiSearch } from "react-icons/fi"; // 검색 아이콘
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { setKeyword } from "../../store/slices/roomListSlice"; // ✅ 추가가

function MainSearchbar() {
  const [searchText, setSearchText] = useState("");
  const navigate = useNavigate();

  const dispatch = useDispatch();


  const handleSearch = () => {
    if (!searchText.trim()) return;

    dispatch(setKeyword(searchText)); // ✅ 검색어 Redux에 저장
    navigate("/map"); 
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") handleSearch();
  };

  return (
    <div className="main-searchbox">
      <input
        type="text"
        placeholder="지역, 매물번호를 검색하세요!"
        value={searchText}
        onChange={(e) => setSearchText(e.target.value)}
        onKeyDown={handleKeyDown}
      />
      <FiSearch className="search-icon" onClick={handleSearch} />
    </div>
  );
}

export default MainSearchbar;