import React, { useState } from "react";
import "./RoomList.css";
import AiRecommend from "./ai_recommend/AiRecommend";
import { useSelector } from "react-redux";

const RoomList = () => {
  const [selectedTab, setSelectedTab] = useState("원룸/투룸");

  const handleTabClick = (tab) => {
    setSelectedTab(tab);
  };

  // ✅ Redux 상태에서 매물 리스트, 로딩 상태 가져오기
  const { rooms, loading, keyword } = useSelector((state) => state.roomList);

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
          <div key={room.propertyId} className="room-item">
            <img src={room.imageUrl} alt="매물 이미지" />
            <div>
              <p className="room-title">
                {room.contractType} {room.price}
              </p>
              <p className="room-description">{room.address}</p>
            </div>
          </div>
          ))
        )}
    </div>
  );
};

export default RoomList;
