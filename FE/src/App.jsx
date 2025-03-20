import React from 'react';
import zeepseek from './assets/logo/zeepseek.png'
import './App.css'
import { useNavigate } from 'react-router-dom';

function App() {
  const navigate = useNavigate();
  
  return (
    <>
      <div className="app-container">
      <img
        src={zeepseek}
        alt="zeepseek 로고"
        className="logo"
        onClick={() => navigate("/main")}
      />
    </div>
    </>
  )
}

export default App
