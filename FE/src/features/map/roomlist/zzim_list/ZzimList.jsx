import React, { useEffect, useState } from "react";
import "../RoomList.css"; // ✅ 기존 RoomList 스타일 그대로 적용
import "./ZzimList.css"
import RoomDetail from "../RoomDetail";
import { useSelector } from "react-redux";
import defaultImage from "../../../../assets/logo/192image.png";
import { setSelectedPropertyId } from "../../../../store/slices/roomListSlice";
import { fetchLikedProperties, unlikeProperty, fetchNearbyPlaces } from "../../../../common/api/api";
import { useDispatch } from "react-redux";

const ZzimList = () => {
  const [rooms, setRooms] = useState([]);
  const selectedPropertyId = useSelector((state) => state.roomlist)
  const [circleOverlay, setCircleOverlay] = useState(null);
  const [nearbyMarkers, setNearbyMarkers] = useState([]);
  const user = useSelector((state) => state.auth.user);

  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 15;

  const [selectedFacilityType, setSelectedFacilityType] = useState("leisure");

  const selectFacilityType = (type) => {
    setSelectedFacilityType(type);
  };

  const dispatch = useDispatch()

  const facilityEmojiMap = {
    chicken: "🍗",
    leisure: "🎮",
    health: "🏥",
    convenience: "🏪",
    transport: "🚌",
    cafe: "☕",
  };

  useEffect(() => {
    const loadZzimRooms = async () => {
      if (!user?.idx) return;
      const result = await fetchLikedProperties(user.idx);
      const likedRooms = result.map(room => ({ ...room, liked: true }));
      setRooms(likedRooms || []);
    };
    loadZzimRooms();
  }, [user?.idx]);

  const handleMouseEnter = async (room) => {
    if (!room.latitude || !room.longitude) return;

    nearbyMarkers.forEach((m) => m.setMap(null));
    if (circleOverlay) circleOverlay.setMap(null);

    const map = window.map;
    const latlng = new window.kakao.maps.LatLng(room.latitude, room.longitude);
    window.setHoverMarker(room.latitude, room.longitude);
    map.setCenter(latlng);
    map.setLevel(5);

    const circle = new window.kakao.maps.Circle({
      center: latlng,
      radius: 1000,
      strokeWeight: 2,
      strokeColor: "#00a0e9",
      strokeOpacity: 0.8,
      fillColor: "#00a0e9",
      fillOpacity: 0.1,
    });
    circle.setMap(map);
    setCircleOverlay(circle);

    // ✅ 안전한 데이터 구조 분해
    try {
        const response = await fetchNearbyPlaces(selectedFacilityType, room.longitude, room.latitude);
        const places = response?.data || [];
      
        const emoji = facilityEmojiMap[selectedFacilityType] || "📍";
      
        const markers = places.map(({ latitude, longitude }) => {
          const content = `<div class="custom-facility-marker">${emoji}</div>`;
          return new window.kakao.maps.CustomOverlay({
            position: new window.kakao.maps.LatLng(latitude, longitude),
            content,
            yAnchor: 1,
          });
        });
      
        markers.forEach((m) => m.setMap(map));
        setNearbyMarkers(markers);
      } catch (error) {
        console.error("주변 시설 가져오기 실패:", error);
      }
  };

  const handleMouseLeave = () => {
    window.clearHoverMarker();
    nearbyMarkers.forEach((m) => m.setMap(null));
    if (circleOverlay) circleOverlay.setMap(null);
  };

  // 찜 토글 기능
  const toggleLike = async (room) => {
    const { propertyId } = room;
    if (user === null) return alert("로그인이 필요합니다.");
  
    try {
      // 프론트 상태 먼저 업데이트 (optimistic update)
      setRooms((prevRooms) => prevRooms.filter(r => r.propertyId !== propertyId));
  
      // 백엔드 요청 (실패 시 복구 로직을 넣어도 됨)
      await unlikeProperty(propertyId, user.idx);
  
    } catch (err) {
      console.error("찜 해제 실패:", err);
      alert("찜 해제에 실패했습니다.");
  
      // 실패 시 복구
      setRooms((prevRooms) => [...prevRooms, room]);
    }
  };

    // 페이지네이션 계산
    const totalPages = Math.ceil(rooms.length / pageSize);
    const currentRooms = rooms.slice((currentPage - 1) * pageSize, currentPage * pageSize);
    const goToPage = (page) => {
      window.scrollTo({ top: 0, behavior: "smooth" });
      setCurrentPage(page);
    };

    return (
        <>
        <div className="facility-type-sidebar">
            {[
                { key: "leisure", label: "여가" },
                { key: "health", label: "의료" },
                { key: "convenience", label: "편의" },
                { key: "transport", label: "교통" },
                { key: "cafe", label: "카페" },
                { key: "chicken", label: "치킨집" },
            ].map(({ key, label }) => (
                <button
                key={key}
                className={`facility-button ${
                    selectedFacilityType === key ? "active" : ""
                }`}
                onClick={() => selectFacilityType(key)}
                >
                {label}
                </button>
            ))}
        </div>
          {rooms.length === 0 ? (
            <div className="no-result-message">💔 찜한 매물이 없습니다.</div>
          ) : (
            <>
              {currentRooms.map((room) => (
                <div
                  key={room.propertyId}
                  className={`room-item ${selectedPropertyId === room.propertyId ? "selected" : ""}`}
                  onClick={() =>
                                  dispatch(
                                    setSelectedPropertyId(
                                      selectedPropertyId === room.propertyId
                                        ? null
                                        : room.propertyId
                                    )
                                  )
                                }
                  onMouseEnter={() => handleMouseEnter(room)}
                  onMouseLeave={handleMouseLeave}
                >
                  <img src={room.imageUrl || defaultImage} alt="매물 이미지" />
                  <div className="room-info">
                    <p className="room-title">{room.contractType} {room.price}</p>
                    <p className="room-description">{room.description}</p>
                    <p className="room-address">{room.address}</p>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleLike(room);
                      }}
                      className={`like-btn ${room.liked ? "liked" : ""}`}
                    >
                      {room.liked ? "❤️" : "🤍"}
                    </button>
                  </div>
                </div>
              ))}
      
              {/* ✅ 페이지네이션 전체를 포함하는 fragment 닫힘 필요 */}
              {rooms.length > pageSize && (
                <div className="pagination">
                  <button onClick={() => goToPage(1)} disabled={currentPage === 1}>&laquo;</button>
                  <button onClick={() => goToPage(currentPage - 1)} disabled={currentPage === 1}>&lsaquo;</button>
      
                  {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => (
                    <button
                      key={pageNum}
                      className={pageNum === currentPage ? "active" : ""}
                      onClick={() => goToPage(pageNum)}
                    >
                      {pageNum}
                    </button>
                  ))}
      
                  <button onClick={() => goToPage(currentPage + 1)} disabled={currentPage === totalPages}>&rsaquo;</button>
                  <button onClick={() => goToPage(totalPages)} disabled={currentPage === totalPages}>&raquo;</button>
                </div>
              )}
            </>
          )}

            {/* {selectedPropertyId && <RoomDetail propertyId={selectedPropertyId} />} */}
            
        </>
      );
};

export default ZzimList;
