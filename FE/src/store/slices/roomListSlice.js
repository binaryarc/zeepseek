import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { getPropertiesByDongId } from "../../common/api/api"; // ✅ 변경된 부분

// ✅ dongId 기반 매물 조회
export const fetchRoomListByDongId = createAsyncThunk(
    "roomList/fetchByDong",
    async (dongId) => {
      const res = await getPropertiesByDongId(dongId); // ✅ api.js에서 가져온 함수 사용
      return res.properties;
    }
  );

const roomListSlice = createSlice({
  name: "roomList",
  initialState: {
    rooms: [],
    currentDongId: null,
    loading: false,
    searchLock: false, // ✅ 중복 요청 방지용
  },
  reducers: {
    // ✅ 현재 지도 중심 dongId 저장 (중복 요청 방지용)
    setCurrentDongId: (state, action) => {
      state.currentDongId = action.payload;
    },
    setSearchLock: (state, action) => {
      state.searchLock = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchRoomListByDongId.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchRoomListByDongId.fulfilled, (state, action) => {
        state.rooms = action.payload || [];
        state.loading = false;
      })
      .addCase(fetchRoomListByDongId.rejected, (state) => {
        state.loading = false;
      });
  },
});

export const { setCurrentDongId,setSearchLock } = roomListSlice.actions;
export default roomListSlice.reducer;
