import { useEffect, useState, useRef } from "react";
import "./RoomDetail.css";
import { getPropertyDetail, fetchCommuteTime } from "../../../common/api/api";
import defaultImage from "../../../assets/logo/192image.png";
// import { useDispatch } from "react-redux";
// import { setSelectedPropertyId } from "../../../store/slices/roomListSlice";
import date from "../../../assets/images/detail_png/date.png";
import floor from "../../../assets/images/detail_png/floor.png";
import rooms from "../../../assets/images/detail_png/room.png";
import size from "../../../assets/images/detail_png/size.png";
import direction from "../../../assets/images/detail_png/direction.png";
// import close from "../../../assets/images/detail_png/close.png";
// import phone from "../../../assets/images/detail_png/phone.png";
// import chat from "../../../assets/images/detail_png/chat.png";
import { useSelector } from "react-redux"; // ✅ 사용자 정보 가져오기

const RoomDetail = ({ propertyId: propId }) => {
  const [detail, setDetail] = useState(null);
  // const dispatch = useDispatch();
  const detailRef = useRef(null); // ✅ 이 ref로 RoomDetail 영역 추적

  const [commute, setCommute] = useState(null); // ⬅️ 통근 시간 상태
  const [roomDetail, setRoomDetail] = useState(null);

  const user = useSelector((state) => state.auth.user); // ⬅️ 사용자 정보

  const selectedId = useSelector((state) => state.roomList.selectedPropertyId);
  const source = useSelector((state) => state.roomList.selectedPropertySource);
  const propertyId = propId ?? selectedId;
  // const source = useSelector((state) => state.roomList.selectedPropertySource);

  const room = useSelector((state) =>
    source === "recommend"
      ? state.roomList.aiRecommendedList.find((r) => r.propertyId === propertyId)
      : state.roomList.rooms.find((r) => r.propertyId === propertyId)
  );

  // 다른 곳 클릭했을 때, RoomDetail 닫기
  // useEffect(() => {
  //   const handleClickOutside = (event) => {
  //     if (detailRef.current && !detailRef.current.contains(event.target)) {
  //       dispatch(setSelectedPropertyId(null)); // 닫기
  //     }
  //   };

  //   document.addEventListener("mousedown", handleClickOutside);
  //   return () => {
  //     document.removeEventListener("mousedown", handleClickOutside);
  //   };
  // }, []);

  useEffect(() => {
    if (!room) return;

    if (source === "recommend") {
      setRoomDetail(room);
    } else {
      const fetchDetail = async () => {
        try {
          const detail = await getPropertyDetail(room.propertyId);
          setRoomDetail({ ...room, ...detail });
        } catch (err) {
          console.error("매물 상세 정보 불러오기 실패:", err);
          setRoomDetail(room); // fallback
        }
      };
      fetchDetail();
    }
  }, [room, source]);

  useEffect(() => {
    if (!roomDetail || !user?.idx || !roomDetail.latitude || !roomDetail.longitude) return;

    const fetchCommute = async () => {
      try {
        const commuteData = await fetchCommuteTime({
          userId: user.idx,
          lat: roomDetail.latitude,
          lon: roomDetail.longitude,
        });
        setCommute(commuteData);
      } catch (error) {
        console.error("통근 시간 가져오기 실패:", error);
      }
    };
    fetchCommute();
  }, [roomDetail, user]);

  const formatFee = (fee) => {
    if (!fee || fee === 0) return "없음";
    return `${Math.round(fee / 10000)}만원`;
  };

  if (!roomDetail) return null;

  return (
    <div className="room-detail" ref={detailRef}>
      <div className="detail-scrollable">
        <img
          src={roomDetail.imageUrl || defaultImage}
          alt="매물 이미지"
          className="detail-image"
        />

        <div className="detail-info">
          <p className="detail-address">{roomDetail.address}</p>
          <h2>
            {roomDetail.contractType} {roomDetail.price}
          </h2>
          <p>관리비 {formatFee(roomDetail.maintenanceFee)}</p>
          <div className="detail-description">{roomDetail.description}</div>

          {commute ? (
            <div className="commute-section">
              <div className="commute-title">🚩 {commute.destination}</div>
              <div className="commute-all">
                <div className="commute-line">
                  <span>🚗</span>
                  <span>{commute.drivingTimeString}</span>
                </div>
                <div className="commute-line">
                  <span>🚇</span>
                  <span>{commute.transitTimeString}</span>
                </div>
                <div className="commute-line">
                  <span>🚶</span>
                  <span>{commute.walkingTimeString}</span>
                </div>
              </div>
            </div>
          ) : (
            <div className="commute-section">
              <div className="commute-title">
                <span className="spinner" /> 기준지와의 이동 시간 계산 중...
              </div>
            </div>
          )}

          <hr />

          <div className="detail-line">
            <img src={date} alt="날짜 아이콘" className="detail-icons" />
            <p>{roomDetail.moveInDate || "-"}</p>
          </div>

          <div className="detail-line">
            <img src={size} alt="면적 아이콘" className="detail-icons" />
            <p>{roomDetail.area || "-"}</p>
          </div>

          <div className="detail-line">
            <img src={floor} alt="층수 아이콘" className="detail-icons" />
            <p>{roomDetail.floorInfo || "-"}</p>
          </div>
          <div className="detail-line">
            <img src={rooms} alt="방욕실 아이콘" className="detail-icons" />
            <p>{roomDetail.roomBathCount || "-"}</p>
          </div>
          <div className="detail-line">
            <img src={direction} alt="방향" className="detail-icons" />
            <p>{roomDetail.direction || "-"}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RoomDetail;
