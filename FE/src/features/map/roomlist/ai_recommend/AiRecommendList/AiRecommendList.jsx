import React from "react";
import "./AiRecommendList.css";

const AiRecommendList = ({ room, onClose }) => {
  if (!room) return null;

  return (
    <div className="recommend-modal-overlay">
      <div className="recommend-modal">
        <button className="close-btn" onClick={onClose}>
          &times;
        </button>
        <div className="modal-header">
          <h2>{room.contractType} {room.price}</h2>
        </div>
        <img
          src={room.imageUrl || "/images/logo/192image.png"}
          alt="매물 이미지"
          className="modal-image"
        />
        <div className="modal-content">
          <p><strong>주소:</strong> {room.address}</p>
          <p><strong>설명:</strong> {room.description}</p>
          {/* 여기에 필요한 추가 정보들 넣기 */}
        </div>
      </div>
    </div>
  );
};

export default AiRecommendList;
