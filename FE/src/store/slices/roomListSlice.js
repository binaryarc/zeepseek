import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import { searchProperties, fetchPropertiesByBounds } from "../../common/api/api"; // ✅ 변경된 부분

// ✅ keyword 기반 매물 검색 (검색 + 지도 이동 모두 사용)
export const fetchRoomList = createAsyncThunk(
    "roomList/fetchByKeyword",
    async (keyword) => {
      const res = await searchProperties(keyword);
      return res.properties;
    }
  );

// ✅ 지도 이동 시 bounds 기반 매물 조회 API
export const fetchRoomListByBounds = createAsyncThunk(
    "roomList/fetchByBounds",
    async ({ guName, dongName }) => {
      const res = await fetchPropertiesByBounds(guName, dongName);
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
    selectedPropertyId: null,
  },
  reducers: {
    // ✅ 현재 지도 중심 dongId 저장 (중복 요청 방지용)
    setCurrentDongId: (state, action) => {
      state.currentDongId = action.payload;
    },
    setSearchLock: (state, action) => {
      state.searchLock = action.payload;
      console.log("searchLock", state.searchLock);
    },
    setSelectedPropertyId: (state, action) => {
      state.selectedPropertyId = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
    .addCase(fetchRoomList.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchRoomList.fulfilled, (state, action) => {
        state.rooms = action.payload || [];
        state.loading = false;
      })
      .addCase(fetchRoomListByBounds.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchRoomListByBounds.fulfilled, (state, action) => {
        state.rooms = action.payload || [];
        state.loading = false;
      })
      .addCase(fetchRoomList.rejected, (state) => {
        state.loading = false;
      })
      .addCase(fetchRoomListByBounds.rejected, (state) => {
        state.loading = false;
      });
  },
});

export const { setCurrentDongId,setSearchLock, setSelectedPropertyId } = roomListSlice.actions;
export default roomListSlice.reducer;
