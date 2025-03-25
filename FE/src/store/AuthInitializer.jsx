// // store/AuthInitializer.jsx
// import { useEffect } from "react";
// import { useDispatch } from "react-redux";
// import { setAccessToken, setUser, logout } from "./slices/authSlice";
// // import { refreshAccessToken } from "../common/api/authApi";

// const AuthInitializer = ({ children }) => {
//   const dispatch = useDispatch();

//   useEffect(() => {
//     const initializeAuth = async () => {
//       try {
//         const res = await refreshAccessToken();
//         dispatch(setAccessToken(res.data.accessToken));
//         // 유저 정보도 필요하다면 추가로 요청
//       } catch (err) {
//         dispatch(logout());
//       }
//     };

//     initializeAuth();
//   }, [dispatch]);

//   return <>{children}</>;
// };

// export default AuthInitializer;
