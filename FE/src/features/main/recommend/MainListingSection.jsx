import React, { useEffect, useState, useRef } from "react";
import ListingCard from "./ListingCard";
import "./MainListingSection.css"; // 섹션 관련 스타일
import { aiRecommendByUserId } from "../../../common/api/api";
import { useSelector } from "react-redux";
import { TfiArrowCircleRight } from "react-icons/tfi";
import { TfiArrowCircleLeft } from "react-icons/tfi";

const MainListingSection = () => {
  const user = useSelector((state) => state.auth.user);
  const [dongName, setDongName] = useState("");
  const [recommendList, setRecommendList] = useState([]);
  const containerRef = useRef(null); // 스크롤 컨테이너에 대한 ref

  useEffect(() => {
    if (!user) return;
    console.log("recommend userid: " + user.idx);

    const fetchRecommendations = async () => {
      try {
        const res = await aiRecommendByUserId(user.idx);
        setDongName(res.data.dongName);
        setRecommendList(res.data.recommendedProperties);
        console.log("recommendList: ", res.data.recommendedProperties);
      } catch (error) {
        console.error("추천 매물 정보를 불러오는데 실패했습니다:", error);
      }
    };

    fetchRecommendations();
  }, [user]);

  // 좌우 스크롤 함수 (300px씩 이동, 필요에 따라 조정 가능)
  const handleScrollLeft = () => {
    if (containerRef.current) {
      containerRef.current.scrollBy({ left: -300, behavior: "smooth" });
    }
  };

  const handleScrollRight = () => {
    if (containerRef.current) {
      containerRef.current.scrollBy({ left: 300, behavior: "smooth" });
    }
  };

  return (
    <section className="main-listing-section">
      <h1>ZEEPSEEK AI가 추천하는 매물</h1>
      <p>"{dongName}"을 자주보셔서 추천해 드려요!!!🔥</p>
      <div className="listing-container-wrapper">
        <button className="scroll-button left" onClick={handleScrollLeft}>
          <TfiArrowCircleLeft size={32} color="#333" />
        </button>
        <ul className="main-listing-container" ref={containerRef}>
          {user &&
            recommendList.map((item, index) => (
              <ListingCard
                key={index}
                image={item.imageUrl}
                contractType={item.contractType}
                price={item.price}
                address={item.address}
                roomType={item.roomType}
                description={item.description}
                roomBathCount={item.roomBathCount}
              />
            ))}
        </ul>
        <button className="scroll-button right" onClick={handleScrollRight}>
          <TfiArrowCircleRight size={32} color="#333" />
        </button>
      </div>
    </section>
  );
};

export default MainListingSection;
