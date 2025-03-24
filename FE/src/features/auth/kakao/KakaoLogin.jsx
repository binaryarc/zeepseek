import React from "react";
import kakao from "../../../assets/images/kakao.png"
import './KakaoLogin.css'
const KakaoLogin = () => {

    const handleKakaoLogin = () => {
        console.log("리다이렉트 URI:", import.meta.env.VITE_KAKAO_REDIRECT_URI);
        const KAKAO_AUTH_URL = `https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=${import.meta.env.VITE_KAKAO_CLIENT_ID}&redirect_uri=${import.meta.env.VITE_KAKAO_REDIRECT_URI}`;
        console.log("전체 인증 URL:", KAKAO_AUTH_URL);
        window.location.href = KAKAO_AUTH_URL;
    };


return (
    <button className="kakao-button" onClick={handleKakaoLogin}>
        <img src={kakao} alt="카카오로그인" className="kakao" />
    </button>
)
}

export default KakaoLogin;