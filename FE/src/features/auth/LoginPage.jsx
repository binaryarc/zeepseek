import React, { useState } from "react";
import './LoginPage.css'
import { useNavigate } from 'react-router-dom';
import zeeplogin from '../../assets/logo/zeep_login.png'
import KakaoLogin from "./kakao/KakaoLogin";
import NaverLogin from "./naver/NaverLogin";
import AdminLogin from "./admin/AdminLogin";

const LoginPage = () => {    
    const navigate = useNavigate();
    const [isOpen, setIsOpen] = useState(false);

    return (
        <>
          <div className="login-container">
            <img src={zeeplogin} alt="zeepseek 로고" className="login-logo" onClick={() => navigate("/main")}/>
            <div className="login-buttons">
              <KakaoLogin />
              <NaverLogin />

              <p 
                className="admin-login-trigger" 
                onClick={() => setIsOpen(!isOpen)}
              >
                관계자 로그인
              </p>
              {isOpen && <AdminLogin />}
            </div>
        </div>
        </>
      )
}

export default LoginPage;