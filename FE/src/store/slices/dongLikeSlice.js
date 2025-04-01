import { createSlice } from "@reduxjs/toolkit";

const dongLikeSlice = createSlice({
  name: "dongLike",
  initialState: {},
  reducers: {
    likeDong: (state, action) => {
      state[action.payload] = true;
    },
    unlikeDong: (state, action) => {
      state[action.payload] = false;
    },
  },
});

export const { likeDong, unlikeDong } = dongLikeSlice.actions;
export default dongLikeSlice.reducer;