import React, { useState } from "react";
import "./RoomList.css";
import AiRecommend from "./ai_recommend/AiRecommend";
import { useDispatch, useSelector } from "react-redux";
import { setSelectedPropertyId } from "../../../store/slices/roomListSlice";
import defaultImage from "../../../assets/logo/192image.png";


const RoomList = () => {
  const [selectedTab, setSelectedTab] = useState("원룸/투룸");
  const dispatch = useDispatch();
  const handleTabClick = (tab) => {
    setSelectedTab(tab);
  };

  // ✅ Redux 상태에서 매물 리스트, 로딩 상태 가져오기
  const { rooms, loading, keyword, selectedPropertyId } = useSelector((state) => state.roomList);
  
  return (
    <div className="room-list">
      <nav className="room-type">
        {["원룸/투룸", "오피스텔", "주택/빌라", "AI 추천"].map((tab) => (
          <span
            key={tab}
            className={selectedTab === tab ? "active-tab" : ""}
            onClick={() => handleTabClick(tab)}
          >
            {tab}
          </span>
        ))}
      </nav>

      {selectedTab === "AI 추천" ? (
        <AiRecommend />
      ) : loading ? (
        <div className="loading-message">🔄 매물 불러오는 중...</div>
      ) : rooms.length === 0 ? (
        <div className="no-result-message">
          ❗ "{keyword}"에 대한 매물이 없습니다.
        </div>
      ) : (
        rooms.map((room) => (
          <div
          key={room.propertyId}
           className={`room-item ${selectedPropertyId === room.propertyId ? "selected" : ""}`}
          onClick={() => {
            if (room.propertyId === selectedPropertyId) {
              dispatch(setSelectedPropertyId(null)); // 같은 매물 클릭 시 닫기
            } else {
              dispatch(setSelectedPropertyId(room.propertyId)); // 새로운 매물 선택
            }
          }}          
        >
            <img src={room.imageUrl || defaultImage} alt="매물 이미지" />
            <div>
              <p className="room-title">
                {room.contractType} {room.price}
              </p>
              <p className="room-description">{room.address}</p>
            </div>
          </div>
          ))
          
        )
        }
        
    </div>
    
  );
};

export default RoomList;
