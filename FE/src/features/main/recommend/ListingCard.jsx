import React from "react";
import "./ListingCard.css"
import "../../mypage/zzim/Zzim.css"
import defaultImg from "../../../assets/logo/512image.png";

const ListingCard = ({ image, address , contractType, price, roomType, description, roomBathCount }) => {

  const getRoomType = (roomBathCount) => {
    // 숫자에 해당하는 매핑 객체
    const roomTypeMapping = {
      "1": "원룸",
      "2": "투룸",
      "3": "쓰리룸",
      "4": "포룸"  // 필요에 따라 다른 문자열로 변경 가능
    };
    
    // roomBathCount 값이 존재하는지 확인 후, '/'를 기준으로 split하여 첫번째 값 추출
    if (roomBathCount) {
      const count = roomBathCount.split('/')[0];
      return roomTypeMapping[count] || "";
    }
    return "";
  }

  return (
    <li className="zzim-list">
      <div className="zzim-card">
        <img
          src={image || defaultImg}
          alt="매물 이미지"
          className="zzim-img"
        />
        <div className="zzim-info">
          <span className="zzim-room-type">{getRoomType(roomBathCount)}</span>
          <p className="price">{price}</p>
          <p className="desc">{description}</p>
          <p className="addr">{address}</p>
        </div>
      </div>
    </li>
  );
};
export default ListingCard;
