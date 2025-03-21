import React from "react";
import "./RoomList.css";

const RoomList = () => {
  // 더미 데이터
  const properties = [
    {
      id: 1,
      title: "전세 3억 2,000",
      description: "신용산역 역세권, 저렴한 전세",
      img: "https://picsum.photos/200/300?random=1",
    },
    {
      id: 2,
      title: "전세 1억 8,000",
      description: "용산역 역세권, 저렴한 전세",
      img: "https://picsum.photos/200/300?random=1",
    },
    {
      id: 3,
      title: "월세 5000/150",
      description: "카페거리 근처, 용산역 접근성",
      img: "https://picsum.photos/200/300?random=1",
    },
  ];

  return (
    <div className="room-list">
      <h2>매물 리스트</h2>
      {properties.map((room) => (
        <div key={room.id} className="room-item">
          <img src={room.img} alt={room.title} />
          <div>
            <p className="room-title">{room.title}</p>
            <p className="room-description">{room.description}</p>
          </div>
        </div>
      ))}
    </div>
  );
};

export default RoomList;
