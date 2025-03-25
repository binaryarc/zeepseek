import { useEffect, useRef } from "react";
import "./ClusteringMarkers.css";

function ClusteringMarkers({ map, saleData }) {
  const overlaysRef = useRef([]);

  useEffect(() => {
    if (!map || !window.kakao || !saleData) return;

    const drawGridMarkers = () => {
      const level = map.getLevel();

      console.log("level: ", level)

      // ✅ 줌 레벨에 따라 그리드 간격 조정 (대략적인 경도/위도 단위)
      const gridSize = level >= 14 ? 0.002 : level >= 12 ? 0.005 : 0.01;

      // ✅ 기존 마커 제거
      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];

      // ✅ 그리드 셀별 매물 집계
      const gridMap = new Map();

      saleData.forEach((room) => {
        const latIdx = Math.floor(room.lat / gridSize);
        const lngIdx = Math.floor(room.lng / gridSize);
        const key = `${latIdx}_${lngIdx}`;

        if (!gridMap.has(key)) {
          gridMap.set(key, { count: 0, latSum: 0, lngSum: 0 });
        }

        const cell = gridMap.get(key);
        cell.count += 1;
        cell.latSum += room.lat;
        cell.lngSum += room.lng;
      });

      // ✅ 각 셀 중심에 클러스터 마커 생성
      for (const [key, cell] of gridMap.entries()) {
        const centerLat = cell.latSum / cell.count;
        const centerLng = cell.lngSum / cell.count;

        const content = `
          <div class="marker-container">
            <div class="circle-count">${cell.count}</div>
          </div>
        `;

        const overlay = new window.kakao.maps.CustomOverlay({
          position: new window.kakao.maps.LatLng(centerLat, centerLng),
          content,
          yAnchor: 1,
          map,
        });

        overlaysRef.current.push(overlay);
      }
    };

    drawGridMarkers();
    window.kakao.maps.event.addListener(map, "idle", drawGridMarkers);

    return () => {
      overlaysRef.current.forEach((o) => o.setMap(null));
      window.kakao.maps.event.removeListener(map, "idle", drawGridMarkers);
    };
  }, [map, saleData]);

  return null;
}

export default ClusteringMarkers;
