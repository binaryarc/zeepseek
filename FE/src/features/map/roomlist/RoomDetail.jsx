import { useEffect, useState, useRef } from "react";
import "./RoomDetail.css";
import { getPropertyDetail, fetchCommuteTime } from "../../../common/api/api";
import defaultImage from "../../../assets/logo/192image.png";
import { useDispatch } from "react-redux";
import { setSelectedPropertyId } from "../../../store/slices/roomListSlice";
import date from "../../../assets/images/detail_png/date.png";
import floor from "../../../assets/images/detail_png/floor.png";
import room from "../../../assets/images/detail_png/room.png";
import size from "../../../assets/images/detail_png/size.png";
import direction from "../../../assets/images/detail_png/direction.png";
import close from "../../../assets/images/detail_png/close.png";
// import phone from "../../../assets/images/detail_png/phone.png";
// import chat from "../../../assets/images/detail_png/chat.png";
import { useSelector } from "react-redux"; // ✅ 사용자 정보 가져오기

const RoomDetail = ({ propertyId }) => {
  const [detail, setDetail] = useState(null);
  const dispatch = useDispatch();
  const detailRef = useRef(null); // ✅ 이 ref로 RoomDetail 영역 추적

  const [commute, setCommute] = useState(null); // ⬅️ 통근 시간 상태

  const user = useSelector((state) => state.auth.user); // ⬅️ 사용자 정보

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
    console.log("받은 propertyId:", propertyId);
    const fetchDetail = async () => {
      console.log("RoomDetail에서 받은 propertyId:", propertyId);
      const data = await getPropertyDetail(propertyId);
      console.log("받은 상세 데이터:", data);
      if (data) setDetail(data);
      console.log("요청 보낼 정보:", user?.idx, data?.latitude, data?.longitude);

      // ✅ 통근 시간 요청 (userId + 매물 좌표)
      if (user.idx && data.latitude && data.longitude) {
        console.log('여기옴?')
        const commuteData = await fetchCommuteTime({
          userId: user.idx,
          lat: data.latitude,
          lon: data.longitude,
        });
        setCommute(commuteData);
      }
    };
    fetchDetail();
  }, [propertyId]);

  const formatFee = (fee) => {
    if (!fee || fee === 0) return "없음";
    return `${Math.round(fee / 10000)}만원`;
  };

  if (!detail) return null; // 아직 로딩 중

  return (
    <div className="room-detail" ref={detailRef}>
      {/* <img
        src={close}
        alt="닫기"
        onClick={() => dispatch(setSelectedPropertyId(null))}
        className="close-btn"
      /> */}
      <div className="detail-scrollable">
        <img
          src={detail.imageUrl || defaultImage}
          alt="매물 이미지"
          className="detail-image"
        />

        <div className="detail-info">
          <p className="detail-address">{detail.address}</p>
          <h2>
            {detail.contractType} {detail.price}
          </h2>
          <p>관리비 {formatFee(detail.maintenanceFee)}</p>
          <div className="detail-description">{detail.description}</div>
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
            <p>{detail.moveInDate || "-"}</p>
          </div>

          <div className="detail-line">
            <img src={size} alt="면적 아이콘" className="detail-icons" />
            <p>{detail.area || "-"}</p>
          </div>

          <div className="detail-line">
            <img src={floor} alt="층수 아이콘" className="detail-icons" />
            <p>{detail.floorInfo || "-"}</p>
          </div>
          <div className="detail-line">
            <img src={room} alt="방욕실 아이콘" className="detail-icons" />
            <p>{detail.roomBathCount || "-"}</p>
          </div>
          <div className="detail-line">
            <img src={direction} alt="방향" className="detail-icons" />
            <p>{detail.direction || "-"}</p>
          </div>
          {/* <div className="detail-fixed-footer">
            <img src={phone} alt="전화" />
            <img src={chat} alt="메시지" />
          </div> */}
        </div>
      </div>
    </div>
  );
};

export default RoomDetail;
