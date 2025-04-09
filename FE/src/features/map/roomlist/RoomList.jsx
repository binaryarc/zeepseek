import React, { useState, useEffect } from "react";
import "./RoomList.css";
import AiRecommend from "./ai_recommend/AiRecommend";
import ZzimList from "./zzim_list/ZzimList";
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
import { useRef } from "react";

const RoomList = () => {
  const roomListRef = useRef(null);

  const reduxSelectedRoomType = useSelector(
    (state) => state.roomList.selectedRoomType
  );

  const [selectedTab, setSelectedTab] = useState(
    reduxSelectedRoomType || "원룸/투룸"
  );

  const clearAllMapOverlays = () => {
    // 공통 마커 제거 함수
    if (window.clearHoverMarker) {
      window.clearHoverMarker();
    }
  
    // 다른 컴포넌트에서 만든 circle/marker ref는 접근 불가 → 전역에서 관리하거나,
    // 각 컴포넌트 언마운트 시 자동 정리되도록 해야 함
  };
  

  useEffect(() => {
    setSelectedTab(reduxSelectedRoomType);
  }, [reduxSelectedRoomType]);

  const dispatch = useDispatch();
  const {
    currentGuName,
    currentDongName,
    rooms,
    loading,
    keyword,
    selectedPropertyId,
    currentPage,
    pageSize,
  } = useSelector((state) => state.roomList);

  useEffect(() => {
    if (!selectedPropertyId || rooms.length === 0) return;
    console.log("여기야?");

    const index = rooms.findIndex((r) => r.propertyId === selectedPropertyId);
    if (index === -1) return;

    const page = Math.floor(index / pageSize) + 1;
    if (currentPage !== page) {
      dispatch(setCurrentPage(page));
    }
  }, [selectedPropertyId, rooms, pageSize, currentPage]);

  useEffect(() => {
    if (!selectedPropertyId) return;

    const el = document.querySelector(`[data-id='${selectedPropertyId}']`);
    if (el) {
      el.scrollIntoView({
        behavior: "smooth",
        block: "center",
      });
    }
  }, [selectedPropertyId, currentPage]);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (roomListRef.current && !roomListRef.current.contains(e.target)) {
        // 외부 클릭이면 RoomDetail 닫기
        dispatch(setSelectedPropertyId(null));
      }
    };
    if (selectedPropertyId !== null) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [selectedPropertyId]);

  let level = 5;
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

    // 로그인 필요 탭일 경우 확인
    const isAuthRequired = tab === "ZEEPSEEK추천" || tab === "찜";
    if (isAuthRequired && !user?.idx) {
      alert("로그인이 필요합니다.");
      return;
    }
    
    // 🔥 탭 바뀌면 지도 마커 정리!
    if (window.clearHoverMarker) window.clearHoverMarker();
    dispatch(setSelectedPropertyId(null));
    setSelectedTab(tab);
    dispatch(setSelectedRoomType(tab));

    if (currentGuName && (currentDongName || currentDongName === "")) {
      console.log(tab);
      if (level < 6 && level > 3) {
        console.log("아아아아아아", user.idx);
        dispatch(
          fetchRoomListByBounds({
            guName: currentGuName,
            dongName: currentDongName,
            filter: tab,
            userId: user?.idx ?? null,
          })
        );
      } else if (level >= 6) {
        dispatch(
          fetchRoomListByBounds({
            guName: currentGuName,
            dongName: "",
            filter: tab,
            userId: user?.idx ?? null,
          })
        );
      }
      console.log(currentDongName, currentGuName, "실행돼썽용용");
    }
  };

  // Modified:
  // keyword가 비어있지 않고 (null이 아니고) keyword와 currentDongName이 다르면 무조건 currentDongName 사용
  // 그렇지 않으면 keyword가 있으면 keyword, 없으면 currentDongName 사용
  const displayKeyword =
    keyword && keyword.trim() !== "" && keyword !== currentDongName
      ? currentDongName
      : keyword && keyword.trim() !== ""
      ? keyword
      : currentDongName;

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
    dispatch(setSelectedPropertyId(null)); // ✅ 현재 선택된 매물 초기화
    dispatch(setCurrentPage(page));
  };

  return (
    <div className="room-list" ref={roomListRef}>
      <nav className="room-type">
        {["원룸/투룸", "오피스텔", "주택/빌라", "ZEEPSEEK추천", "찜"].map((tab) => (
          <span
            key={tab}
            className={selectedTab === tab ? "active-tab" : ""}
            onClick={() => handleTabClick(tab)}
          >
            {tab}
          </span>
        ))}
      </nav>

      {selectedTab === "ZEEPSEEK추천" ? (
        <AiRecommend />
      ) : selectedTab === "찜" ? (
        <ZzimList />
      ) : loading ? (
        <div className="loading-message">
          <span className="room-spinner" /> 매물 로딩 중...
        </div>
      ) : currentRooms.length === 0 ? (
        <div className="no-result-message">
          ❗ "{displayKeyword}"에 대한 매물이 없습니다.
        </div>
      ) : (
        <>
          {currentRooms.map((room) => (
            <div
              key={room.propertyId}
              data-id={room.propertyId} // ✅ 여기!
              className={`room-item ${
                selectedPropertyId === room.propertyId ? "selected" : ""
              }`}
              onClick={() => {
                if (room.latitude && room.longitude) {
                  window.setHoverMarker(room.latitude, room.longitude);
                }
                if (selectedPropertyId === room.propertyId) {
                  console.log("끕니다");
                  dispatch(setSelectedPropertyId(null)); // 다시 클릭 → 닫기
                  window.clearHoverMarker();
                } else {
                  console.log(selectedPropertyId, room.propertyId);
                  console.log("켜요요");
                  dispatch(setSelectedPropertyId(room.propertyId)); // 다른 매물 → 열기
                }
              }}
              // onMouseEnter={() => {
              //   if (room.latitude && room.longitude) {
              //     window.setHoverMarker(room.latitude, room.longitude);
              //   }
              // }}
              // onMouseLeave={() => {
              //   window.clearHoverMarker();
              // }}
            >
              <img src={room.imageUrl || defaultImage} alt="매물 이미지" />
              <div className="room-info">
                <p className="room-title">
                  {room.contractType} {room.price}
                </p>
                <p className="room-description">{room.description}</p>
                <p className="room-address">{room.address}</p>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleLike(room);
                  }}
                  className={`like-btn ${room.liked ? "liked" : ""}`} // liked 상태에 따라 클래스를 추가
                >
                  {room.liked ? "❤️" : "🤍"}
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
