import { useEffect, useRef } from "react";
import guData from "../../../../assets/data/seoul_gu_centroids_from_geojson.json";
import dongData from "../../../../assets/data/seoul_dong_centroids_from_geojson.json";
import "./SaleCountMarkers.css";
import {
  fetchDongPropertyCounts,
  fetchGuPropertyCounts,
} from "../../../../common/api/api";
import { useState } from "react";
import GridClustering from "./GridClustering/GridClustering";
import { useSelector } from "react-redux";

function SaleCountMarkers({ map }) {
  const overlaysRef = useRef([]); // ✅ useRef로 오버레이 관리
  const selectedRoomType = useSelector(
    (state) => state.roomList.selectedRoomType
  );
  const roomTypeMap = {
    "원룸/투룸": "one-room",
    "주택/빌라": "house",
    오피스텔: "office",
  };
  const filterKey = roomTypeMap[selectedRoomType];

  const likedDongs = useSelector((state) => state.dongLike); // ✔ 찜한 동 ID 배열

  // const [dongId, setDongId] = useState(null);
  const [level, setLevel] = useState(null);

  useEffect(() => {
    if (!map || !window.kakao) return;

    const drawMarkers = async () => {
      const currentLevel = map.getLevel();
      setLevel(currentLevel);

      if (currentLevel >= 9) {
        // 기존 오버레이 제거
        overlaysRef.current.forEach((o) => o.setMap(null));
        overlaysRef.current = [];
      
        // 필터별 전체 매물 수
        const totalCounts = {
          "one-room": 49055,
          "house": 73188,
          "office": 13589,
        };
      
        const count = totalCounts[filterKey];
        if (!count) return;
      
        const position = new window.kakao.maps.LatLng(37.5405, 126.978); // 서울 시청 위치
      
        const contentDiv = document.createElement("div");
        contentDiv.className = "marker-wrapper";
        contentDiv.innerHTML = `
          <div class="marker-container">
            <div class="circle-count">${count}</div>
            <div class="region-label">서울시</div>
          </div>
        `;
      
        const overlay = new window.kakao.maps.CustomOverlay({
          position,
          content: contentDiv,
          yAnchor: 1,
          map,
        });
      
        overlaysRef.current.push(overlay);
        return; // ✅ 여기서 종료 (구/동 마커 안 그리게)
      }
      

      if (currentLevel <= 3) {
        // ✅ 기존 오버레이 모두 제거
        overlaysRef.current.forEach((o) => o.setMap(null));
        overlaysRef.current = [];
        return;
      }

      const targetData = currentLevel >= 6 ? guData : dongData;

      let countData = [];

      if (currentLevel >= 6) {
        if (!filterKey) return;
        countData = await fetchGuPropertyCounts(filterKey);
      } else if (currentLevel < 6 && currentLevel >= 3) {
        if (!filterKey) return;
        countData = await fetchDongPropertyCounts(filterKey); // ✅ 파라미터 전달
      }

      // 📌 데이터 매핑
      const countMap = {};
      countData.forEach((item) => {
        if (currentLevel >= 6) {
          countMap[item.guName] = item.propertyCount;
        } else if (currentLevel > 3) {
          countMap[String(item.dongId)] = item.propertyCount;
        }
      });

      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];

      targetData.forEach((region) => {
        const position = new window.kakao.maps.LatLng(region.lat, region.lng);
        const splitName = region.name.trim().split(" ");
        const displayName = splitName[splitName.length - 1];

        let count = 0;
        let labelContent = "";
        if (currentLevel >= 6) {
          // 구 단위: 하트 표시 X
          count = countMap[region.name] || 0;

          labelContent = `
            <div class="marker-container">
              <div class="circle-count">${count}</div>
              <div class="region-label">${displayName}</div>
            </div>
          `;
        } else if (currentLevel > 3) {
          // 동 단위: 하트 표시 O
          const dongId = region.dongId;
          count = countMap[dongId] || 0;
          const isLiked = likedDongs[String(dongId)];

          labelContent = `
          <div class="marker-container">
            <div class="circle-count">${count}</div>
            <div class="region-label">${displayName}</div>
            ${isLiked ? '<div class="heart-overlay">❤️</div>' : ""}
          </div>
        `;
        }

        const contentDiv = document.createElement("div");
        contentDiv.className = "marker-wrapper";
        contentDiv.innerHTML = labelContent;

        const overlay = new window.kakao.maps.CustomOverlay({
          position,
          content: contentDiv,
          yAnchor: 1,
          map,
        });

        // 🔥 클릭 이벤트는 DOM에 직접 바인딩
        contentDiv.addEventListener("click", () => {
          map.setCenter(position);
          map.setLevel(5);
        });

        overlaysRef.current.push(overlay); // ✅ 최신 목록에 추가
      });
    };

    drawMarkers();
    window.kakao.maps.event.addListener(map, "idle", drawMarkers);

    return () => {
      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];
      window.kakao.maps.event.removeListener(map, "idle", drawMarkers);
    };
  }, [map, filterKey, likedDongs]);

  return level <= 3 ? <GridClustering map={map} /> : null;
}

export default SaleCountMarkers;
