// store/authSlice.js
import { createSlice } from "@reduxjs/toolkit";

const initialState = {
  accessToken: null,
  user: null,
  // isAuthenticated: false, // ✅ 추가
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setAccessToken: (state, action) => {
      state.accessToken = action.payload;
      // state.isAuthenticated = !!action.payload; // ✅ 자동 설정
    },
    setUser: (state, action) => {
      state.user = action.payload;
    },
    logout: (state) => {
      state.accessToken = null;
      state.user = null;
      localStorage.removeItem("isAuthenticated");
      // state.isAuthenticated = false; // ✅ 로그아웃 시 false
    },
  },
});

export const { setAccessToken, setUser, logout } = authSlice.actions;
export default authSlice.reducer;
