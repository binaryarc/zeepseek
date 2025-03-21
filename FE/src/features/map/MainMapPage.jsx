import React from "react";
import Navbar from "../../common/navbar/Navbar"; // 네비게이션 바 컴포넌트
import Map from "./mainmap/map.jsx"; // 지도 컴포넌트
import RoomList from "./roomlist/RoomList"; // 매물 리스트 컴포넌트
import "./MainMapPage.css";

const MainMapPage = () => {
  return (
    <div className="map-page">
      <Navbar />
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
