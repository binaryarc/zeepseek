import React from "react";
import "./AiRecommendList.css";
import defaultImg from "../../../../../assets/logo/512image.png";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  LabelList,
  Legend,
} from "recharts";
import date from "../../../../../assets/images/detail_png/date.png";
import floor from "../../../../../assets/images/detail_png/floor.png";
import roomImg from "../../../../../assets/images/detail_png/room.png";
import size from "../../../../../assets/images/detail_png/size.png";
import direction from "../../../../../assets/images/detail_png/direction.png";
// import close from "../../../assets/images/detail_png/close.png";

const AiRecommendList = ({ room, values, onClose }) => {
  if (!room && !values) return null;

  const categories = [
    { key: "cafeScore", label: "카페" },
    { key: "chickenScore", label: "치킨" },
    { key: "convenienceScore", label: "편의" },
    { key: "healthScore", label: "의료" },
    { key: "leisureScore", label: "여가" },
    { key: "transportScore", label: "교통" },
  ];

  const chartData = categories.map(({ key, label }) => ({
    name: label,
    사용자점수: -(values[key] || 0),
    매물점수: (room[key] || 0) , // 👈 반드시 *100 해줘야 비교가 정확함
  }));

  const formatFee = (fee) => {
    if (!fee || fee === 0) return "없음";
    return `${Math.round(fee / 10000)}만원`;
  };
    

  return (
    <div className="recommend-modal-overlay">
      <div className="recommend-modal">
        <button className="recommend-modal-close-btn" onClick={onClose}>
          &times;
        </button>
        <div className="modal-body">
          {/* 왼쪽: 매물 정보 */}
          <div className="recommend-modal-left-section">
            <div className="modal-image-section">
              <img
                src={room.imageUrl || defaultImg}
                alt="매물 이미지"
                className="modal-image"
              />
            </div>
          <div className="recommend-detail-info">
              <p className="detail-address">{room.address}</p>
              <h2>
                {room.contractType} {room.price}
              </h2>
              <p>관리비 {formatFee(room.maintenanceFee)}</p>
              <div className="detail-description">{room.description}</div>
              <hr />
              <div className="recommend-room-detail">
                <div className="recommend-detail-line">
                  <img src={date} alt="날짜 아이콘" className="detail-icons" />
                  <p>{room.moveInDate || "-"}</p>
                </div>

                <div className="recommend-detail-line">
                  <img src={size} alt="면적 아이콘" className="detail-icons" />
                  <p>{room.area || "-"}</p>
                </div>

                <div className="recommend-detail-line">
                  <img src={floor} alt="층수 아이콘" className="detail-icons" />
                  <p>{room.floorInfo || "-"}</p>
                </div>
                <div className="recommend-detail-line">
                  <img src={roomImg} alt="방욕실 아이콘" className="detail-icons" />
                  <p>{room.roomBathCount || "-"}</p>
                </div>
                <div className="recommend-detail-line">
                  <img src={direction} alt="방향" className="detail-icons" />
                  <p>{room.direction || "-"}</p>
                </div>
                {/* <div className="detail-fixed-footer">
                  <img src={phone} alt="전화" />
                  <img src={chat} alt="메시지" />
                </div> */}
              </div>
            </div>
          </div>

          {/* 오른쪽: 그래프 */}
          <div className="modal-score-section" style={{ width: "50%", height: "100%" }}>
            
            <p><strong>사용자와 매물의 점수 비교</strong></p>
            <ResponsiveContainer width="100%" height={300}>
  <BarChart
    layout="vertical"
    data={chartData}
    margin={{ top: 20, right: 40, left: 40, bottom: 20 }}
    barCategoryGap="20%" // 바 간격 조절
  >
    <XAxis
      type="number"
      domain={[0, 100]}
      tickFormatter={(value) => `${value}`}
    />
    <YAxis
      type="category"
      dataKey="name"
      axisLine={false}
      tickLine={false}
      tick={{ fontWeight: "bold", fontSize: 14 }}
    />
    <Tooltip
      formatter={(value, name) => [`${value.toFixed(2)}점`, name]}
    />
    <Legend
      wrapperStyle={{ marginTop: 10 }}
      payload={[
        { value: '사용자점수', type: 'square', color: '#8884d8' },
        { value: '매물점수', type: 'square', color: '#82ca9d' },
      ]}
    />
    <Bar
      dataKey="사용자점수"
      fill="#8884d8"
      barSize={20}
      radius={[0, 10, 10, 0]}
    >
      <LabelList
        dataKey="사용자점수"
        position="right"
        formatter={(value) => `${value}점`}
        fill="#ffffff"
      />
    </Bar>
    <Bar
      dataKey="매물점수"
      fill="#82ca9d"
      barSize={20}
      radius={[10, 0, 0, 10]}
    >
      <LabelList
        dataKey="매물점수"
        position="right"
        formatter={(value) => `${value.toFixed(2)}점`}
        fill="#ffffff"
      />
    </Bar>
  </BarChart>
</ResponsiveContainer>


            <p><strong>내 취향과의 유사도:</strong> {room.similarity}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AiRecommendList;
