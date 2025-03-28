import React, { useEffect, useState } from "react";
import "./CurrentLocationLabel.css";
import { useDispatch, useSelector } from "react-redux";
import { useRef } from "react";
import {
  setCurrentDongId,
  fetchRoomListByBounds,
  setCurrentGuAndDongName
} from "../../../../store/slices/roomListSlice";

function CurrentLocationLabel({ map }) {
  const [locationName, setLocationName] = useState("");
  const dispatch = useDispatch();
  const currentDongId = useSelector((state) => state.roomList.currentDongId);
  const searchLock = useSelector((state) => state.roomList.searchLock);
  const searchLockRef = useRef(searchLock); // ✅ useRef로 감싸서 최신값 유지
  const selectedRoomType = useSelector((state) => state.roomList.selectedRoomType);

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
          if (status === window.kakao.maps.services.Status.OK) {
            console.log(result);
            const regionData = result[1]; // result[1]은 행정동 정보
            const dongCode = regionData.code.slice(0, -2); // 👉 행정동 코드 (dongId)
            const guName = regionData.region_2depth_name;
            const dongName = regionData.region_3depth_name.replaceAll(".", "·");

            // UI에 표시할 동/구 이름 설정
            if (level >= 6) {
              setLocationName(regionData.region_2depth_name); // 구
            } else {
              setLocationName(regionData.region_3depth_name); // 동
            }

            // ✅ 여기서 검색 이동이면 무시
            if (window.isMovingBySearch) {
              console.log("🔒 검색 이동 중 → fetchRoomListByBounds 무시", dongName);
              return;
            }

            // ✅ 지도 직접 이동이면 실행
            
            if (dongCode && dongCode !== currentDongId) {
              console.log("🔓 지도 이동 중 → fetchRoomListByBounds 실행", dongName);
              dispatch(setCurrentDongId(dongCode));
              if (level >=6 ) {
                console.log('구바운드')
                dispatch(fetchRoomListByBounds({ guName, dongName: '', filter: selectedRoomType }));
              } else if (level < 6 && level > 3) {
                console.log('동바운드')
                dispatch(fetchRoomListByBounds({ guName, dongName, filter: selectedRoomType }));
              }
              dispatch(setCurrentGuAndDongName({ guName, dongName }));
            }

            // // ✅ 현재 저장된 dongId와 다르면 요청
            // if (dongCode && dongCode !== currentDongId) {
            //   if (searchLockRef.current) {
            //     // 🔓 검색으로 인한 이동이면 그냥 무시
            //     dispatch(setSearchLock(false));
            //     console.log("검색으로 인한 이동이라 무시합니다.");
            //   } else {
            //     console.log("여기로 너 안오잖아");
            //     dispatch(setCurrentDongId(dongCode));
            //     console.log(dongName);
            //     dispatch(fetchRoomListByBounds({ guName, dongName }));
            //   }
            // }

            
          }
        }
      );
    };

    updateCenterAddress(); // 초기 위치 설정
    window._idleHandler = updateCenterAddress;
    window.kakao.maps.event.addListener(map, "idle", updateCenterAddress);

    return () => {
      window.kakao.maps.event.removeListener(map, "idle", updateCenterAddress);
    };
  }, [map, currentDongId, dispatch]);

  if (!locationName) return null;

  return <div className="location-label">{locationName}</div>;
}

export default CurrentLocationLabel;
