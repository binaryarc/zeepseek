// src/api/zeepApi.js
import axios from "axios";
import store from "../../store/store";

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
export const fetchGuPropertyCounts = async (filterKey) => {
  try {
    const res = await zeepApi.get(`/property/count/gu/${filterKey}`);
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
    // console.log("동별 매물 개수 조회 결과:", res);
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
  size = 10000,
  userId = 2
) => {
  try {
    const res = await zeepApi.post("/search", {
      keyword,
      filter, // ✅ roomType 필드 추가
      page,
      size,
      userId,
    });
    console.log(res.data);
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
  size = 10000,
  userId = 2
) => {
  try {
    const res = await zeepApi.post("/search/mapper", {
      guName,
      dongName,
      filter,
      page,
      size,
      userId,
    });
    console.log(res.data);
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

// 매물 비교 용 api(아직 안됨, 다시 만들어야 함)
export const fetchPropertyCompare = async (payload) => {
  try {
    const token = store.getState().auth.accessToken;
    const res = await zeepApi.post("/compare/property", payload, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    return res.data;
  } catch (err) {
    console.error("매물 비교 실패:", err);
    return null;
  }
};

// 동네 점수 부르는 api => 지금 쓸 수 없음, 아마 안쓸듯
export const fetchRegionScore = async (regionName) => {
  const response = await fetch(
    `/api/v1/region/score?name=${encodeURIComponent(regionName)}`
  );
  return await response.json();
};

// 동네 비교 api => AI 비교 요약 내용만 존재()
export const fetchRegionSummary = async (region1, region2) => {
  try {
    const response = await zeepApi.post("/dong/compare/dong", {
      dong1: region1,
      dong2: region2,
    });
    console.log("동네 비교 요약 성공: ", response);
    return response;
  } catch (error) {
    console.error("동네 비교 요약 api 통신 실패: ", error);
    return [];
  }
};

// 매물 찜 추가 (POST)
export const likeProperty = async (propertyId, userId) => {
  try {
    const res = await zeepApi.post(`/zzim/property/${propertyId}/${userId}`, {
      headers: {
        Authorization: `Bearer ${store.getState().auth.accessToken}`,
      },
    });
    return res.data;
  } catch (error) {
    console.error("찜 추가 실패:", error);
    throw error;
  }
};

// 매물 찜 삭제 (DELETE)
export const unlikeProperty = async (propertyId, userId) => {
  try {
    const res = await zeepApi.delete(
      `/zzim/property/${propertyId}/${userId}`,
      {}, // body 없음
      {
        headers: {
          Authorization: `Bearer ${store.getState().auth.accessToken}`,
        },
      }
    );
    return res.data;
  } catch (error) {
    console.error("찜 삭제 실패:", error);
    throw error;
  }
};

// 찜한 동네 리스트 불러오는 api
export const fetchLikedRegions = async (userId) => {
  try {
    const res = await zeepApi.get(`/zzim/select/dong/${userId}`);
    console.log("찜한 동네 리스트 호출: ", res);
    return res;
  } catch (err) {
    console.error("찜한 동네 불러오기 실패:", err);
  }
};

// 찜한 매물 리스트 불러오기
export const fetchLikedProperties = async (userId) => {
  try {
    const res = await zeepApi.get(
      `/zzim/select/property/${userId}`,
      {},
      {
        headers: {
          Authorization: `Bearer ${store.getState().auth.accessToken}`,
        },
      }
    );
    return res.data;
  } catch (err) {
    console.error("찜한 매물 불러오기 실패:", err);
    return [];
  }
};

export const postSurvey = async (surveyData, accessToken) => {
  // console.log("accessToken:", accessToken);
  const response = await zeepApi.post(
    "/auth/survey",
    surveyData ,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    }
  );
  return response.data;
};

// // 응답 인터셉터
// zeepApi.interceptors.response.use(
//   (response) => response,
//   async (error) => {
//     const originalRequest = error.config;

//     // 토큰 만료 시 재발급 시도
//     if (error.response?.status === 401 && !originalRequest._retry) {
//       originalRequest._retry = true;

//       try {
//         const res = await zeepApi.post("/auth/refresh");
//         const newToken = res.data.accessToken;
//         store.dispatch(setAccessToken(newToken));
//         originalRequest.headers.Authorization = `Bearer ${newToken}`;
//         return zeepApi(originalRequest);
//       } catch {
//         store.dispatch(logout());
//         window.location.href = "/login";
//       }
//     }

//     return Promise.reject(error);
//   }
// );

export default zeepApi;
