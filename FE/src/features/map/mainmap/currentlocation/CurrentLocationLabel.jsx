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
import { useLocation } from "react-router-dom";

function CurrentLocationLabel({ map }) {
  const [locationName, setLocationName] = useState("");
  const dispatch = useDispatch();
  // const currentDongId = useSelector((state) => state.roomList.currentDongId);
  const searchLock = useSelector((state) => state.roomList.searchLock);
  const searchLockRef = useRef(searchLock); // ✅ useRef로 감싸서 최신값 유지
  // const selectedRoomType = useSelector(
  //   (state) => state.roomList.selectedRoomType
  // );
  const user = useSelector((state) => state.auth.user);
  const location = useLocation();
  const selectedRoomType = useSelector((state) => state.roomList.selectedRoomType);
  const idleSkipRef = useRef(false);

  useEffect(() => {
    // 외부에서 호출할 수 있게 라벨 갱신 함수 window에 등록
    window.updateCurrentLocationLabel = () => {
      const center = map.getCenter();
      const level = map.getLevel();
      const geocoder = new window.kakao.maps.services.Geocoder();
  
      geocoder.coord2RegionCode(
        center.getLng(),
        center.getLat(),
        (result, status) => {
          if (status !== window.kakao.maps.services.Status.OK) return;
  
          const regionData = result[1];
          const guName = regionData.region_2depth_name;
          const dongName = regionData.region_3depth_name.replaceAll(".", "·");
  
          setLocationName(level >= 6 ? guName : dongName);
        }
      );
    };
  
    return () => {
      // 페이지 나갈 때 정리
      delete window.updateCurrentLocationLabel;
    };
  }, [map]);
  
  useEffect(() => {
    if (location?.state?.selectedPropertyId) {
      console.log('여기들어옴?')
      idleSkipRef.current = true;

      setTimeout(() => {
        requestAnimationFrame(() => {
          const center = map.getCenter();
          const level = map.getLevel();

          const geocoder = new window.kakao.maps.services.Geocoder();

          geocoder.coord2RegionCode(
            center.getLng(),
            center.getLat(),
            (result, status) => {
              if (status !== window.kakao.maps.services.Status.OK) return;

              const regionData = result[1];
              const guName = regionData.region_2depth_name;
              const dongName = regionData.region_3depth_name.replaceAll(
                ".",
                "·"
              );

              setLocationName(level >= 6 ? guName : dongName);
            }
          );
        });
      }, 300);
    }
  }, [location?.state?.selectedPropertyId]);

  // ✅ searchLock 최신값 반영
  useEffect(() => {
    searchLockRef.current = searchLock;
  }, [searchLock]);

  useEffect(() => {
    if (!map || !window.kakao) return;

    const geocoder = new window.kakao.maps.services.Geocoder();

    const updateCenterAddress = () => {
      // ✅ 검색에서 들어온 경우 idle 처리 무시
      if (idleSkipRef.current) {
        idleSkipRef.current = false;
        return;
      }

      console.log("✅ idle 이벤트 발생!", map.getCenter());
      const center = map.getCenter();
      const level = map.getLevel();

      geocoder.coord2RegionCode(
        center.getLng(),
        center.getLat(),
        (result, status) => {
          console.log("📍역지오코딩 결과", result, status); // 이거 추가!
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
            // ✅ 라벨은 업데이트해야 하니까 여기서 setLocationName은 계속 실행
            window.isMovingBySearch = false;
            return;
          }

          // ✅ 일반 이동 시 동이 변경되면 fetch
          if (
            dongCode &&
            dongCode !== currentDongId &&
            !searchLockRef.current
          ) {
            dispatch(setCurrentDongId(dongCode));
            dispatch(setCurrentGuAndDongName({ guName, dongName }));

            dispatch(
              fetchRoomListByBounds({
                guName,
                dongName: level >= 6 ? "" : dongName,
                filter: selectedRoomType,
                userId: user?.idx ?? null,
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
  }, [map, selectedRoomType]);

  if (!locationName) return null;

  return <div className="location-label">{locationName}</div>;
}

export default CurrentLocationLabel;
