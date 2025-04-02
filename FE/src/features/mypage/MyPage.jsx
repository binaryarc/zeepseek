import "./MyPage.css";
import Navbar from "../../common/navbar/Navbar";
import { useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
const MyPage = () => {
  const navigate = useNavigate();
  const user =useSelector((state) => state.auth.user)
  const social = user?.provider;
  const nickname = user?.nickname || '로그인 유저';
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
        <button className="logout-btn">로그아웃</button>
        <button className="withdraw-btn">회원 탈퇴</button>
      </div>
    </div>
  );
};

export default MyPage;
