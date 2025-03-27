import React from "react";
import "./AiSlidePanel.css";
import AiRecommend from "../roomlist/ai_recommend/AiRecommend";

const AiSlidePanel = ({ isOpen, onClose }) => {
  return (
    <div className={`slide-panel ${isOpen ? "open" : ""}`}>
      <button className="close-btn" onClick={onClose}>X</button>
      <AiRecommend />
    </div>
  );
};

export default AiSlidePanel;
