import { configureStore } from "@reduxjs/toolkit";
import authReducer from "./slices/authSlice";
import roomListReducer from "./slices/roomListSlice";

const store = configureStore({
  reducer: {
    auth: authReducer,
    roomList: roomListReducer,
  },
});

export default store;
