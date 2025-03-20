import React from "react";
import kakao from "../../assets/button/kakao.png"
import './KakaoLogin.css'
const KakaoLogin = () => {

    const handleKakaoLogin = () => {
        const KAKAO_AUTH_URL = `https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=${import.meta.env.VITE_KAKAO_CLIENT_ID}&redirect_uri=${import.meta.env.VITE_KAKAO_REDIRECT_URI}`;
        
        window.location.href = KAKAO_AUTH_URL; // ✅ 카카오 로그인 페이지로 이동
    };


return (
    <button className="kakao-button" onClick={handleKakaoLogin}>
        <img src={kakao} alt="카카오로그인" className="kakao" />
    </button>
)
}

export default KakaoLogin;