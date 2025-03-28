// src/api/zeepApi.js
import axios from "axios";
import store from "../../store/store";
import { setAccessToken, logout } from "../../store/slices/authSlice";

const zeepApi = axios.create({
  baseURL: `https://j12e203.p.ssafy.io/api/v1`, // ✅ API 서버 주소
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
export const fetchDongPropertyCounts = async (filterKey) => {
  try {
    const res = await zeepApi.get(`/property/count/dong/${filterKey}`);
    // console.log("동동별 매물 개수 조회 결과:", res);
    return res.data;
  } catch (err) {
    console.error("동별 매물 개수 조회 실패:", err);
    return [];
  }
};

// ✅ 매물 검색 요청 (keyword 기반)
export const searchProperties = async (
  keyword,
  filter,
  page = 1,
  size = 10000
) => {
  try {
    const res = await zeepApi.post("/search", {
      keyword,
      filter, // ✅ roomType 필드 추가
      page,
      size,
    });
    return res.data;
  } catch (error) {
    console.error("매물 검색 API 실패:", error);
    return [];
  }
};

// ✅ 지도 드래그 매물 조회
export const fetchPropertiesByBounds = async (
  guName,
  dongName,
  filter, // ✅ 추가
  page = 1,
  size = 10000
) => {
  try {
    const res = await zeepApi.post("/search/mapper", {
      guName,
      dongName,
      filter,
      page,
      size,
    });
    return res.data;
  } catch (error) {
    console.error("지도 드래그 매물 조회 실패:", error);
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


// grid 위도, 경도 정보 API 통신
export const fetchGridSaleCountsByType = async (cells, type) => {
  try {
    const res = await zeepApi.post(`/property/cells?type=${type}`, { cells });
    console.log("유형별 그리드 매물 개수 조회 결과:", type);
    return res.data;
  } catch (error) {
    console.error("유형별 그리드 매물 개수 조회 실패:", error);
    return [];
  }
};


// AI 추천 API 요청
export const fetchAIRecommendedProperties = async (preferenceData) => {
  try {
    const res = await zeepApi.post("/recommend", preferenceData);
    return res.data;
  } catch (error) {
    console.error("AI 추천 요청 실패:", error);
    return null;
  }
};

export const fetchDongDetail = async (dongId) => {
  try {
    const res = await zeepApi.get(`/dong/${dongId}`);
    return res.data;
  } catch (err) {
    console.error("동 상세 정보 조회 실패:", err);
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
        const res = await zeepApi.post("/auth/refresh");
        const newToken = res.data.accessToken;
        store.dispatch(setAccessToken(newToken));
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return zeepApi(originalRequest);
      } catch {
        store.dispatch(logout());
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);



export default zeepApi;
