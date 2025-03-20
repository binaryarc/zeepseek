import React, { StrictMode } from 'react';
import { createRoot } from 'react-dom/client'
import {
  createBrowserRouter,
  createRoutesFromElements,
  Route,
  RouterProvider,
} from "react-router-dom";
import './index.css'
import App from './App.jsx'
import MainPage from './features/main/MainPage.jsx';
import LoginPage from './features/auth/LoginPage.jsx'

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
        <Route path="/" element={<App />} />
        <Route path="/main" element={<MainPage />} />
        <Route path="/login" element={<LoginPage />} />    </>
  )
)



createRoot(document.getElementById('root')).render(
  <StrictMode>
     <RouterProvider router={router} />
  </StrictMode>,
)
