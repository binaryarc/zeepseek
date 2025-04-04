import React from "react";
import "./AiRecommendList.css";
import defaultImg from "../../../../../assets/logo/512image.png"

const AiRecommendList = ({ room, item, onClose }) => {
  if (!room && !item) return null;


  return (
    <div className="recommend-modal-overlay">
      <div className="recommend-modal">
        <button className="recommend-modal-close-btn" onClick={onClose}>
          &times;
        </button>
        <div className="modal-body">
          <div className="recommend-modal-left-section">
            <div className="modal-image-section">
              <img
                src={room.imageUrl || defaultImg}
                alt="매물 이미지"
                className="modal-image"
              />
            </div>
            <div className="modal-info-section">
              <p><strong>{room.address}</strong></p>
              <h2 className="modal-title">
                {room.contractType} {room.price}
              </h2>
              <p><strong>설명:</strong> {room.description}</p>
            </div>
          </div>
          <div className="modal-score-section">
            <p>유사도 : {item.similarity}</p>
            <p>점수 비교</p>
            
          </div>
        </div>
      </div>
    </div>
  );
};

export default AiRecommendList;
