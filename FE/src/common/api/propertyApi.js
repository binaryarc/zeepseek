import zeepApi from './api.js';

// 동 기준 매물 리스트 요청
export const fetchPropertiesByDong = async (dongId) => {
  try {
    const response = await zeepApi.get(`/property/dong/${dongId}`);
    return response.data; // 리스트 형태로 옴
  } catch (err) {
    console.error('동 매물 리스트 조회 실패:', err);
    throw err;
  }
};