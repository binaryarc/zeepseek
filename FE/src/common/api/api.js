// src/api/zeepApi.js
import axios from "axios";
import store from "../../store/store";
import { setAccessToken } from "../../store/slices/authSlice";

const zeepApi = axios.create({
  baseURL: `https://j12e203.p.ssafy.io/api/v1`, // ✅ API 서버 주소
  withCredentials: true, // ✅ 쿠키 포함 요청
});

// const authApi = axios.create({
//   baseURL: `http://localhost:8082/api/v1`, // ✅ API 서버 주소
//   withCredentials: false,
// });

// 요청 인터셉터 - accessToken이 있으면 Authorization 헤더에 추가
zeepApi.interceptors.request.use((config) => {
  const token = store.getState().auth.accessToken;
  if (token) {
    // 모든 요청에 accessToken을 헤더에 추가합니다.
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});


// refresh 작업을 위한 상태 변수와 실패 큐
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// 응답 인터셉터 - 403 혹은 인증 관련 에러 발생시 refresh 후 재시도
zeepApi.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // refresh 요청이 여러 번 반복되지 않도록 플래그를 사용
    if (error.response &&
      (error.response.status === 403 || error.response.status === 401) &&
      !originalRequest._retry) {
      if (isRefreshing) {
        // 이미 refresh 요청 진행 중이면 큐에 넣어두고, refresh 완료 후 재시도
        const token = await new Promise(function (resolve, reject) {
          failedQueue.push({ resolve, reject });
        });
        originalRequest.headers.Authorization = `Bearer ${token}`;
        return await zeepApi(originalRequest);
      }

      originalRequest._retry = true;
      isRefreshing = true;

      // refresh 요청: 쿠키(withCredentials)를 함께 보내므로 별도의 토큰 헤더는 필요 없음
      return new Promise((resolve, reject) => {
        zeepApi
          .post("/auth/refresh")
          .then(({ data }) => {
            const newAccessToken = data.data.accessToken;
            // store 업데이트
            store.dispatch(setAccessToken(newAccessToken));
            // 기본 헤더에도 갱신된 토큰을 설정
            zeepApi.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
            processQueue(null, newAccessToken);
            // 원래 요청 재시도
            originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
            resolve(zeepApi(originalRequest));
          })
          .catch((err) => {
            processQueue(err, null);
            // refresh 실패 시 로그아웃 처리
            store.dispatch(logout());
            reject(err);
          })
          .finally(() => {
            isRefreshing = false;
          });
      });
    }
    return Promise.reject(error);
  }
);


// 🔹 매물 개수 조회 - 구 단위
export const fetchGuPropertyCounts = async (filterKey) => {
  try {
    const res = await zeepApi.get(`/property/count/gu/${filterKey}`);
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
  userId,
  page = 1,
  size = 10000
) => {
  try {
    const res = await zeepApi.post("/search", {
      keyword,
      filter, // ✅ roomType 필드 추가
      userId,
      page,
      size,
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
  userId,
  page = 1,
  size = 10000
) => {
  try {
    const res = await zeepApi.post("/search/mapper", {
      guName,
      dongName,
      filter,
      userId,
      page,
      size,
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

// 매물 점수 불러오는 api
export const fetchProPertyScore = async (propertyId) => {
  try {
    const res = await zeepApi.get(`/property/score/${propertyId}`);
    console.log("매물 점수 호출 성공: ", res);
    return res.data;
  } catch (err) {
    console.error("매물 점수 호출 실패:", err);
  }
};

// 매물 비교 용 api(아직 안됨, 다시 만들어야 함)
export const fetchPropertyCompare = async (userId, prop1, prop2) => {
  console.log("token값: ", store.getState().auth.accessToken);
  try {
    const res = await zeepApi.post("/dong/compare/property", {
      userId: userId,
      prop1: prop1,
      prop2: prop2,
    });
    // {
    // headers: {
    //     Authorization: `Bearer ${store.getState().auth.accessToken}`,
    //   },
    // });
    console.log("매물 비교 요청 성공: ", res);
    return res;
  } catch (err) {
    console.error("매물 비교 실패b:", err);
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
export const fetchRegionSummary = async (userId, region1, region2) => {
  try {
    const response = await zeepApi.post("/dong/compare/dong", {
      userId: userId,
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
    console.log(res);
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
  console.log("토큰?", store.getState().auth.accessToken);
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
    console.log("찜한 매물 리스트 호출: ", res);
    return res.data;
  } catch (err) {
    console.error("찜한 매물 불러오기 실패:", err);
    return [];
  }
};

// 동네 찜 추가 (POST)
export const likeDongApi = async (dongId, userId) => {
  try {
    const res = await zeepApi.post(`/zzim/dong/${dongId}/${userId}`, {
      headers: {
        Authorization: `Bearer ${store.getState().auth.accessToken}`,
      },
      withCredentials: true,
    });
    return res.data;
  } catch (error) {
    console.error("동네 찜 추가 실패:", error);
    throw error;
  }
};

// 동네 찜 삭제 (DELETE)
export const unlikeDongApi = async (dongId, userId) => {
  try {
    const res = await zeepApi.delete(
      `/zzim/dong/${dongId}/${userId}`,
      {},
      {
        headers: {
          Authorization: `Bearer ${store.getState().auth.accessToken}`,
        },
      }
    );
    return res.data;
  } catch (error) {
    console.error("동네 찜 삭제 실패:", error);
    throw error;
  }
};

export const postSurvey = async (surveyData, accessToken) => {
  // console.log("accessToken:", accessToken);
  const response = await zeepApi.post("/auth/survey", surveyData, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
  return response.data;
};

export const patchSurvey = async (userId, surveyData, accessToken) => {
  const response = await zeepApi.patch(`/auth/${userId}`, surveyData, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
  return response.data;
};

export const fetchNearbyPlaces = async (type, lng, lat) => {
  try {
    const response = await zeepApi.get(
      `/places/search?type=${type}&x=${lng}&y=${lat}`, {
        headers: {
          Authorization: `Bearer ${store.getState().auth.accessToken}`,
        },
      }
    );
    console.log("추천 매물 반경 위치 정보 요청 성공: ", response);
    return response;
  } catch (err) {
    console.log("추천 매물 반경 위치 정보 요청 실패: ", err);
    return [];
  }
};

// 동네 댓글 조회
export const fetchDongComments = async (dongId, token) => {
  try {
    const res = await zeepApi.get(`/dong/${dongId}/comment`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
    console.log(res.data);
    return res.data; // ✅ 댓글 배열만 추출
  } catch (err) {
    console.error("댓글 조회 실패:", err);
    return [];
  }
};

export const postDongComment = async (dongId, nickname, content, token) => {
  try {
    const res = await zeepApi.post(
      `/dong/${dongId}/comment`,
      {
        nickname: nickname,
        content: content,
      },
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );
    return res.data;
  } catch (err) {
    console.error("댓글 작성 실패:", err);
    throw err;
  }
};

export const deleteDongComment = async (dongId, commentId, token) => {
  try {
    const res = await zeepApi.delete(
      `/dong/${dongId}/comment`, // 🔥 이건 그대로
      {
        params: { commentId }, // ✅ query string
        headers: {
          Authorization: `Bearer ${token}`, // ✅ 토큰
        },
      }
    );
    return res.data;
  } catch (err) {
    console.error("❌ 댓글 삭제 실패:", err.response?.data || err.message);
    throw err;
  }
};

// 찜한 동네 리스트에서 동네 이름 검색하는 api
export const searchDongByName = async (dongName) => {
  try {
    const res = await zeepApi.get(`/dong/search?name=${dongName}`);
    console.log("동 이름 검색 결과: ", res);
    return res;
  } catch (err) {
    console.error("동 이름 검색 실패: ", err);
    throw err;
  }
};

// ai 추천 api
export const aiRecommendByUserId = async (userId) => {
  try {
    const res = await zeepApi.get(`/recommend/ai-recommend?userId=${userId}`);
    console.log(res);
    return res;
  } catch (err) {
    console.error("사용자 추천 실패: ", err);
    throw err;
  }
};

// 🚇 기준지 ↔ 매물 통근 시간 조회 API
export const fetchCommuteTime = async ({ userId, lat, lon }) => {
  try {
    const res = await zeepApi.get(
      `/distance/property-transit?userId=${userId}&propertyLat=${lat}&propertyLon=${lon}`
    );
    console.log("dd", res);
    return res.data; // ✅ { driveTime, publicTransportTime, walkTime }
  } catch (error) {
    console.error("통근 시간 조회 실패:", error);
    return null;
  }
};

export const fetchRandomNickname = async () => {
  const response = await zeepApi.get("/auth/random-nickname");
  return response.data.data;
};

export const fetchTop5Property = async () => {
  try {
    const res = await zeepApi.get(`/rankings/${userId}`);
    console.log(res);
    return res;
  } catch (err) {
    console.error("Top5 매물물 실패: ", err);
    throw err;
  }
}

export default zeepApi;
