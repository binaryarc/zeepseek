import axios from "axios";
import { apiConfig } from "./apiConfig";


const api = axios.create({
  baseURL: apiConfig.baseURL,
  headers: apiConfig.headers,
  withCredentials: false,
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
    const res = await api.get("/search", {
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
    const res = await api.get(`/property/dong/${dongId}`);
    console.log("동 매물 조회 결과:", dongId);
    console.log("동 매물 조회 결과:", res);
    return res.data; // 🔥 res.properties가 아니라 res.data로 전체 리턴
  } catch (error) {
    console.error("동 매물 조회 실패:", error);
    return [];
  }
};


export default api;
