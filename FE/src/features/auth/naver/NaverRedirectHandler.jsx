import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { oauthLogin } from "../../../common/api/authApi";

const NaverRedirectHandler = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const code = searchParams.get("code");
  const state = searchParams.get("state");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const handleNaverLogin = async () => {
      // 코드와 state 파라미터가 모두 있는지 확인
      if (code && state) {
        try {
          console.log("네이버 로그인 처리 중... 코드:", code.substring(0, 5) + "...");
          const result = await oauthLogin(code, "naver");
          console.log("로그인 성공:", result);
          navigate("/main");
        } catch (error) {
          console.error("네이버 로그인 처리 실패:", error);
          setError("로그인 처리 중 오류가 발생했습니다.");
          setTimeout(() => navigate("/login"), 3000);
        } finally {
          setLoading(false);
        }
      } else {
        console.error("인증 코드 또는 상태값이 없습니다.");
        setError("인증 정보가 올바르지 않습니다.");
        setTimeout(() => navigate("/login"), 3000);
        setLoading(false);
      }
    };

    handleNaverLogin();
  }, [code, state, navigate]);

  if (loading) {
    return <div className="login-processing">네이버 로그인 처리 중...</div>;
  }

  if (error) {
    return <div className="login-error">{error}</div>;
  }

  return null;
};

export default NaverRedirectHandler;