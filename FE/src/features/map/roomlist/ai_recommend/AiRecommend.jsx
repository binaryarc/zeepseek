import React, { useState, useRef, useEffect } from "react";
import "./AiRecommend.css";
import {
  fetchAIRecommendedProperties,
  fetchNearbyPlaces,
  getPropertyDetail,
} from "../../../../common/api/api";
import defaultImage from "../../../../assets/logo/192image.png";
import DongNameMarkers from "../../mainmap/salecountmarkers/DongNameMarkers/DongNameMarkers";
import GuNameMarkers from "../../mainmap/salecountmarkers/GuNameMarkers/GuNameMarkers";
import AiRecommendList from "./AiRecommendList/AiRecommendList";
import zeepai from "../../../../assets/images/zeepai.png";
import { useSelector, useDispatch } from "react-redux";
import {
  setSelectedPropertyId,
  setSelectedPropertySource,
  setAiRecommendedList,
  setFilterValues,
} from "../../../../store/slices/roomListSlice";

const AiRecommend = () => {
  const [roomType, setRoomType] = useState("원룸/투룸");
  const [contractType, setContractType] = useState("월세");
  const [priceRange, setPriceRange] = useState(["", ""]); // [최소, 최대]
  const dispatch = useDispatch();

  const [selectedRoom, setSelectedRoom] = useState(null); // 모달에 띄울 매물 상세 정보

  const circleOverlayRef = useRef(null);  // 최신 원 마커 관리용 useRef
  const nearbyMarkersRef = useRef([]); // 마커들 ref에 보관

  const [maxType, setMaxType] = useState(null);

  const user = useSelector((state) => state.auth.user);
  const filterValues = useSelector((state) => state.roomList.filterValues);

  // 선택된 매물 id Redux에서 가져오기(매물 상세 정보 창 관리용)
  const selectedPropertyId = useSelector(state => state.roomList.selectedPropertyId);

  const filters = [
    "여가",
    "식당",
    "의료",
    "편의",
    "대중교통",
    "카페",
    "치킨집",
  ];


  const [isLoading, setIsLoading] = useState(false);
  const [isRecoDone, setIsRecoDone] = useState(false);

  const aiRecommendedList = useSelector((state) => state.roomList.aiRecommendedList);
  const [priceError, setPriceError] = useState("");

  const handleSliderChange = (label, value) => {
    dispatch(
      setFilterValues({
        ...filterValues,
        [label]: value,
      })
    );
  };

  const isValidPriceRange = ([minStr, maxStr]) => {
    const min = Number(minStr);
    const max = Number(maxStr);
  
    if (!minStr || !maxStr) {
      setPriceError("가격을 모두 입력해주세요.");
      return false;
    }
  
    if (isNaN(min) || isNaN(max)) {
      setPriceError("숫자만 입력 가능합니다.");
      return false;
    }
  
    if (min < 0 || max < 0) {
      setPriceError("0원 이상만 입력 가능합니다.");
      return false;
    }
  
    if (min > max) {
      setPriceError("최소값은 최대값보다 작아야 합니다.");
      return false;
    }
  
    setPriceError("");
    return true;
  };


  // 원 마커 지우는 함수
  const clearMapOverlays = () => {
    window.clearHoverMarker?.();
    nearbyMarkersRef.current.forEach((marker) => marker.setMap(null));
    nearbyMarkersRef.current = [];
    if (circleOverlayRef.current) {
      circleOverlayRef.current.setMap(null);
      circleOverlayRef.current = null;
    }
  };
  
  // 상세 정보 닫힐 때도 마커 지우기
  useEffect(() => {
    if (selectedPropertyId === null) {
      clearMapOverlays(); // ✅ 상세 정보 창 닫힐 때도 제거
    }
  }, [selectedPropertyId]);
  
  

  const handleRecommendClick = async () => {
    if (!isValidPriceRange(priceRange)) return;

    const preferenceData = {
      userId: user.idx,
      age: user.age,
      gender: user.gender,
      roomType,
      contractType,
      priceRange: [
        Number(priceRange[0]),
        Number(priceRange[1]),
      ],
      transportScore: filterValues["대중교통"] / 100,
      restaurantScore: filterValues["식당"] / 100,
      healthScore: filterValues["의료"] / 100,
      convenienceScore: filterValues["편의"] / 100,
      cafeScore: filterValues["카페"] / 100,
      chickenScore: filterValues["치킨집"] / 100,
      leisureScore: filterValues["여가"] / 100,
    };

    console.log("request data: ", preferenceData);

    setIsLoading(true); // 로딩 시작

    try {
      const result = await fetchAIRecommendedProperties(preferenceData);
      if (result) {
        const rawList = result.recommendedProperties;

        // 1. 각 매물의 상세 정보를 요청
        const detailedList = await Promise.all(
          rawList.map(async (item) => {
            const detail = await getPropertyDetail(item.propertyId);
            return {
              ...item, // AI 추천 점수 등
              ...detail, // 상세 정보 (contractType, price 등)
            };
          })
        );

        

        console.log("전체 추천 결과: ", result);
        console.log("추천 매물 목록:", result.recommendedProperties);
        console.log("detailedList: ", detailedList)

        dispatch(setAiRecommendedList(detailedList));
        dispatch(setFilterValues({ ...filterValues }));

        // setRecommendedList(detailedList);
        setIsRecoDone(true);
        setMaxType(result.maxType);
        console.log("user정보: ", user);
      }
    } catch (error) {
      console.error("추천 실패:", error);
    } finally {
      setIsLoading(false); // 로딩 종료
    }
  };

  const handleRetry = () => {
    setIsRecoDone(false);
    dispatch(setAiRecommendedList([]));
    dispatch(setSelectedPropertyId(null));
    dispatch(setSelectedPropertySource(null));
  };


  const handleRoomClick = async (item) => {

    if (selectedPropertyId === item.propertyId) {
      dispatch(setSelectedPropertyId(null));
      dispatch(setSelectedPropertySource(null));
      clearMapOverlays(); // ✅ 같은 매물 클릭 시 제거
      return;
    }
  
    dispatch(setSelectedPropertyId(item.propertyId));
    dispatch(setSelectedPropertySource("recommend"));

    clearMapOverlays(); // ✅ 기존 마커 제거

    window.clearHoverMarker();
    nearbyMarkersRef.current.forEach((marker) => marker.setMap(null));
    nearbyMarkersRef.current = [];
    if (circleOverlayRef.current) {
      circleOverlayRef.current.setMap(null);
      circleOverlayRef.current = null;
    }

    if (!item.latitude || !item.longitude || !window.map) return;
    window.setHoverMarker(item.latitude, item.longitude);
    const latlng = new window.kakao.maps.LatLng(item.latitude, item.longitude);
    window.map.setLevel(5);
    window.map.setCenter(latlng);

    const circle = new window.kakao.maps.Circle({
      center: latlng,
      radius: 1000,
      strokeWeight: 2,
      strokeColor: "#00a0e9",
      strokeOpacity: 0.8,
      strokeStyle: "solid",
      fillColor: "#00a0e9",
      fillOpacity: 0.1,
    });
    circle.setMap(window.map);
    circleOverlayRef.current = circle;

    try {
      const response = await fetchNearbyPlaces(maxType, item.longitude, item.latitude);
      const imageSrc = `/images/icons/${maxType}.png`;
      const imageSize = new window.kakao.maps.Size(30, 30);
      const markerImage = new window.kakao.maps.MarkerImage(imageSrc, imageSize);
      const newMarkers = (response.data || []).map(({ latitude, longitude, name }) =>
        new window.kakao.maps.Marker({
          position: new window.kakao.maps.LatLng(latitude, longitude),
          map: window.map,
          title: name,
          image: markerImage,
        })
      );
      newMarkers.forEach((marker) => marker.setMap(window.map));
      nearbyMarkersRef.current = newMarkers;
    } catch (err) {
      console.error("시설 마커 에러:", err);
    }

    setSelectedRoom(item);
  };

  return (
    <div className="ai-filter-container">
      <DongNameMarkers map={window.map} />
      <GuNameMarkers map={window.map} />
      {!isRecoDone && !isLoading && (
        <div className="slider-section">
          <h3 className="recommend-title">나랑 딱 맞는 매물 찾기</h3>
          <div className="option-section">
            <div className="button-select-group">
              <label>방 종류</label>
              <div className="button-row">
                {["원룸/투룸", "오피스텔", "주택/빌라"].map((type) => (
                  <button
                    key={type}
                    className={`toggle-button ${
                      roomType === type ? "active" : ""
                    }`}
                    onClick={() => setRoomType(type)}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>

            <div className="button-select-group">
              <label>계약 방식</label>
              <div className="button-row">
                {["월세", "전세", "매매"].map((type) => (
                  <button
                    key={type}
                    className={`toggle-button ${
                      contractType === type ? "active" : ""
                    }`}
                    onClick={() => setContractType(type)}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>

            <div className="price-range-group">
            {priceError && <p className="price-error">{priceError}</p>}
              <label>가격 범위 (단위: 만원)</label>
              <div style={{ display: "flex", gap: "1rem" }}>
                <input
                  type="number"
                  inputMode="numeric"
                  value={priceRange[0]}
                  onChange={(e) =>
                    setPriceRange([Number(e.target.value), priceRange[1]])
                  }
                  placeholder="최소"
                  // min={0}
                />
                <p>~</p>
                <input
                  type="number"
                  inputMode="numeric"
                  value={priceRange[1]}
                  onChange={(e) =>
                    setPriceRange([priceRange[0], e.target.value])
                  }
                  placeholder="최대"
                  // min={priceRange[0]}
                />
                <span>만원</span>
              </div>
            </div>
          </div>
          {/* <hr /> */}
          <p className="filter-title">내 집 근처에는?</p>
          {filters.map((label) => (
            <div key={label} className={`slider-block slider-${label}`}>
              <div className="slider-label-row">
                <label>{label}</label>
                <span>{filterValues[label]}</span>
              </div>
              <input
                type="range"
                min="1"
                max="100"
                value={filterValues[label]}
                onChange={(e) =>
                  handleSliderChange(label, Number(e.target.value))
                }
                style={{
                  "--value": `${filterValues[label]}%`,
                }}
              />
            </div>
          ))}
          <button
            className="recommend-search-btn"
            onClick={handleRecommendClick}
          >
            AI 추천 받기
          </button>
        </div>
      )}

      {isLoading && (
        <div className="loader-container">
          <img src={zeepai} alt="ai_image" className="zeepai_image" />
          <p>AI가 매물을 추천 중이에요...</p>
          <div className="spinner"></div>
        </div>
      )}

      
      {isRecoDone && !isLoading && (
        <div className="result-section">
          <button className="re-recommend-search-btn" onClick={handleRetry}>
            추천 다시 받기
          </button>
          <div className="recommend-results">
            <h4 className="result-title">
              추천 매물 목록
            </h4>
            <ul className="result-list">
              {aiRecommendedList.map((item) => (
                <li
                  key={item.propertyId}
                  className={`room-item ${
                    selectedPropertyId === item.propertyId ? "selected" : ""
                  }`}
                  onClick={() => handleRoomClick(item)}
                >
                  <img src={item.imageUrl || defaultImage} alt="매물 이미지" />
                  <div className="room-info">
                    <p className="room-title">{item.contractType} {item.price}</p>
                    <p className="room-description">{item.description}</p>
                    <p className="room-address">{item.address}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};

export default AiRecommend;
