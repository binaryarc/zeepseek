// 찜 목록
import React, { useEffect, useState } from "react";
// import { useSelector } from "react-redux";
import { fetchLikedProperties } from "../../../common/api/api";
import "./Zzim.css";
import Navbar from "../../../common/navbar/Navbar";

const Zzim = () => {
  const [groupedZzims, setGroupedZzims] = useState({});
  const userId = 2

  useEffect(() => {
    console.log('실행되니?')
    fetchLikedProperties(userId).then((zzims) => {
      // 구 단위로 그룹핑
      const grouped = zzims.reduce((acc, item) => {
        const gu = item.address.split(" ")[1];
        if (!acc[gu]) acc[gu] = [];
        acc[gu].push(item);
        return acc;
      }, {});
      setGroupedZzims(grouped);
    });
  }, [userId]);

  return (
    <div className="zzim-page">
      <Navbar/>
      <h2 className="zzim-title">찜한 매물</h2>
      {Object.keys(groupedZzims).map((gu) => (
        <div key={gu} className="zzim-gu-section">
          <h3 className="gu-name">{gu}</h3>
          <div className="zzim-list">
            {groupedZzims[gu].map((room) => (
              <div key={room.propertyId} className="zzim-card">
                <img
                  src={room.imageUrl}
                  alt="매물 이미지"
                  className="zzim-img"
                />
                <div className="zzim-info">
                  <span className="room-type">{room.computedRoomType}</span>
                  <p className="price">{room.price}</p>
                  <p className="desc">{room.description}</p>
                  <p className="addr">{room.address}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

export default Zzim;
