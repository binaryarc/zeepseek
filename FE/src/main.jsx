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

const router = createBrowserRouter(
  createRoutesFromElements(
    <>
        <Route path="/" element={<App />} />
        <Route path="/main" element={<MainPage />} />
    </>
  )
)



createRoot(document.getElementById('root')).render(
  <StrictMode>
     <RouterProvider router={router} />
  </StrictMode>,
)
