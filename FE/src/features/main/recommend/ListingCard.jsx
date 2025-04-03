import React from "react";
import "./ListingCard.css"
import defaultImg from "../../../assets/logo/512image.png";

const ListingCard = ({ image, altText, price, subInfo, description }) => {
  return (
    <li className="main-listing-li">
      <div className="main-listing-item">
        {/* 상단 이미지 영역 */}
        <div className="listing-upside">
          <div className="listing-img-container">
            {/* 실제로는 image prop이 있으면 그걸 사용, 없으면 defaultImg를 쓰는 식으로 구현 가능 */}
            <img
              src={defaultImg}
              alt={altText}
              className="listing-img"
            />
          </div>
        </div>

        {/* 하단 정보 영역 */}
        <div className="listing-info">
          <h3 className="listing-price">{price}</h3>
          <p className="listing-subinfo">{subInfo}</p>
          <p className="listing-desc">{description}</p>
        </div>
      </div>
    </li>
  );
};
export default ListingCard;
