import * as ReactDOM from "react-dom/client";
import React, { StrictMode } from 'react';
import {
  createBrowserRouter,
  createRoutesFromElements,
  Route,
  RouterProvider,
} from "react-router-dom";
import store from './store/store.js'
import './index.css'
import App from "./App.jsx";
import MainPage from "./features/main/MainPage.jsx";
import LoginPage from "./features/auth/LoginPage.jsx";
import MainMap from "./features/map/mainmap/MainMap.jsx";

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
      <Route path="/" element={<App />} />
      <Route path="/main" element={<MainPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/map" element={<MainMap />} />
    </>
  )
);



ReactDOM.createRoot(document.getElementById("root")).render(
    //  <Provider store={store}>
        // <AuthInitializer>
            <RouterProvider router={router} />
        // </AuthInitializer>
    // </Provider>
);
