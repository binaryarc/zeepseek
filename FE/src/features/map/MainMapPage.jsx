import React from "react";
import Map from "./mainmap/Map"; // 지도 컴포넌트
import RoomList from "./roomlist/RoomList"; // 매물 리스트 컴포넌트
import "./MainMapPage.css";
import Searchbar from "../../common/searchbar/SearchBar";
import RoomDetail from "./roomlist/RoomDetail";
import { useSelector } from "react-redux";
import AiSlidePanel from "./AiSlidePanel/AiSlidePanel"
import AIRecommendModal from "./mainmap/AiRecommendModal/AiRecommendModal";
import { useEffect } from "react";

const MainMapPage = () => {
  const selectedPropertyId = useSelector((state) => state.roomList.selectedPropertyId);
  useEffect(() => {
    return () => {
      // 이 컴포넌트가 언마운트될 때 전체 새로고침
      window.location.reload();
    };
  }, []);


  return (
    <div className="map-page">
      <Searchbar />
      <div className="map-content">
      <RoomList />

      <div className="map-container">
    <Map />
    
    {selectedPropertyId && (
      <div className="room-detail-overlay">
        <RoomDetail propertyId={selectedPropertyId} />
      </div>
    )}
  </div>
</div>
</div>
  );
};

export default MainMapPage;
