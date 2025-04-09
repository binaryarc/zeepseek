import React, { useEffect, useState, useRef } from "react";
import ListingCard from "./ListingCard";
import "./MainListingSection.css"; // 섹션 관련 스타일
import { fetchTop5Property } from "../../../common/api/api";
import { useSelector } from "react-redux";
import { TfiArrowCircleLeft, TfiArrowCircleRight } from "react-icons/tfi";
import defaultImg from "../../../assets/logo/512image.png"
import { useNavigate } from "react-router-dom";

const Top5ListingSection = () => {
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
    // ...생략
  ];

  useEffect(() => {
    if (!user) return;

    const fetchRecommendations = async () => {
      try {
        const res = await fetchTop5Property(user.idx);
        console.log("top5 매물: ", res);
        setDongName(res.data.name);
        setRecommendList(res.data.properties);  // ★ 여기가 비어 있을 수 있음
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
      <h1>
        {user ? (
          <>
            방금 보신 <span className="highlight-ai">{dongName}</span>의 인기 매물
          </>
        ) : (
          "로그인이 필요한 서비스 입니다!"
        )}
      </h1>

      <div className="listing-container-wrapper">
        <button className="scroll-button left" onClick={handleScrollLeft}>
          <TfiArrowCircleLeft size={32} color="#333" />
        </button>

        {/* 
          ★ 이 부분이 핵심입니다:
          1) user가 있으나 dataToRender가 빈 배열인 경우 = "아직 인기 매물이 없습니다" 
          2) 그 외에는 ListingCard를 렌더링 
        */}
        <ul
          className="main-listing-container"
          ref={containerRef}
          style={!user ? { filter: "blur(4px)" } : {}}
        >
          {dataToRender.length > 0 ? (
            dataToRender.map((item, index) => (
              <ListingCard
                key={index}
                image={item.imageUrl}
                contractType={item.contractType}
                price={item.price}
                address={item.address}
                roomType={item.roomType}
                description={item.description}
                roomBathCount={item.roomBathCount}
                onClick={() => {
                  const type = item.roomType;
                  const normalizedRoomType =
                    type === "원룸/투룸" || type === "오피스텔" ? type : "주택/빌라";

                  navigate("/map", {
                    state: {
                      lat: item.latitude,
                      lng: item.longitude,
                      roomType: normalizedRoomType,
                      selectedPropertyId: item.propertyId,
                    },
                  });
                }}
              />
            ))
          ) : (
            /* user가 있는데 리스트가 비었을 때만 메시지 표시 (user 없으면 blur) */
            <div className="no-listing-message">
              아직 인기 매물이 없습니다
            </div>
          )}
        </ul>

        <button className="scroll-button right" onClick={handleScrollRight}>
          <TfiArrowCircleRight size={32} color="#333" />
        </button>

        {!user && (
          <div className="login-overlay">
            로그인을 해주세요!!
            <br />
            로그인을 하시면 추천을 받아보실 수 있습니다!
          </div>
        )}
      </div>
    </section>
  );
};

export default Top5ListingSection;