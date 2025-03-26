// map/detailregion/DetailRegion.jsx
import "./DetailRegion.css";

const DetailRegion = ({ dongName }) => {
  return (
    <div className="detail-region-box">
      <h4>{dongName}</h4>
      <p>동네 설명</p>
    </div>
  );
};

export default DetailRegion;
