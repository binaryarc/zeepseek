import { useEffect, useState } from "react";
import { fetchDongComments } from "../../../common/api/api";
import { useSelector } from "react-redux";
import { postDongComment } from "../../../common/api/api";
import back from "../../../assets/images/back.png"
import "./Community.css"
const Community = ({ dongId, dongName, guName, onClose }) => {
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState("");
    const [loading, setLoading] = useState(false);
    const accessToken = useSelector((state) => state.auth.accessToken);
    const nickname = useSelector((state) => state.auth.user?.nickname);
  
    useEffect(() => {
      const loadComments = async () => {
        const res = await fetchDongComments(dongId);
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

    return (
      <div className="community-box">
        <div className="community-header">
          <h4>{guName} {dongName}</h4>
          <img src={back} alt="" onClick={onClose} className="community-back"/>
        </div>
        <ul className="comment-list">
          {comments.map((c, i) => (
            <li key={i} className="comment-item">
              <p className="comment-content">{c.content}</p>
              <p className="comment-meta">- {c.nickname} | {new Date(c.createdAt).toLocaleDateString()}</p>
            </li>
          ))}
        </ul>
        <div className="comment-form">
        <input
            type="text"
            placeholder="댓글을 입력하세요"
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
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
  