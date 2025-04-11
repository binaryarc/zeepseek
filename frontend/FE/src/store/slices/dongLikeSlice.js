import { createSlice } from "@reduxjs/toolkit";

const dongLikeSlice = createSlice({
  name: "dongLike",
  initialState: {},
  reducers: {
    likeDong: (state, action) => {
      const id = String(action.payload); // 🔹 문자열로 변환
      state[id] = true;
    },
    unlikeDong: (state, action) => {
      const id = String(action.payload); // 🔹 문자열로 변환
      state[id] = false;
    },
    setDongLikes: (state, action) => {
      const likedMap = {};
      action.payload.forEach((dong) => {
        likedMap[dong.dongId] = true;
      });
      return likedMap;
    },
  },
});

export const { likeDong, unlikeDong, setDongLikes } = dongLikeSlice.actions;
export default dongLikeSlice.reducer;
