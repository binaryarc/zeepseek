import "./MyPage.css";
import Navbar from "../../common/navbar/Navbar";
import { useNavigate } from "react-router-dom";

const MyPage = () => {
  const nickname = "크롤링하는 크룡"; // 실제론 백엔드에서 가져오겠죠?
  const social = "N"; // 예: N은 네이버, K는 카카오 등
  const navigate = useNavigate();

  return (
    <div className="mypage-container">
      <Navbar />
      <div className="mypage-card">
        <div className="mypage-profile-img" />
        <p>
          연결된 소셜계정 : <span className="social-circle">{social}</span>
        </p>
        <p>닉네임 : {nickname}</p>
      </div>

      <div className="mypage-buttons">
        <button onClick={() => navigate("/zzim")}>찜한 매물 & 동네</button>
        <button>내 정보 수정</button>
      </div>

      <div className="mypage-footer">
        <button className="logout-btn">로그아웃</button>
        <button className="withdraw-btn">회원 탈퇴</button>
      </div>
    </div>
  );
};

export default MyPage;
