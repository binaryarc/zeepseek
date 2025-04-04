import React, { useEffect, useState } from "react";
import ListingCard from "./ListingCard";
import "./MainListingSection.css"; // 섹션 관련 스타일
import { aiRecommendByUserId } from "../../../common/api/api";
import { useSelector } from "react-redux";

const MainListingSection = () => {
  const user = useSelector((state) => state.auth.user);
  const [dongName, setDongName] = useState("");
  const [recommendList, setRecommendList] = useState([]);

  useEffect(() => {
    if (!user) return;
    console.log("recommend userid: " + user.idx);

    const fetchRecommendations = async () => {
      try {
        const res = await aiRecommendByUserId(user.idx);
        setDongName(res.data.dongName)
        setRecommendList(res.data.recommendedProperties);
        console.log("recommendList: ", res.data.recommendedProperties);
      } catch (error) {
        console.error("추천 매물 정보를 불러오는데 실패했습니다:", error);
      }
    };

    fetchRecommendations();
  }, [user]);

  return (
    <section className="main-listing-section">
      <h1>ZEEPSEEK AI가 추천하는 매물</h1>
      <p>"{dongName}"을 자주보셔서 추천해 드려요!!!🔥</p>
      <ul className="main-listing-container">
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
    </section>
  );
};


export default MainListingSection;
