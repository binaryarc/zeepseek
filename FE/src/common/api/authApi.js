import api from "./api";
import { setAuth } from "../../store/authSlice";
import store from "../../store/store";

// OAuth 로그인 (카카오 & 네이버)
export const oauthLogin = async (authorizationCode, provider) => {
  try {
    const response = await api.post("/api/v1/auth/login", {
      authorizationCode,
      provider,
    });

    // Redux 상태 업데이트 (토큰 & 유저 정보 저장)
    store.dispatch(setAuth({ accessToken: response.data.accessToken, user: response.data }));

    return response.data;
  } catch (error) {
    console.error(` ${provider} 로그인 실패:`, error);
    throw error;
  }
};
