import React from 'react';
import zeepseek from './assets/logo/zeepseek.png'
import './App.css'
import { useNavigate } from 'react-router-dom';


function App() {
  const navigate = useNavigate();

  const handleAnimationEnd = () => {
    navigate('/main');
  };

  return (
    <>
      <div className="app-container">
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
