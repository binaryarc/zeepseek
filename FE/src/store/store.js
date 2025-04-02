import { configureStore } from "@reduxjs/toolkit";
import authReducer from "./slices/authSlice";
import roomListReducer from "./slices/roomListSlice";
import dongLikeReducer from "./slices/dongLikeSlice"

const store = configureStore({
  reducer: {
    auth: authReducer,
    roomList: roomListReducer,
    dongLike: dongLikeReducer, // ✅ 반드시 여기에 추가돼야 함
  },
});

export default store;
