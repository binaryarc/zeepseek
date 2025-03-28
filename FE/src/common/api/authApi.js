import zeepApi from "./api";

// OAuth 로그인 (카카오 & 네이버)
export const oauthLogin = async (authorizationCode, provider) => {
  try {
    const response = await zeepApi.post("/auth/sessions", {
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
  return await zeepApi.post("/auth/login", {
    authorizationCode,
    provider,
  });
};

// 로그아웃
export const logoutOAuth = async (accessToken) => {
  console.log("로그아웃 요청 accessToken:", accessToken);
  return await zeepApi.delete("/auth/sessions", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
    withCredentials: true, // HttpOnly 쿠키도 포함되도록
  })
};

export const refreshAccessToken = async () => {
  return await zeepApi.post("/auth/refresh"); // refresh_token은 쿠키에서 자동 전송됨
};
