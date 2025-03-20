import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { oauthLogin } from "../../../common/api/authApi";

const KakaoRedirectHandler = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const code = searchParams.get("code");

  useEffect(() => {
    const handleKakaoLogin = async () => {
      if (code) {
        try {
          await oauthLogin(code, "kakao");
          navigate("/main");
        } catch (error) {
          navigate("/login");
        }
      }
    };
    handleKakaoLogin();
  }, [code, navigate]);

  return;
};

export default KakaoRedirectHandler;
