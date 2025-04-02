// 찜 목록
import React, { useEffect, useState } from "react";
// import { useSelector } from "react-redux";
import { fetchLikedProperties, likeProperty, unlikeProperty } from "../../../common/api/api";
import "./Zzim.css";
import Navbar from "../../../common/navbar/Navbar";
import { useDispatch, useSelector } from "react-redux";
import blankImg from "../../../assets/logo/512image.png"
import { FaHeart } from "react-icons/fa";

const Zzim = () => {
  const [groupedZzims, setGroupedZzims] = useState({});
  const user = useSelector((state) => state.auth.user)
  const [flag, setFlag] = useState(false)

  useEffect(() => {
    if (!user?.idx) return;
    
    fetchLikedProperties(user.idx).then((zzims) => {
      // 구 단위로 그룹핑
      const grouped = zzims.reduce((acc, item) => {
        const gu = item.address.split(" ")[1];
        if (!acc[gu]) acc[gu] = [];
        acc[gu].push(item);
        return acc;
      }, {});
      setGroupedZzims(grouped);
      console.log(groupedZzims)
    });
  }, [user.idx, flag]);

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
  };

const toggleLike = async (room) => {
    const { propertyId } = room;
    if (user === null) return alert("로그인이 필요합니다.");

    try {
      await unlikeProperty(propertyId, user.idx);
      setFlag(prevFlag => !prevFlag);
    } catch (err) {
      console.error("찜 취소 실패:", err);
    }
  };

  return (
    <div className="zzim-page">
      <Navbar />
      <h2 className="zzim-title">찜한 매물</h2>
      {Object.keys(groupedZzims).length === 0 ? (
        <div className="empty-card">
          <p className="animate-text">
            <span>매</span>
            <span>물</span>
            <span>을</span>
            <span>&nbsp;</span>
            <span>찜</span>
            <span>해</span>
            <span>&nbsp;</span>
            <span>보</span>
            <span>세</span>
            <span>요</span>
            <span>!</span>
            <span>!</span>
          </p>
        </div>
      ) : (
        Object.keys(groupedZzims).map((gu) => (
          <div key={gu} className="zzim-gu-section">
            <h3 className="gu-name">{gu}</h3>
            <div className="zzim-list">
              {groupedZzims[gu].map((room) => (
                <div key={room.propertyId} className="zzim-card">
                  <img
                    src={room.imageUrl || blankImg}
                    alt="매물 이미지"
                    className="zzim-img"
                  />
                  <button onClick={() => toggleLike(room)} className="zzim-delete-btn">
                    <FaHeart color="red" size={24} />
                  </button>
                  <div className="zzim-info">
                    <span className="zzim-room-type">{getRoomType(room.roomBathCount)}</span>
                    <p className="price">{room.price}</p>
                    <p className="desc">{room.description}</p>
                    <p className="addr">{room.address}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))
      )}
    </div>
  );
};

export default Zzim;
