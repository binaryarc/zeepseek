import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useDispatch } from "react-redux";
import { oauthLogin } from "../../../common/api/authApi";
import { setAccessToken, setUser } from "../../../store/slices/authSlice";
import { fetchLikedRegions } from "../../../common/api/api";
import { setDongLikes } from "../../../store/slices/dongLikeSlice";

const KakaoRedirectHandler = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [searchParams] = useSearchParams();
  const code = searchParams.get("code");

  useEffect(() => {
    const handleKakaoLogin = async () => {
      if (code) {
        try {
          const userInfo = await oauthLogin(code, "kakao");
          const { accessToken, user: userInfoData } = userInfo.data;

          dispatch(setAccessToken(accessToken));
          dispatch(setUser(userInfoData));
          localStorage.setItem("isAuthenticated", "true");

          // 🔥 찜한 동네 연결
          const likedDongRes = await fetchLikedRegions(userInfoData.idx);
          dispatch(setDongLikes(likedDongRes.data));

          // 🔀 최초 로그인 여부 분기
          if (userInfoData.isFirst === 1) {
            navigate("/survey");
          } else {
            navigate("/main");
          }
        } catch (error) {
          console.error("카카오 로그인 실패:", error);
          navigate("/login");
        }
      }
    };

    handleKakaoLogin();
  }, [code, dispatch, navigate]);

  return <div className="login-processing">카카오 로그인 처리 중...</div>;
};

export default KakaoRedirectHandler;
