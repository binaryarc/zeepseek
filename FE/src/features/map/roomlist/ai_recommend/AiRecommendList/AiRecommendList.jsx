import React from "react";
import "./AiRecommendList.css";

const AiRecommendList = ({ room, onClose }) => {
  if (!room) return null;

  return (
    <div className="recommend-modal-overlay">
      <div className="recommend-modal">
        <button className="recommend-modal-close-btn" onClick={onClose}>
          &times;
        </button>
        <div className="modal-body">
          <div className="modal-image-section">
            <img
              src={room.imageUrl || "/images/logo/192image.png"}
              alt="매물 이미지"
              className="modal-image"
            />
          </div>
          <div className="modal-info-section">
            <h2 className="modal-title">
              {room.contractType} {room.price}
            </h2>
            <p><strong>주소:</strong> {room.address}</p>
            <p><strong>설명:</strong> {room.description}</p>
            {/* 필요한 정보들 추가 가능 */}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AiRecommendList;
