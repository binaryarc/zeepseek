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
import MyPage from "./features/mypage/MyPage.jsx"
import ComparePage from "./features/compare/ComparePage.jsx"
import RegionCompare from "./features/compare/region/RegionCompare.jsx";
import EstateCompare from "./features/compare/estate/EstateCompare.jsx";
import AuthInitializer from "./store/AuthInitializer.jsx";
import Zzim from "./features/mypage/zzim/Zzim.jsx";
import { Provider } from "react-redux";


//import OAuth2s' jsx
import KakaoRedirectHandler from "./features/auth/kakao/KakaoRedirectHandler.jsx";
import NaverRedirectHandler from "./features/auth/naver/NaverRedirectHandler.jsx";
import FirstLoginSurvey from "./features/main/survey/FirstLoginSurvey.jsx";


const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/" element={<App />} />
      <Route path="/main" element={<MainPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/map" element={<MainMapPage />} />
      <Route path="/mypage" element={<MyPage />} />
      <Route path="/zzim" element={<Zzim />} />
      <Route path="/compare" element={<ComparePage />}>
        <Route path="region" element={<RegionCompare />}/>
        <Route path="estate" element={<EstateCompare />}/>
      </Route>
      <Route path="/survey" element={<FirstLoginSurvey />}></Route>
      
      {/* 소셜 로그인 콜백 라우트 추가 */}
      <Route path="/auth/kakao/callback" element={<KakaoRedirectHandler />} />
      <Route path="/auth/naver/callback" element={<NaverRedirectHandler />} />
    </>
  ),
  { basename: "/"} // 라우트가 /api/경로 파일을 처리하지 않도록 선언
);

ReactDOM.createRoot(document.getElementById("root")).render(
  <Provider store={store}>
    {/* <AuthInitializer/> */}
    <RouterProvider router={router} />
  </Provider>
  
);