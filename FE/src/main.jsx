import * as ReactDOM from "react-dom/client";
import React, { StrictMode } from "react";
import {
  createBrowserRouter,
  createRoutesFromElements,
  Route,
  RouterProvider,
} from "react-router-dom";
import store from './store/store.js'
import "./index.css";
import App from "./App.jsx";
import MainPage from "./features/main/MainPage.jsx";
import LoginPage from "./features/auth/LoginPage.jsx";
import MainMapPage from "./features/map/MainMapPage.jsx";
// import AuthInitializer from "./store/AuthInitializer.jsx";
import { Provider } from "react-redux";


//import OAuth2s' jsx
import KakaoRedirectHandler from "./features/auth/kakao/KakaoRedirectHandler.jsx";
import NaverRedirectHandler from "./features/auth/naver/NaverRedirectHandler.jsx";

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/" element={<App />} />
      <Route path="/main" element={<MainPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/map" element={<MainMapPage />} />

      {/* 소셜 로그인 콜백 라우트 추가 */}
      <Route path="/kakao/callback" element={<KakaoRedirectHandler />} />
      <Route path="/naver/callback" element={<NaverRedirectHandler />} />
    </>
  ),
  { basename: "/"} // 라우트가 /api/경로 파일을 처리하지 않도록 선언
);

ReactDOM.createRoot(document.getElementById("root")).render(
  <Provider store={store}>
    {/* <AuthInitializer>  */}
      <RouterProvider router={router} />
    {/* </AuthInitializer> */}
  </Provider>
  
);