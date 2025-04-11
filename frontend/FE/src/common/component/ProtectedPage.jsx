import React from "react";
import { useSelector } from "react-redux";
import { useNavigate, useLocation } from "react-router-dom";
import "./ProtectedPage.css";

const ProtectedPage = ({
  children,
  message = <>로그인 후 이용하실 수 있습니다</>,
  redirectPath = "/login",
  buttonText = "로그인",
}) => {
  const user = useSelector((state) => state.auth.user);  const navigate = useNavigate();
  const isAuthenticated = !!user;
  const location = useLocation();

  return (
    <div className="protected-wrapper">
      {/* 항상 children을 렌더링하고, 비로그인 시만 흐림 오버레이 */}
      <div className={`protected-content ${!isAuthenticated ? "blur" : ""}`}>
        {children}
      </div>

      {!isAuthenticated && (
        <div className="login-prompt">
          <p>{message}</p>
          <button
            onClick={() =>
              navigate(redirectPath, {
                state: { from: location.pathname }, // 현재 위치 저장
              })
            }
            className="login-btn"
          >
            {buttonText}
          </button>
        </div>
      )}
    </div>
  );
};

export default ProtectedPage;
