import React from 'react';
import zeepseek from './assets/logo/zeepseek.png'
import './App.css'
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

function App() {
  const navigate = useNavigate();

  const sendTestRequest = async () => {
    try {
      const response = await axios.get('http://j12e203.p.ssafy.io:8081/api/test');
      console.log('Response:', response.data);
    } catch (error) {
      console.error('Error:', error);
    }
  };
  
  return (
    <>
      <div className="app-container">
      <img
        src={zeepseek}
        alt="zeepseek 로고"
        className="logo"
        onClick={() => navigate("/main")}
      />
      <button onClick={sendTestRequest}>Send Test Request</button>
    </div>
    </>
  )
}

export default App
