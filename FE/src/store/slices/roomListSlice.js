import { createSlice, createAsyncThunk } from "@reduxjs/toolkit";
import {
  searchProperties,
  fetchPropertiesByBounds,
} from "../../common/api/api"; // ✅ 변경된 부분

// ✅ keyword 기반 매물 검색 (검색 + 지도 이동 모두 사용)
export const fetchRoomList = createAsyncThunk(
  "roomList/fetchByKeyword",
  async ({ keyword, filter }) => {
    const res = await searchProperties(keyword, filter);
    return res.properties;
  }
);

// ✅ 지도 이동 시 bounds 기반 매물 조회 API
export const fetchRoomListByBounds = createAsyncThunk(
  "roomList/fetchByBounds",
  async ({ guName, dongName, filter }) => {
    const res = await fetchPropertiesByBounds(guName, dongName, filter);
    return res.properties;
  }
);

const roomListSlice = createSlice({
  name: "roomList",
  initialState: {
    rooms: [],
    currentPage: 1,
    pageSize: 15,
    currentDongId: null,
    loading: false,
    searchLock: false, // ✅ 중복 요청 방지용
    selectedPropertyId: null,
    selectedRoomType: "원룸/투룸", // ✅ 선택된 방 종류
    currentGuName: null,
    currentDongName: null,
  },
  reducers: {
    setCurrentPage: (state, action) => {
      state.currentPage = action.payload;
    },
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
    setSelectedRoomType: (state, action) => {
      state.selectedRoomType = action.payload;
    },
    setCurrentGuAndDongName: (state, action) => {
      state.currentGuName = action.payload.guName;
      state.currentDongName = action.payload.dongName;
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
        state.currentPage = 1; // ✅ 검색 시 항상 첫 페이지로 초기화
      })
      .addCase(fetchRoomListByBounds.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchRoomListByBounds.fulfilled, (state, action) => {
        state.rooms = action.payload || [];
        state.loading = false;
        state.currentPage = 1;
      })
      .addCase(fetchRoomList.rejected, (state) => {
        state.loading = false;
      })
      .addCase(fetchRoomListByBounds.rejected, (state) => {
        state.loading = false;
      });
  },
});

export const {
  setCurrentPage,
  setCurrentDongId,
  setSearchLock,
  setSelectedPropertyId,
  setSelectedRoomType,
  setCurrentGuAndDongName,
} = roomListSlice.actions;
export default roomListSlice.reducer;
