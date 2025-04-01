import "./MyPage.css";
import Navbar from "../../common/navbar/Navbar";
import { useSelector } from "react-redux";
// import zeep from "../../assets/images/z"
const MyPage = () => {
  const user = useSelector((state) => state.auth.user)
  const nickname = user?.nickname
  const social = user?.provider; // 예: N은 네이버, K는 카카오 등

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
          <button>찜한 매물 & 동네</button>
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