import { useEffect } from "react";
import { useDispatch } from "react-redux";
import { setAccessToken, logout, setUser } from "./slices/authSlice";
import { refreshAccessToken } from "../common/api/authApi";
import { fetchLikedRegions } from "../common/api/api";
import { setDongLikes} from "./slices/dongLikeSlice";

const AuthInitializer = () => {
  const dispatch = useDispatch();

  useEffect(() => {
    const isAuthenticated = localStorage.getItem("isAuthenticated");
    const hasAccessCookie = document.cookie.includes("refreshtoken");
    if (isAuthenticated === "true" && hasAccessCookie) {
      refreshAccessToken()
        .then(async (res) => {
          const accessToken = res.data.accessToken;
          const user = res.data.user;
          dispatch(setAccessToken(accessToken));
          dispatch(setUser(user));
          console.log(user)
          // ✅ user는 없음 → setUser 생략

          // ✅ 찜한 동네 정보 가져오기
        const likedDongRes = await fetchLikedRegions(user.idx);
        dispatch(setDongLikes(likedDongRes.data));
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
