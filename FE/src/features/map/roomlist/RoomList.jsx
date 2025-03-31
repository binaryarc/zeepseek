import React, { useState } from "react";
import "./RoomList.css";
import AiRecommend from "./ai_recommend/AiRecommend";
import { useDispatch, useSelector } from "react-redux";
import {
  setSelectedPropertyId,
  setCurrentPage,
  setSelectedRoomType,
  fetchRoomListByBounds,
} from "../../../store/slices/roomListSlice";
import defaultImage from "../../../assets/logo/192image.png";

const RoomList = () => {
  const [selectedTab, setSelectedTab] = useState("원룸/투룸");
  const dispatch = useDispatch();
  const { currentGuName, currentDongName } = useSelector(
    (state) => state.roomList
  );

  const level = window.map?.getLevel();

  const handleTabClick = (tab) => {
    setSelectedTab(tab);
    dispatch(setSelectedRoomType(tab));

    if (tab === "AI 추천") return;

    if (currentGuName && (currentDongName || currentDongName === "")) {
      console.log(tab)
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

      console.log(currentDongName, currentGuName, '실행돼썽용용')
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
        {["원룸/투룸", "오피스텔", "주택/빌라"].map((tab) => (
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
