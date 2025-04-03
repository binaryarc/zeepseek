import "./MyPage.css";
import Navbar from "../../common/navbar/Navbar";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { logout } from "../../store/slices/authSlice";
import { logoutOAuth } from "../../common/api/authApi";
import { deleteOAuth } from "../../common/api/authApi";

const MyPage = () => {
  const navigate = useNavigate();
  const user =useSelector((state) => state.auth.user)
  const accessToken = useSelector((state) => state.auth.accessToken)
  const social = user?.provider;
  const nickname = user?.nickname || '로그인 유저';
  const dispatch = useDispatch()

  const handleLogout = async () => {
    try {
      await logoutOAuth(accessToken);      // 백엔드에 로그아웃 요청
      dispatch(logout());                  // Redux 상태 초기화
      navigate("/main");                 // 로그인 페이지로 이동 (선택)
    } catch (err) {
      console.error("로그아웃 실패", err);
    }
  };

  const handleUserDelete = async () => {
    const confirmDelete = window.confirm("정말 탈퇴하시겠습니까? 😢");
  
    if (!confirmDelete) return; // ❌ 취소하면 함수 종료
  
    try {
      await deleteOAuth(user.idx, accessToken);
      dispatch(logout());
      navigate("/main");
    } catch (err) {
      console.error("회원탈퇴 실패", err);
    }
  };
  
  return (
    <div className="mypage-container">
      <Navbar />
      <div className="mypage-content">
        <div className="mypage-card">
          <div className="mypage-profile-img" />
          <div className="mypage-user-info">
            <p>연결된 소셜계정 : <span className="social-circle">{social}</span></p>
            <p>닉네임 : {nickname}</p>
          </div>
        </div>

        <div className="mypage-buttons">
          <button onClick={() => navigate('/zzim')}>찜한 매물 & 동네</button>
          <button>내 정보 수정</button>
        </div>
      </div>


      <div className="mypage-footer">
        <button className="logout-btn" onClick={handleLogout}>로그아웃</button>
        <button className="withdraw-btn" onClick={handleUserDelete}>회원 탈퇴</button>
      </div>
    </div>
  );
};

export default MyPage;
