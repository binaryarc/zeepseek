import { useEffect, useState, useRef } from "react";
import { fetchDongComments } from "../../../common/api/api";
import { useSelector } from "react-redux";
import { postDongComment } from "../../../common/api/api";
import back from "../../../assets/images/back.png";
import "./Community.css";
import { deleteDongComment } from "../../../common/api/api";
import AlertModal from "../../../common/component/AlertModal";

const Community = ({ dongId, dongName, guName, onClose }) => {
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState("");
  const [loading, setLoading] = useState(false);
  const accessToken = useSelector((state) => state.auth.accessToken);
  const nickname = useSelector((state) => state.auth.user?.nickname);
  const commentListRef = useRef(null);
  const [contextMenu, setContextMenu] = useState({
    visible: false,
    x: 0,
    y: 0,
    commentId: null,
  });
  const [isChanged, setIsChanged] = useState(false);
  const [alertMessage, setAlertMessage] = useState("");
  
  useEffect(() => {
    const loadComments = async () => {
      const res = await fetchDongComments(dongId);
      setComments(res);
    };
    loadComments();
  }, [dongId]);

  useEffect(() => {
    if (commentListRef.current) {
      commentListRef.current.scrollTop = commentListRef.current.scrollHeight;
    }
  }, [comments]);

  const handlePost = async () => {
    if (!newComment.trim()) return;
    if (!accessToken || !nickname) {
      setAlertMessage("로그인이 필요합니다.");
      return;
    }
    
    try {
      setLoading(true);
      await postDongComment(dongId, nickname, newComment, accessToken);
      const updated = await fetchDongComments(dongId);
      setComments(updated);
      setNewComment("");
      setIsChanged(true);
    } catch {
      setAlertMessage("댓글 작성에 실패했어요 😢");
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
      setIsChanged(true);
    } catch {
      setAlertMessage("삭제에 실패했어요 😢");
    }
  };

  const handleRightClick = (e, commentId) => {
    e.preventDefault();
    setContextMenu({ visible: true, x: e.clientX, y: e.clientY, commentId });
  };

  const handleClickOutside = () =>
    setContextMenu({ visible: false, x: 0, y: 0, commentId: null });

  useEffect(() => {
    window.addEventListener("click", handleClickOutside);
    return () => window.removeEventListener("click", handleClickOutside);
  }, []);


  useEffect(() => {
    const handleMouseDown = (e) => {
      // 삭제 버튼(span.bubble-delete) 클릭한 경우는 무시
      if (
        e.target.classList.contains("bubble-delete") || 
        e.target.closest(".bubble-delete")
      ) {
        return; // ❌ 닫지 마!
      }
  
      // 마우스 왼쪽 버튼 클릭일 때만 닫기
      if (e.button === 0) {
        handleClickOutside();
      }
    };
  
    window.addEventListener("mousedown", handleMouseDown);
    return () => window.removeEventListener("mousedown", handleMouseDown);
  }, []);

  return (
    <div className="community-box" onWheel={(e) => e.stopPropagation()}>
      <div className="community-header">
        <h4>
          {guName} {dongName}
        </h4>
        <img src={back} alt="" onClick={() => onClose(isChanged)} className="community-back" />
      </div>
      <ul className="dong-comment-list" ref={commentListRef}>
        {comments.map((c, i) => {
          const isMine = c.nickname === nickname;
          const createdDate = new Date(c.createdAt);
          const kstDate = new Date(createdDate.getTime() + 9 * 60 * 60 * 1000);
          const dateStr = kstDate.toISOString().split("T")[0]; // yyyy-mm-dd

          const prevDateStr =
            i > 0
              ? new Date(
                  new Date(comments[i - 1].createdAt).getTime() +
                    9 * 60 * 60 * 1000
                )
                  .toISOString()
                  .split("T")[0]
              : null;

          const showDateLabel = i === 0 || dateStr !== prevDateStr;

          return (
            <li key={i} className="dong-comment-item">
              {showDateLabel && (
                <div className="comment-date-label">{dateStr}</div>
              )}
              <span
                className={`nickname-label ${
                  isMine ? "nickname-right" : "nickname-left"
                }`}
              >
                {c.nickname ?? "익명"}
              </span>
              <div
                className={`bubble-wrapper ${
                  isMine ? "my-wrapper" : "other-wrapper"
                }`}
              >
                <div
                  className={`bubble ${
                    isMine ? "my-comment" : "other-comment"
                  }`}
                  onContextMenu={(e) => {
                    if (isMine) {
                      handleRightClick(e, c.commentId);
                    }
                  }}
                >
                  {c.content}
                </div>

                <div className={`bubble-meta ${isMine ? "meta-right" : "meta-left"}`}>
                {contextMenu.visible && contextMenu.commentId === c.commentId ? (
                  <span
                    className="bubble-delete"
                    onClick={(e) => {
                      e.stopPropagation(); 
                      handleDelete(c.commentId);
                      handleClickOutside();
                    }}
                  >
                    삭제
                  </span>
                ) : (
                  <span className="bubble-time">
                    {kstDate.toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                      hour12: false,
                    })}
                  </span>
                )}

                </div>
              </div>
            </li>
          );
        })}
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
      {alertMessage && (
        <AlertModal
          message={alertMessage}
          buttonText="확인"
          onClose={() => setAlertMessage("")}
        />
      )}
    </div>
    
  );
};

export default Community;
