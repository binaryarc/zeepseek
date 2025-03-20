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
        name: "test",
        age: 25
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
      <button onClick={sendTestRequest}>Send Test Request</button>
      <button onClick={sendTestPost}>Send Test Request</button>
    </div>
    </>
  )
}

export default App
