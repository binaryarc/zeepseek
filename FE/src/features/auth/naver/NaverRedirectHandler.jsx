import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { oauthLogin } from "../../../common/api/authApi";

const NaverRedirectHandler = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const code = searchParams.get("code");
  const state = searchParams.get("state");

  useEffect(() => {
    const handleNaverLogin = async () => {
      if (code && state) {
        try {
          console.log("네이버 로그인 처리 중...", code.substring(0, 5) + "...");
          await oauthLogin(code, "naver");
          navigate("/main");
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
  }, [code, state, navigate]);

  // 로딩 중 표시
  return <div className="login-processing">네이버 로그인 처리 중...</div>;
};

export default NaverRedirectHandler;