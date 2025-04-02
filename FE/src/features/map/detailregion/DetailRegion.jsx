// map/detailregion/DetailRegion.jsx
import "./DetailRegion.css";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { fetchDongDetail } from "../../../common/api/api";
import Community from "./Community";
import { likeDong, unlikeDong } from "../../../store/slices/dongLikeSlice";
import { unlikeDongApi, likeDongApi } from "../../../common/api/api";
import { fetchDongComments } from "../../../common/api/api";

const getTop3Scores = (dongData) => {
  const categories = {
    convenience: { label: "편의", icon: "🛍️" },
    transport: { label: "교통", icon: "🚇" },
    leisure: { label: "여가", icon: "🎮" },
    health: { label: "건강", icon: "🏥" },
    restaurant: { label: "식당", icon: "🍽️" },
    mart: { label: "마트", icon: "🛒" },
    safe: { label: "안전", icon: "🛡️" },
  };

  return Object.entries(categories)
    .map(([key, { label, icon }]) => ({
      key,
      label,
      icon,
      value: dongData[key],
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 3);
};

const DetailRegion = () => {
  const dongId = useSelector((state) => state.roomList.currentDongId); // Redux에서 가져오기
  const liked = useSelector((state) => {
    const result = state.dongLike?.[dongId];
    console.log("💚 현재 동 ID:", dongId, "찜 여부:", result);
    console.log('아아', state)
    return result === true;
  });
  const user = useSelector((state) => state.auth.user);
  const [dongData, setDongData] = useState(null);
  const [comments, setComments] = useState([]);
  const [showCommunity, setShowCommunity] = useState(false);  const dispatch = useDispatch();

  useEffect(() => {
    if (!dongId) return;

    const loadDongDetail = async () => {
      const data = await fetchDongDetail(dongId);
      setDongData(data);

      const commentData = await fetchDongComments(dongId); // ⬅️ 댓글도 같이 불러오기
      setComments(commentData);
    };

    loadDongDetail();
  }, [dongId]);

  const handleToggleZzim = async () => {
    if (!user) return alert("로그인이 필요합니다!");

    try {
      if (liked) {
        await unlikeDongApi(dongId, user.idx);
        dispatch(unlikeDong(dongId));
        console.log("하트 눌러졌으요");
      } else {
        await likeDongApi(dongId, user.idx);
        dispatch(likeDong(dongId));
        console.log("하트 빠졌으요");
      }
    } catch (err) {
      console.error("찜 토글 실패:", err);
    }
  };

  if (!dongData) {
    return (
      <div className="detail-region-box">
        <div className="spinner-wrapper">
          <div className="spinner"></div>
        </div>
      </div>
    );
  }

  const topScores = getTop3Scores(dongData);

  return (
    <div className="detail-region-box">
      {!showCommunity ? (
        <>
          <button
        className={`detail-zzim-button ${liked ? "liked" : ""}`}
        onClick={handleToggleZzim}
      >
        {liked ? "❤️" : "🤍"}
      </button>
      <h3 className="dong-title">
            {dongData.guName} {dongData.name}
          </h3>

          <div className="score-bars">
            {topScores.map(({ label, icon, value }) => (
              <div key={label} className="score-item">
                <span className="score-label">
              {icon} {label}
            </span>
                <div className="score-bar-wrapper">
                  <div className="score-bar" style={{ width: `${value}%` }} />
                </div>
              </div>
            ))}
          </div>

          <p className="summary-title">📍 동네 요약</p>
          <p className="summary">{dongData.summary}</p>

          <p className="comment-preview-title">💬 커뮤니티</p>
          {/* 💬 최신 댓글 미리보기 */}
          <div className="comment-preview">
           
            {comments.length > 0 ? (
              <>
                <p className="comment-content">"{comments[0].content}"</p>
                {/* <p className="comment-meta">- {comments[0].nickname}</p> */}
              </>
            ) : (
              <p className="comment-content">아직 댓글이 없어요.</p>
            )}
            <hr />
            <button className="comment-more-btn" onClick={() => setShowCommunity(true)}>
              댓글 더 보기 ⟫
            </button>
          </div>
        </>
      ) : (
        <Community
          dongId={dongId}
          dongName={dongData.name}
          guName={dongData.guName}
          onClose={() => setShowCommunity(false)}
        />
      )}
    </div>
  );
};

export default DetailRegion;
