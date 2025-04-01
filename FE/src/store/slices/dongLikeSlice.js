import { createSlice } from "@reduxjs/toolkit";

const dongLikeSlice = createSlice({
  name: "dongLike",
  initialState: {},
  reducers: {
    likeDong: (state, action) => {
        const id = String(action.payload);   // 🔹 문자열로 변환
        state[id] = true;
      },
      unlikeDong: (state, action) => {
        const id = String(action.payload);   // 🔹 문자열로 변환
        state[id] = false;
      },
  },
});

export const { likeDong, unlikeDong } = dongLikeSlice.actions;
export default dongLikeSlice.reducer;