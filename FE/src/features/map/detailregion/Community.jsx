import { useEffect, useState } from "react";
import { fetchDongComments } from "../../../common/api/api";
import { useSelector } from "react-redux";
import { postDongComment } from "../../../common/api/api";
import back from "../../../assets/images/back.png"
import "./Community.css"
import { deleteDongComment } from "../../../common/api/api";

const Community = ({ dongId, dongName, guName, onClose }) => {
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState("");
    const [loading, setLoading] = useState(false);
    const accessToken = useSelector((state) => state.auth.accessToken);
    const nickname = useSelector((state) => state.auth.user?.nickname);
  
    useEffect(() => {
      const loadComments = async () => {
        const res = await fetchDongComments(dongId);
        console.log("🧾 댓글 확인:", res); // 👈 이거 찍어보세요!
        setComments(res);
      };
      loadComments();
    }, [dongId]);
    
    const handlePost = async () => {
        if (!newComment.trim()) return;
        if (!accessToken || !nickname) {
          alert("로그인이 필요합니다.");
          return;
        }
    
        try {
          setLoading(true);
          await postDongComment(dongId, nickname, newComment, accessToken);
          const updated = await fetchDongComments(dongId);
          setComments(updated);
          setNewComment("");
        } catch {
          alert("댓글 작성에 실패했어요 😢");
        } finally {
          setLoading(false);
        }
      };
    
      const handleDelete = async (commentId) => {
        if (!accessToken) return alert("로그인이 필요합니다!");
        const confirmDelete = window.confirm("댓글을 삭제하시겠습니까?");
        if (!confirmDelete) return;
      
        try {
          await deleteDongComment(dongId, commentId, accessToken);
          const updated = await fetchDongComments(dongId);
          setComments(updated);
        } catch {
          alert("삭제에 실패했어요 😢");
        }
      };

    return (
      <div
          className="community-box"
          onWheel={(e) => e.stopPropagation()}
        >
        <div className="community-header">
          <h4>{guName} {dongName}</h4>
          <img src={back} alt="" onClick={onClose} className="community-back"/>
        </div>
        <ul className="dong-comment-list">
          {comments.map((c, i) => (
            <li key={i} className="dong-comment-item">
              <p className="dong-comment-content">{c.content}</p>
              <p className="dong-comment-meta">
                - {c.nickname ?? "익명"} | {new Date(c.createdAt).toLocaleDateString()}
                {/* 🔐 로그인 사용자와 닉네임 일치 시에만 보여주기 */}
                {c.nickname === nickname && (
                  <button
                    className="delete-btn"
                    onClick={() => handleDelete(c.commentId)}
                  >
                    삭제
                  </button>
                )}
              </p>
            </li>
          ))}
        </ul>
        <div className="dong-comment-form">
        <input
            type="text"
            placeholder="댓글을 입력하세요"
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                handlePost();
              }
            }}
            disabled={loading}
          />
                  <button onClick={handlePost} disabled={loading}>
            등록
        </button>
        </div>
      </div>
    );
  };
  
  export default Community;
  