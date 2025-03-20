import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { oauthLogin } from "../../../common/api/authApi";

const NaverRedirectHandler = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const code = searchParams.get("code");

  useEffect(() => {
    const handleNaverLogin = async () => {
      if (code) {
        try {
          await oauthLogin(code, "naver");
          navigate("/main");
        } catch (error) {
          navigate("/login");
        }
      }
    };
    handleNaverLogin();
  }, [code, navigate]);

  return;
};

export default NaverRedirectHandler;
