import React, { useState } from "react";
import "./RoomList.css";
import AiRecommend from "./ai_recommend/AiRecommend";
import { useDispatch, useSelector } from "react-redux";
import {
  setSelectedPropertyId,
  setCurrentPage,
  setSelectedRoomType,
  fetchRoomListByBounds,
  setRoomList,
} from "../../../store/slices/roomListSlice";
import defaultImage from "../../../assets/logo/192image.png";
import { FaHeart, FaRegHeart } from "react-icons/fa";
import { likeProperty, unlikeProperty } from "../../../common/api/api";

const RoomList = () => {
  const reduxSelectedRoomType = useSelector(
    (state) => state.roomList.selectedRoomType
  );

  const [selectedTab, setSelectedTab] = useState(
    reduxSelectedRoomType || "원룸/투룸"
  );

  const dispatch = useDispatch();
  const { currentGuName, currentDongName } = useSelector(
    (state) => state.roomList
  );

  let level = null;
  if (window.isMapReady && typeof window.map?.getLevel === "function") {
    level = window.map.getLevel();
  } else {
    console.warn("❗맵이 아직 준비되지 않았습니다.");
  }
  const user = useSelector((state) => state.auth.user);

  const toggleLike = async (room) => {
    const { propertyId } = room;
    if (user === null) return alert("로그인이 필요합니다.");

    try {
      if (room.liked) {
        await unlikeProperty(propertyId, user.idx);
      } else {
        await likeProperty(propertyId, user.idx);
      }

      // ✅ rooms 배열 업데이트
      const updatedRooms = rooms.map((r) =>
        r.propertyId === propertyId ? { ...r, liked: !r.liked } : r
      );
      dispatch(setRoomList(updatedRooms));
    } catch (err) {
      console.error("찜 토글 실패:", err);
    }
  };

  const handleTabClick = (tab) => {
    setSelectedTab(tab);
    dispatch(setSelectedRoomType(tab));

    // // ✅ AI 추천 탭이면 지도에서 grid 관련 레이어 제거
    // if (tab === "AI 추천") {
    //   if (window.clearPolygonLayer) {
    //     window.clearPolygonLayer(); // 예: 폴리곤 클러스터 제거
    //   }
    //   if (window.clearClusterMarkers) {
    //     window.clearClusterMarkers(); // 예: 클러스터 마커 제거
    //   }
    //   return;
    // }

    if (currentGuName && (currentDongName || currentDongName === "")) {
      console.log(tab);
      if (level < 6 && level > 3) {
        dispatch(
          fetchRoomListByBounds({
            guName: currentGuName,
            dongName: currentDongName,
            filter: tab,
          })
        );
      } else if (level >= 6) {
        dispatch(
          fetchRoomListByBounds({
            guName: currentGuName,
            dongName: "",
            filter: tab,
          })
        );
      }

      console.log(currentDongName, currentGuName, "실행돼썽용용");
    }
  };
  // ✅ Redux 상태에서 매물 리스트, 로딩 상태 가져오기
  const { rooms, loading, keyword, selectedPropertyId, currentPage, pageSize } =
    useSelector((state) => state.roomList);

  const totalPages = Math.ceil(rooms.length / pageSize);
  const maxPageButtons = 3; // 페이지 버튼 최대 노출 수
  const startPage = Math.max(1, currentPage - Math.floor(maxPageButtons / 2));
  const endPage = Math.min(totalPages, startPage + maxPageButtons - 1);
  const currentRooms = rooms.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  );

  const goToPage = (page) => {
    window.scrollTo({ top: 0, behavior: "smooth" });
    dispatch(setCurrentPage(page));
  };

  // const handlePageChange = (page) => {
  //   window.scrollTo({ top: 0, behavior: "smooth" });
  //   dispatch(setCurrentPage(page));
  // };

  return (
    <div className="room-list">
      <nav className="room-type">
        {["원룸/투룸", "오피스텔", "주택/빌라", "AI 추천"].map((tab) => (
          <span
            key={tab}
            className={selectedTab === tab ? "active-tab" : ""}
            onClick={() => handleTabClick(tab)}
          >
            {tab}
          </span>
        ))}
      </nav>

      {selectedTab === "AI 추천" ? (
        <AiRecommend />
      ) : loading ? (
        <div className="loading-message">🔄 매물 불러오는 중...</div>
      ) : currentRooms.length === 0 ? (
        <div className="no-result-message">
          ❗ "{keyword}"에 대한 매물이 없습니다.
        </div>
      ) : (
        <>
          {currentRooms.map((room) => (
            <div
              key={room.propertyId}
              className={`room-item ${
                selectedPropertyId === room.propertyId ? "selected" : ""
              }`}
              onClick={() =>
                dispatch(
                  setSelectedPropertyId(
                    selectedPropertyId === room.propertyId
                      ? null
                      : room.propertyId
                  )
                )
              }
              onMouseEnter={() => {
                if (room.latitude && room.longitude) {
                  window.setHoverMarker(room.latitude, room.longitude);
                }
              }}
              onMouseLeave={() => {
                window.clearHoverMarker();
              }}
            >
              <img src={room.imageUrl || defaultImage} alt="매물 이미지" />
              <div className="room-info">
                <p className="room-title">
                  {room.contractType} {room.price}
                </p>
                <p className="room-description">{room.description}</p>
                <p className="room-address">{room.address}</p>
                <button onClick={() => toggleLike(room)} className="like-btn">
                  {room.liked ? <FaHeart color="red" /> : <FaRegHeart />}
                </button>
              </div>
            </div>
          ))}

          <div className="pagination">
            <button onClick={() => goToPage(1)} disabled={currentPage === 1}>
              &laquo;
            </button>
            <button
              onClick={() => goToPage(currentPage - 1)}
              disabled={currentPage === 1}
            >
              &lsaquo;
            </button>

            {Array.from(
              { length: endPage - startPage + 1 },
              (_, i) => startPage + i
            ).map((num) => (
              <button
                key={num}
                className={num === currentPage ? "active" : ""}
                onClick={() => goToPage(num)}
              >
                {num}
              </button>
            ))}

            <button
              onClick={() => goToPage(currentPage + 1)}
              disabled={currentPage === totalPages}
            >
              &rsaquo;
            </button>
            <button
              onClick={() => goToPage(totalPages)}
              disabled={currentPage === totalPages}
            >
              &raquo;
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default RoomList;
