import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { setAccessToken, logout, setUser } from "./slices/authSlice";
import { refreshAccessToken } from "../common/api/authApi";

const AuthInitializer = () => {
  const dispatch = useDispatch();

  useEffect(() => {
    const isAuthenticated = localStorage.getItem("isAuthenticated");

    if (isAuthenticated === "true") {
      refreshAccessToken()
        .then((res) => {
          const accessToken = res.data.accessToken;
          const user = res.data.user;
          dispatch(setAccessToken(accessToken));
          dispatch(setUser(user));
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
