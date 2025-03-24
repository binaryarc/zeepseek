import axios from "axios";
import { apiConfig } from "./apiConfig";
import store from "../../store/store";

const api = axios.create({
  baseURL: apiConfig.baseURL,
  headers: apiConfig.headers,
  withCredentials: true,
});

// ✅ 요청 인터셉터 (모든 요청에 `accessToken` 자동 추가)
api.interceptors.request.use((config) => {
  const token = store.getState().auth.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});


// 

export default api;
