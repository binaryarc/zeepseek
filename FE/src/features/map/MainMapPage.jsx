import React from "react";
import Map from "./mainmap/Map"; // 지도 컴포넌트
import RoomList from "./roomlist/RoomList"; // 매물 리스트 컴포넌트
import "./MainMapPage.css";
import Searchbar from "../../common/searchbar/SearchBar";

const MainMapPage = () => {
  return (
    <div className="map-page">
      <Searchbar />
      <div className="map-content">
        {/* 왼쪽 매물 리스트 */}
        <RoomList />

        {/* 오른쪽 지도 */}
        <div className="map-container">
          <Map />
        </div>
      </div>
    </div>
  );
};

export default MainMapPage;
