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

// 🔹 매물 개수 조회 - 구 단위
export const fetchGuPropertyCounts = async () => {
  try {
    const res = await zeepApi.get("/property/count/gu");
    // console.log("구별 매물 개수 조회 결과:", res);
    return res.data;
  } catch (err) {
    console.error("구별 매물 개수 조회 실패:", err);
    return [];
  }
};

// 🔹 매물 개수 조회 - 동 단위
export const fetchDongPropertyCounts = async () => {
  try {
    const res = await zeepApi.get("/property/count/dong");
    // console.log("동동별 매물 개수 조회 결과:", res);
    return res.data;
  } catch (err) {
    console.error("동별 매물 개수 조회 실패:", err);
    return [];
  }
};


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

// ✅ 지도 드래그 매물 조회
export const fetchPropertiesByBounds = async (guName, dongName, page=1, size=10000) => {
  try {
    const res = await zeepApi.get("/search/mapper", {
      params: {
        guName,
        dongName,
        page,
        size,
      },
    });
    return res.data;
  } catch (error) {
    console.error("지도 드래그 매물 조회 실패:", error);
    return [];
  }
}



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

export default zeepApi;
