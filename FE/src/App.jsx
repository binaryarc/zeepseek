import React from 'react';
import zeepseek from './assets/logo/zeepseek.png'
import './App.css'
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

function App() {
  const navigate = useNavigate();

  const sendTestRequest = async () => {
    try {
      const response = await axios.get('/api/test');
      console.log('Response:', response.data);
    } catch (error) {
      console.error('Error:', error);
    }
  };

  const sendTestPost = async () => {
    try {
      const response = await axios.post('/api/test', {
        name: "web",
        age: 111
      });
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
      <button onClick={sendTestRequest}>get test</button>
      <button onClick={sendTestPost}>post test</button>
    </div>
    </>
  )
}

export default App
