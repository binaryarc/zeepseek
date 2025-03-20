import React from "react";
import './LoginPage.css'
import { useNavigate } from 'react-router-dom';
import zeeplogin from '../../assets/logo/zeep_login.png'
import KakaoLogin from "./kakao/KakaoLogin";
import NaverLogin from "./naver/NaverLogin";

const LoginPage = () => {    
    const navigate = useNavigate();

    return (
        <>
          <div className="login-container">
            <img src={zeeplogin} alt="zeepseek 로고" className="login-logo" onClick={() => navigate("/main")}/>
            <div className="login-buttons">
               <KakaoLogin />
               <NaverLogin />
            </div>
        </div>
        </>
      )
}

export default LoginPage;