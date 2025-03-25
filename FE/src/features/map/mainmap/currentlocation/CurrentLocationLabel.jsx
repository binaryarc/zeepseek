import React, { useEffect, useState } from "react";
import "./CurrentLocationLabel.css";
import { useDispatch, useSelector } from "react-redux";
import {
  setCurrentDongId,
  fetchRoomListByDongId,
  setSearchLock,
} from "../../../../store/slices/roomListSlice";

function CurrentLocationLabel({ map }) {
  const [locationName, setLocationName] = useState("");
  const dispatch = useDispatch();
  const currentDongId = useSelector((state) => state.roomList.currentDongId);
  const searchLock = useSelector((state) => state.roomList.searchLock);

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
          if (status === window.kakao.maps.services.Status.OK) {
            console.log(result);
            const regionData = result[1]; // result[1]은 행정동 정보
            const dongCode = regionData.code.slice(0, -2); // 👉 행정동 코드 (dongId)

            // ✅ 현재 저장된 dongId와 다르면 요청
            if (dongCode && dongCode !== currentDongId) {
              if (searchLock) {
                // 🔓 검색으로 인한 이동이면 그냥 무시
                dispatch(setSearchLock(false));
              } else {
                dispatch(setCurrentDongId(dongCode));
                dispatch(fetchRoomListByDongId(dongCode));
              }
            }

            // UI에 표시할 동/구 이름 설정
            if (level >= 6) {
              setLocationName(regionData.region_2depth_name); // 구
            } else {
              setLocationName(regionData.region_3depth_name); // 동
            }
          }
        }
      );
    };

    updateCenterAddress(); // 초기 위치 설정
    window.kakao.maps.event.addListener(map, "idle", updateCenterAddress);

    return () => {
      window.kakao.maps.event.removeListener(map, "idle", updateCenterAddress);
    };
  }, [map, currentDongId, dispatch]);

  if (!locationName) return null;

  return <div className="location-label">{locationName}</div>;
}

export default CurrentLocationLabel;
