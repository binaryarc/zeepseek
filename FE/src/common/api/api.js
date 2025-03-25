// src/api/zeepApi.js
import axios from 'axios';
import store from '../../store/store';
import { setAccessToken, logout } from '../../store/authSlice';

const zeepApi = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  withCredentials: true, // ✅ 쿠키 포함 요청
});

// 요청 인터셉터
zeepApi.interceptors.request.use((config) => {
  const token = store.getState().auth.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터
zeepApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 토큰 만료 시 재발급 시도
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const res = await zeepApi.post('/auth/refresh');
        const newToken = res.data.accessToken;
        store.dispatch(setAccessToken(newToken));

        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return zeepApi(originalRequest);
      } catch (refreshErr) {
        store.dispatch(logout());
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

// AI 매물 추천 api


export default zeepApi;
