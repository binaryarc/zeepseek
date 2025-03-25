import zeepApi from "./api";
// // import { setAuth } from "../../store/slices/authSlice";
// import store from "../../store/store";

// OAuth 로그인 (카카오 & 네이버)
export const oauthLogin = async (authorizationCode, provider) => {
  try {
    const response = await zeepApi.post("/api/v1/auth/login", {
      authorizationCode,
      provider,
    });

    // Redux 상태 업데이트 (토큰 & 유저 정보 저장)
    // store.dispatch(setAuth({ accessToken: response.data.accessToken, user: response.data }));

    return response.data;
  } catch (error) {
    console.error(` ${provider} 로그인 실패:`, error);
    throw error;
  }
};


export const loginOAuth = async (authorizationCode, provider) => {
  return await zeepApi.post("/auth/login", {
    authorizationCode,
    provider,
  });
};

export const refreshAccessToken = async () => {
  return await zeepApi.post("/auth/refresh"); // refresh_token은 쿠키에서 자동 전송됨
};