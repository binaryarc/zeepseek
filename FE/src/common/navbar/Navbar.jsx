import React from 'react';
import title from '../../assets/logo/zeeptitle.png';
import './Navbar.css';
import { useNavigate } from 'react-router-dom';

function Navbar() {
    const navigate = useNavigate();
  return (
    <nav className="nav-navbar">
      <img src={title} alt="zeepseek 로고" className="nav-logo" />
      <button className="nav-login-btn" onClick={() => navigate('/login')}>로그인</button>
    </nav>
  );
}

export default Navbar;