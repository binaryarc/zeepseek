import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { setAuth, logout } from "./authSlice";
import { refreshTokenApi } from "../common/api/authApi";

function AuthInitializer({ children }) {
  const dispatch = useDispatch();
  const isAuthenticated = useSelector((state) => state.auth.isAuthenticated);

  useEffect(() => {
    async function initializeAuth() {
      try {
        const res = await refreshTokenApi();
        dispatch(setAuth({ accessToken: res.data.accessToken, user: res.data }));
      } catch (err) {
        dispatch(logout());
      }
    }

    if (!isAuthenticated) {
      initializeAuth();
    }
  }, [dispatch, isAuthenticated]);

  return children;
}

export default AuthInitializer;
