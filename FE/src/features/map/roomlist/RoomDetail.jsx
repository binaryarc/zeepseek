import { useEffect, useState } from "react";
import "./RoomDetail.css";
import { getPropertyDetail } from "../../../common/api/api";
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

const RoomDetail = ({ propertyId }) => {
  const [detail, setDetail] = useState(null);
  const dispatch = useDispatch();
  useEffect(() => {
    console.log("받은 propertyId:", propertyId);
    const fetchDetail = async () => {
      console.log("RoomDetail에서 받은 propertyId:", propertyId);
      const data = await getPropertyDetail(propertyId);
      console.log("받은 상세 데이터:", data);
      if (data) setDetail(data);
    };
    fetchDetail();
  }, [propertyId]);

  const formatFee = (fee) => {
    if (!fee || fee === 0) return "없음";
    return `${Math.round(fee / 10000)}만원`;
  };

  if (!detail) return null; // 아직 로딩 중

  return (
    <div className="room-detail">
      <img
        src={close}
        alt="닫기"
        onClick={() => dispatch(setSelectedPropertyId(null))}
        className="close-btn"
      />
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
