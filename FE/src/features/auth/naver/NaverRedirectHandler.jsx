import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useDispatch } from "react-redux";
import { oauthLogin } from "../../../common/api/authApi";
import { setAccessToken, setUser } from "../../../store/slices/authSlice";
import { setDongLikes } from "../../../store/slices/dongLikeSlice"
import { fetchLikedRegions } from "../../../common/api/api";

const NaverRedirectHandler = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [searchParams] = useSearchParams();
  const code = searchParams.get("code");
  const state = searchParams.get("state");

  useEffect(() => {
    const handleNaverLogin = async () => {
      if (code && state) {
        try {
          console.log("네이버 로그인 처리 중...", code.substring(0, 5) + "...");

          const userInfo = await oauthLogin(code, "naver"); // 응답이 사용자 객체 자체
          console.log(userInfo.data);
          const { accessToken, user: userInfoData } = userInfo.data;

          dispatch(setAccessToken(accessToken));
          dispatch(setUser(userInfoData)); // accessToken 제외한 나머지
          localStorage.setItem('isAuthenticated', 'true'); // local storage에 로그인 상태 저장

          // ✅ 여기서 찜한 동네 연결
          const likedDongRes = await fetchLikedRegions(userInfo.data.user.idx);
          dispatch(setDongLikes(likedDongRes.data));

          // ✅ 여기서 분기 처리
          if (userInfoData.isFirst === 1) {
            navigate("/survey");
          } else {
            navigate("/main");
          }
        } catch (error) {
          console.error("네이버 로그인 처리 실패:", error);
          navigate("/login");
        }
      } else {
        console.error("인증 코드 또는 상태값이 없습니다.");
        navigate("/login");
      }
    };

    handleNaverLogin();
  }, [code, state, navigate, dispatch]);

  return <div className="login-processing">네이버 로그인 처리 중...</div>;
};

export default NaverRedirectHandler;
