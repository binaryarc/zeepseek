import React from "react";
import naver from "../../../assets/images/naver.png"
import './NaverLogin.css'
const NaverLogin = () => {

    const handleNaverLogin = () => {
        const redirectUri = `${window.location.origin}/auth/naver/callback`;
        const NAVER_AUTH_URL = `https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=${import.meta.env.VITE_NAVER_CLIENT_ID}&redirect_uri=${encodeURIComponent(redirectUri)}`;
        
        window.location.href = NAVER_AUTH_URL;
    };


return (
    <button className="naver-button" onClick={handleNaverLogin}>
        <img src={naver} alt="네이버 로그인" className="naver" />
    </button>
)
}

export default NaverLogin;
