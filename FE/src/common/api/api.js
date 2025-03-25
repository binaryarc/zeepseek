// src/api/zeepApi.js
import axios from 'axios';
import store from '../../store/store';
import { setAccessToken, logout } from '../../store/slices/authSlice';

const zeepApi = axios.create({
  baseURL:`https://j12e203.p.ssafy.io/api/v1`, // ✅ API 서버 주소
  withCredentials: false, // ✅ 쿠키 포함 요청
});

// ✅ 요청 인터셉터 (모든 요청에 `accessToken` 자동 추가)
// api.interceptors.request.use((config) => {
//   const token = store.getState().auth.accessToken;
//   if (token) {
//     config.headers.Authorization = `Bearer ${token}`;
//   }
//   return config;
// });

// ✅ 매물 검색 요청 (keyword 기반)
export const searchProperties = async (keyword, page = 1, size = 20) => {
  try {
    const res = await zeepApi.get("/search", {
      params: {
        keyword,
        page,
        size,
      },
    });
    return res.data;
  } catch (error) {
    console.error("매물 검색 API 실패:", error);
    return []; // ✅ 실패 시라도 빈 배열 반환
  }
};

// ✅ 동 ID 기반 매물 조회 API
export const getPropertiesByDongId = async (dongId) => {
  try {
    const res = await zeepApi.get(`roomList/fetchByDong`);
    console.log("동 매물 조회 결과:", dongId);
    console.log("동 매물 조회 결과:", res);
    return res.data; // 🔥 res.properties가 아니라 res.data로 전체 리턴
  } catch (error) {
    console.error("동 매물 조회 실패:", error);
    return [];
  }
};

// 상세 매물 조회 API
export const getPropertyDetail = async (propertyId) => {
  try {
    const res = await zeepApi.get(`/property/${propertyId}`);
    return res.data;
  } catch (error) {
    console.error("매물 상세 조회 실패:", error);
    return null;
  }
};

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
      } catch {
        store.dispatch(logout());
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

export default zeepApi;
