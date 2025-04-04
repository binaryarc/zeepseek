import React from 'react';
import zeepseek from './assets/logo/zeepseek.png'
import './App.css'
import { useNavigate } from 'react-router-dom';
import AuthInitializer from './store/AuthInitializer';


function App() {

  const navigate = useNavigate();
  const handleAnimationEnd = () => {
    // ✅ 첫 로그인 사용자라면 메인으로 이동 막기

    navigate('/main');
  };

  return (
    <>
      <AuthInitializer />
  6    <div className="app-container">
      <img
        src={zeepseek}
        alt="zeepseek 로고"
        className="intro-logo"
        onAnimationEnd={handleAnimationEnd}
      />
    </div>
    </>
  )
}

export default App
