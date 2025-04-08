import { useEffect, useState } from "react";
import "./AdminLogin.css"; // 별도의 CSS 파일로 스타일 분리
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import { fetchLikedRegions } from "../../../common/api/api";
import { setAccessToken, setUser } from "../../../store/slices/authSlice";
import { setDongLikes } from "../../../store/slices/dongLikeSlice";
import { adminLogin } from "../../../common/api/authApi";

const AdminLogin = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const handleLoginSubmit = (e) => {
    e.preventDefault();
    // 로그인 처리 로직 (예: API 호출)
    console.log("로그인 시도:", { username, password });
    // 필요하다면 로그인 성공/실패에 따른 추가 처리를 추가합니다.
    handleNaverLogin();
  };

  const handleNaverLogin = async () => {
      try {
        const userInfo = await adminLogin(username, password); // 응답이 사용자 객체 자체
        console.log(userInfo.data);
        console.log(userInfo.data.user.idx)
        const { accessToken, user: userInfoData } = userInfo.data;

        
        dispatch(setAccessToken(accessToken));
        dispatch(setUser(userInfoData)); // accessToken 제외한 나머지
        localStorage.setItem('isAuthenticated', 'true'); // local storage에 로그인 상태 저장

        // ✅ 여기서 찜한 동네 연결
        
        const likedDongRes = await fetchLikedRegions(userInfo.data.user.idx);
        dispatch(setDongLikes(likedDongRes.data));

        // ✅ 여기서 분기 처리
        if (userInfoData.isFirst === 1) {
          navigate("/main", { state: { showSurvey: true } }); // ✅ 상태로 전달
        } else {
          navigate("/main");
        }
        
      } catch (error) {
        console.error("네이버 로그인 처리 실패:", error);
        navigate("/login");
      }
    }
  

  return (
    <div className="admin-login-container">
      <form onSubmit={handleLoginSubmit}>
        <input
          type="text"
          placeholder="아이디"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          className="admin-input"
        />
        <input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="admin-input"
        />
        <button type="submit" className="admin-submit-button">
          확인
        </button>
      </form>
    </div>
  );
};

export default AdminLogin;