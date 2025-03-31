import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { setAccessToken, logout } from "./slices/authSlice";
import { refreshAccessToken } from "../common/api/authApi";

const AuthInitializer = () => {
  const dispatch = useDispatch();

  useEffect(() => {
    const isAuthenticated = localStorage.getItem("isAuthenticated");
  
    if (isAuthenticated === "true") {
      refreshAccessToken()
        .then((res) => {
          const accessToken = res.data.accessToken;
          dispatch(setAccessToken(accessToken));
          // ✅ user는 없음 → setUser 생략
        })
        .catch((err) => {
          console.error("refresh 실패:", err);
          localStorage.removeItem("isAuthenticated");
          dispatch(logout());
        });
    }
  }, [dispatch]);

  return null;
};

export default AuthInitializer;
