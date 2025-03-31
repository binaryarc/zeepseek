import React, { useState } from "react";
import "./MainSearchBar.css";
import { FiSearch } from "react-icons/fi"; // 검색 아이콘
import { useNavigate } from "react-router-dom";

function MainSearchbar() {
  const [searchText, setSearchText] = useState("");
  const navigate = useNavigate();

  const handleSearch = () => {
    if (!searchText.trim()) return;
    const encoded = encodeURIComponent(searchText.trim());
    navigate(`/map?keyword=${encoded}`);
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