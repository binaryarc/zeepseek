import React, { useEffect, useState, useRef } from "react";
import ListingCard from "./ListingCard";
import "./MainListingSection.css"; // 섹션 관련 스타일
import { aiRecommendByUserId } from "../../../common/api/api";
import { useSelector } from "react-redux";
import { TfiArrowCircleLeft, TfiArrowCircleRight } from "react-icons/tfi";
import defaultImg from "../../../assets/logo/512image.png"
import { useNavigate } from "react-router-dom";
import zeepai from "../../../assets/images/zeepai.png"

const MainListingSection = () => {
  const user = useSelector((state) => state.auth.user);
  const [dongName, setDongName] = useState("");
  const [recommendList, setRecommendList] = useState([]);
  const containerRef = useRef(null);
  const navigate = useNavigate();

  // 더미 데이터: 로그인하지 않은 경우에도 표시할 데이터
  const dummyData = [
    {
      imageUrl: defaultImg,
      contractType: "원룸",
      price: "1,000만원",
      address: "서울시 강남구",
      roomType: "원룸",
      description: "더미 매물 설명",
      roomBathCount: "1/0",
    },
    {
      imageUrl: defaultImg,
      contractType: "투룸",
      price: "2,000만원",
      address: "서울시 서초구",
      roomType: "투룸",
      description: "더미 매물 설명",
      roomBathCount: "2/1",
    },
    {
      imageUrl: defaultImg,
      contractType: "오피스텔",
      price: "1,500만원",
      address: "서울시 강동구",
      roomType: "오피스텔",
      description: "더미 매물 설명",
      roomBathCount: "1/1",
    },
    {
      imageUrl: defaultImg,
      contractType: "주택",
      price: "3,000만원",
      address: "서울시 강북구",
      roomType: "주택",
      description: "더미 매물 설명",
      roomBathCount: "3/2",
    },
    {
      imageUrl: defaultImg,
      contractType: "빌라",
      price: "800만원",
      address: "서울시 마포구",
      roomType: "빌라",
      description: "더미 매물 설명",
      roomBathCount: "1/0",
    },
    {
      imageUrl: defaultImg,
      contractType: "빌라",
      price: "800만원",
      address: "서울시 마포구",
      roomType: "빌라",
      description: "더미 매물 설명",
      roomBathCount: "1/0",
    },
    {
      imageUrl: defaultImg,
      contractType: "빌라",
      price: "800만원",
      address: "서울시 마포구",
      roomType: "빌라",
      description: "더미 매물 설명",
      roomBathCount: "1/0",
    },
  ];

  useEffect(() => {
    if (!user) return;
    const fetchRecommendations = async () => {
      try {
        const res = await aiRecommendByUserId(user.idx);
        setDongName(res.data.dongName);
        setRecommendList(res.data.recommendedProperties);
      } catch (error) {
        console.error("추천 매물 정보를 불러오는데 실패했습니다:", error);
      }
    };

    fetchRecommendations();
  }, [user]);

  // 좌우 스크롤 함수 (300px씩 이동)
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

  // user가 있으면 추천 데이터, 없으면 더미 데이터를 사용
  const dataToRender = user ? recommendList : dummyData;

  return (
    <section className="main-listing-section">
    <p className="main-listing-subtitle">
      {user ? (
        <>
          <h1><img className="zeepai-main" src= {zeepai} alt="zeepai" />ZEEPSEEK <span className="highlight-ai">AI</span>가 추천하는 매물</h1>
          <strong> {user.nickname}</strong> 님과 같은&ensp;
          <span className="user-tag">
            {Math.floor(user.age / 10) * 10}대  {user.gender === 1 ? "남성" : "여성"}
          </span>
          이 많이 본&ensp;
          <span className="user-tag">
            {dongName}
          </span>
          &ensp;매물
        </>
      ) : (
        "로그인이 필요한 서비스입니다!"
      )}
    </p>
      <div className="listing-container-wrapper">
        <button className="scroll-button left" onClick={handleScrollLeft}>
          <TfiArrowCircleLeft size={32} color="#333" />
        </button>
        {/* 로그인 안했을 경우 블러 효과 적용 */}
        <ul
          className="main-listing-container"
          ref={containerRef}
          style={!user ? { filter: "blur(4px)" } : {}}
        >
          {dataToRender.map((item, index) => (
            <ListingCard
              key={index}
              image={item.imageUrl}
              contractType={item.contractType}
              price={item.price}
              address={item.address}
              roomType={item.roomType}
              description={item.description}
              roomBathCount={item.roomBathCount}
              onClick={() =>{
                const type = item.roomType;
                const normalizedRoomType =
                  type === "원룸/투룸" || type === "오피스텔" ? type : "주택/빌라";

                navigate("/map", {
                  state: {
                    lat: item.latitude,
                    lng: item.longitude,
                    roomType: normalizedRoomType, // ✅ 정제된 룸타입
                    selectedPropertyId: item.propertyId,
                  },
                });
              }}
            />
          ))}
        </ul>
        <button className="scroll-button right" onClick={handleScrollRight}>
          <TfiArrowCircleRight size={32} color="#333" />
        </button>
        {/* 로그인하지 않은 경우 오버레이 메시지 표시 */}
        {!user && (
          <div className="login-overlay">
            로그인을 해주세요!!<br />
            로그인을 하시면 추천을 받아보실 수 있습니다!
          </div>
        )}
      </div>
    </section>
  );
};

export default MainListingSection;
