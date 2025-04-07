// import zeepApi from "./api";
// import axios from "axios";
// const authApi = axios.create({
//   baseURL: `http://localhost:8082/api/v1`, // ✅ API 서버 주소
//   withCredentials: true, // ✅ 쿠키 포함 요청
// });

const authApi = axios.create({
  baseURL: `https://j12e203.p.ssafy.io/api/v1`, // ✅ API 서버 주소
  withCredentials: true, // ✅ 쿠키 포함 요청
});

// OAuth 로그인 (카카오 & 네이버)
export const oauthLogin = async (authorizationCode, provider) => {
  try {
    const response = await authApi.post("/auth/sessions", {
      authorizationCode,
      provider,
    });

    return response.data;
  } catch (error) {
    console.error(`${provider} 로그인 실패:`, error);
    throw error;
  }
};

export const loginOAuth = async (authorizationCode, provider) => {
  return await authApi.post("/auth/login", {
    authorizationCode,
    provider,
  });
};

// 로그아웃
export const logoutOAuth = async (accessToken) => {
  console.log("로그아웃 요청 accessToken:", accessToken);
  return await authApi.delete("/auth/sessions", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    withCredentials: true, // HttpOnly 쿠키도 포함되도록
  });
};

export const refreshAccessToken = async () => {
  const res = await authApi.post("/auth/refresh");
  console.log(res);
  return res.data; // ✅ res 자체가 아닌 res.data만 리턴
};

export const deleteOAuth = async (idx, accessToken) => {
  return await authApi.delete(`/auth/${idx}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    withCredentials: true,
  });
};
