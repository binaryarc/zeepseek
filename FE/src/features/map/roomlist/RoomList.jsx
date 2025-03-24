import React, {useState} from "react";
import "./RoomList.css";
import AiRecommend from "./ai_recommend/AiRecommend";

const RoomList = () => {

  const [selectedTab, setSelectedTab] = useState("원룸/투룸");

  const handleTabClick = (tab) => {
    setSelectedTab(tab)
  }

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
    {
      id: 4,
      title: "월세 5000/150",
      description: "카페거리 근처, 용산역 접근성",
      img: "https://picsum.photos/200/300?random=1",
    },
    {
      id: 5,
      title: "월세 5000/150",
      description: "카페거리 근처, 용산역 접근성",
      img: "https://picsum.photos/200/300?random=1",
    },
    {
      id: 6,
      title: "월세 5000/150",
      description: "카페거리 근처, 용산역 접근성",
      img: "https://picsum.photos/200/300?random=1",
    },
    {
      id: 7,
      title: "월세 5000/150",
      description: "카페거리 근처, 용산역 접근성",
      img: "https://picsum.photos/200/300?random=1",
    },
  ];

  return (
    <div className="room-list">
      <nav className="room-type">
        {["원룸/투룸", '오피스텔', "주택/빌라", "AI 추천"].map((tab) => (
          <span
            key={tab}
            className={selectedTab === tab ? "active-tab" : ""}
            onClick={() => handleTabClick(tab)}
          >
            {tab}
          </span>
        ))}
      </nav>

      <div className="room-list-inner">
        {selectedTab === "AI 추천" ? (
          <AiRecommend />
        ) : (
          properties.map((room) => (
            <div key={room.id} className="room-item">
              <img src={room.img} alt={room.title} />
              <div>
                <p className="room-title">{room.title}</p>
                <p className="room-description">{room.description}</p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default RoomList;
