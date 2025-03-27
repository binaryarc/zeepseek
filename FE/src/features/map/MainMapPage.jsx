import React, { useState} from "react";
import Map from "./mainmap/Map"; // 지도 컴포넌트
import RoomList from "./roomlist/RoomList"; // 매물 리스트 컴포넌트
import "./MainMapPage.css";
import Searchbar from "../../common/searchbar/SearchBar";
import RoomDetail from "./roomlist/RoomDetail";
import { useSelector } from "react-redux";
import AiSlidePanel from "./AiSlidePanel/AiSlidePanel"

const MainMapPage = () => {
  const selectedPropertyId = useSelector((state) => state.roomList.selectedPropertyId);

  const [isAiPanelOpen, setIsAiPanelOpen] = useState(false);


  return (
    <div className="map-page">
      <Searchbar />
      <div className="map-content">
      <RoomList />
      <button className="ai-slide-button" onClick={() => setIsAiPanelOpen(true)}>AI 추천</button>

      {isAiPanelOpen && (
        <AiSlidePanel isOpen={isAiPanelOpen} onClose={() => setIsAiPanelOpen(false)} />
      )}



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
