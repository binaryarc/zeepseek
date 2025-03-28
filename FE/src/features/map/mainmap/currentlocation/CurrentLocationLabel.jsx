import React, { useEffect, useState } from "react";
import "./CurrentLocationLabel.css";
import { useDispatch, useSelector } from "react-redux";
import { useRef } from "react";
import {
  setCurrentDongId,
  fetchRoomListByBounds,
  setCurrentGuAndDongName,
} from "../../../../store/slices/roomListSlice";
import store from "../../../../store/store";

function CurrentLocationLabel({ map }) {
  const [locationName, setLocationName] = useState("");
  const dispatch = useDispatch();
  // const currentDongId = useSelector((state) => state.roomList.currentDongId);
  const searchLock = useSelector((state) => state.roomList.searchLock);
  const searchLockRef = useRef(searchLock); // ✅ useRef로 감싸서 최신값 유지
  // const selectedRoomType = useSelector(
  //   (state) => state.roomList.selectedRoomType
  // );

  // ✅ searchLock 최신값 반영
  useEffect(() => {
    searchLockRef.current = searchLock;
  }, [searchLock]);

  useEffect(() => {
    if (!map || !window.kakao) return;

    const geocoder = new window.kakao.maps.services.Geocoder();

    const updateCenterAddress = () => {
      const center = map.getCenter();
      const level = map.getLevel();

      geocoder.coord2RegionCode(
        center.getLng(),
        center.getLat(),
        (result, status) => {
          if (status !== window.kakao.maps.services.Status.OK) return;

          const regionData = result[1];
          const dongCode = regionData.code.slice(0, -2);
          const guName = regionData.region_2depth_name;
          const dongName = regionData.region_3depth_name.replaceAll(".", "·");

          // UI 라벨 표시
          setLocationName(level >= 6 ? guName : dongName);

          const { currentDongId } = store.getState().roomList;
          const selectedRoomType = store.getState().roomList.selectedRoomType;

          // ✅ 검색 이동이면 fetchRoomListByBounds 하지 않고 넘김
          if (window.isMovingBySearch) {
            console.log("🔒 검색 이동 중 → fetchRoomListByBounds 스킵");
            window.isMovingBySearch = false;
            return;
          }

          // ✅ 일반 이동 시 동이 변경되면 fetch
          if (dongCode && dongCode !== currentDongId) {
            dispatch(setCurrentDongId(dongCode));
            dispatch(setCurrentGuAndDongName({ guName, dongName }));

            dispatch(
              fetchRoomListByBounds({
                guName,
                dongName: level >= 6 ? "" : dongName,
                filter: selectedRoomType,
              })
            );
          }
        }
      );
    };

    window.kakao.maps.event.addListener(map, "idle", updateCenterAddress);
    updateCenterAddress();

    return () => {
      window.kakao.maps.event.removeListener(map, "idle", updateCenterAddress);
    };
  }, [map]);

  if (!locationName) return null;

  return <div className="location-label">{locationName}</div>;
}

export default CurrentLocationLabel;
