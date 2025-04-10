import "./MyPage.css";
import Navbar from "../../common/navbar/Navbar";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { logout } from "../../store/slices/authSlice";
import { logoutOAuth } from "../../common/api/authApi";
import { deleteOAuth } from "../../common/api/authApi";
import naverlogo from '../../assets/logo/naver.png'
import kakaologo from '../../assets/logo/kakao.png'
import SurveyPopup from "../../common/component/SurveyPopup";
import { useState } from "react";
import AlertModal from "../../common/component/AlertModal";
import ConfirmModal from "../../common/component/ConfirmModal";

const MyPage = () => {
  const navigate = useNavigate();
  const user =useSelector((state) => state.auth.user)
  const accessToken = useSelector((state) => state.auth.accessToken)
  const social = user?.provider;
  const nickname = user?.nickname || '로그인 유저';
  const [showSurvey, setShowSurvey] = useState(false);
  const dispatch = useDispatch()
  const [alertMessage, setAlertMessage] = useState("");
  const [showConfirm, setShowConfirm] = useState(false);

  const getSocialLogo = (provider) => {
    switch (provider) {
      case 'naver':
        return naverlogo;
      case 'kakao':
        return kakaologo;
      default:
        return null;
    }
  };
  const handleLogout = async () => {
    await logoutOAuth(accessToken);      // 백엔드에 로그아웃 요청
    dispatch(logout());                  // Redux 상태 초기화
    navigate("/main");                 // 로그인 페이지로 이동 (선택)

  };

  const handleUserDelete = () => {
    setShowConfirm(true); // 👉 모달 열기
  };

  const handleConfirmDelete = async () => {
    try {
      await deleteOAuth(user.idx, accessToken);
      dispatch(logout());
      setAlertMessage("회원 탈퇴가 완료되었습니다.");
    } catch {
      setAlertMessage("탈퇴 중 문제가 발생했습니다.");
    } finally {
      setShowConfirm(false); // 모달 닫기
    }
  };

  return (
    <div className="mypage-container">
      <Navbar />
      <div className="mypage-content">
        <div className="mypage-card">
          <div className="mypage-profile-img" />
          <div className="mypage-user-info">
            <p>연결된 소셜계정 : <span className="social-circle">
        {/* {social} */}
        {getSocialLogo(social) && (
          <img 
            src={getSocialLogo(social)} 
            alt={`${social} 로고`} 
            style={{ width: '30px', marginLeft: '8px', verticalAlign: 'middle' }}
          />
        )}
      </span></p>
            <p>닉네임 : {nickname}</p>
          </div>
        </div>

        <div className="mypage-buttons">
          <button onClick={() => navigate('/zzim')}>찜한 매물 </button>
          <button onClick={() => setShowSurvey(true)}>내 정보 수정</button>
        </div>
      </div>
      {showSurvey && (
        <SurveyPopup
          onClose={() => setShowSurvey(false)}
          mode="edit"
          initialData={{
            gender: user.profileInfo?.gender,
            age: user.profileInfo?.age,
            location: user.profileInfo?.location,
            preferences: user.profileInfo?.preferences,
          }}
        />
      ) 
      }
    
      <div className="mypage-footer">
        <button className="logout-btn" onClick={handleLogout}>로그아웃</button>
        <button className="withdraw-btn" onClick={handleUserDelete}>회원 탈퇴</button>
      </div>

      {showConfirm && (
        <ConfirmModal
          message="정말 탈퇴하시겠습니까? 😢"
          onConfirm={handleConfirmDelete}
          onCancel={() => setShowConfirm(false)}
        />
      )}
      
      {alertMessage && (
        <AlertModal
          message={alertMessage}
          buttonText="확인"
          onClose={() => {
            setAlertMessage("");
            navigate("/main");
          }}
        />
      )}
    </div>
  );
};

export default MyPage;
