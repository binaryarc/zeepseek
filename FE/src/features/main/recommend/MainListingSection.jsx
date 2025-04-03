import React from "react";
import ListingCard from "./ListingCard";
import "./MainListingSection.css"; // 섹션 관련 스타일

const MainListingSection = () => {
  // 예시 데이터
  const listings = [
    {
      image: "매물1.jpg", // 실제 이미지 경로
      altText: "매물 1 이미지",
      price: "전세 1억",
      subInfo: "2층, 25m², 고려대 3분",
      description: "잠실세대 분리형, 채광좋은 전세",
    },
    {
      image: "매물2.jpg",
      altText: "매물 2 이미지",
      price: "매매 3억 2,000",
      subInfo: "3층, 34m², 고대역 5분",
      description: "잠실세대 분리형, 채광 좋은 매매",
    },
    {
      image: "매물3.jpg",
      altText: "매물 3 이미지",
      price: "전세 1억 2,000",
      subInfo: "4층, 38m², 고려대 5분",
      description: "잠실세대 분리형, 채광좋은 전세",
    },
    {
      image: "매물4.jpg",
      altText: "매물 4 이미지",
      price: "전세 1억 2,000",
      subInfo: "4층, 38m², 고려대 5분",
      description: "잠실세대 분리형, 채광좋은 전세",
    },
  ];

  return (
    <section className="main-listing-section">
      <h1>ZEEPSEEK AI가 추천하는 매물</h1>
      <p>실시간 인기 매물🔥</p>

      {/* ul로 감싸, li 단위로 카드 렌더링 */}
      <ul className="main-listing-container">
        {listings.map((item, index) => (
          <ListingCard
            key={index}
            image={item.image}
            altText={item.altText}
            price={item.price}
            subInfo={item.subInfo}
            description={item.description}
          />
        ))}
      </ul>
    </section>
  );
};

export default MainListingSection;
