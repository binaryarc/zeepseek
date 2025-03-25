import { useEffect, useState } from "react";
import "./RoomDetail.css";
import { getPropertyDetail } from "../../../common/api/api";
import defaultImage from "../../../assets/logo/192image.png";
import { useDispatch } from "react-redux";
import { setSelectedPropertyId } from "../../../store/slices/roomListSlice";

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
  
    if (!detail) return null; // 아직 로딩 중
  
    return (
      <div className="room-detail">
        <button className="close-btn" onClick={() => dispatch(setSelectedPropertyId(null))}>X</button>        
        <img src={detail.imageUrl || defaultImage} alt="매물 이미지" />
        <div className="detail-info">
          <h3>{detail.contractType} {detail.price}</h3>
          <p>{detail.address}</p>
          <p>{detail.description}</p>
          <p>{detail.area}</p>
          <p>{detail.floorInfo}</p>
          <p>{detail.roomBathCount}</p>
          <p>{detail.direction}</p>
          <p>관리비: {detail.maintenanceFee?.toLocaleString() || "없음"}원</p>
          <p>입주 가능일: {detail.moveInDate}</p>
        </div>
      </div>
    );
  };
  
  export default RoomDetail;
