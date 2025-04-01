import { createSlice } from "@reduxjs/toolkit";

const dongLikeSlice = createSlice({
  name: "dongLike",
  initialState: {},
  reducers: {
    likeDong: (state, action) => {
      console.log("👍 likeDong 호출됨:", action.payload);  // ✅ 로그
      state[action.payload] = true;
    },
    unlikeDong: (state, action) => {
      console.log("👍 likeDong 호출됨:", action.payload);  // ✅ 로그
      state[action.payload] = false;
    },
  },
});

export const { likeDong, unlikeDong } = dongLikeSlice.actions;
export default dongLikeSlice.reducer;