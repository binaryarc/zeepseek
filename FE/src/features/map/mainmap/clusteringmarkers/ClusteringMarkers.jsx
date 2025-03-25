import { useEffect, useRef } from "react";
import "./ClusteringMarkers.css";

function ClusteringMarkers({ map, saleData }) {
  const overlaysRef = useRef([]);

  useEffect(() => {
    if (!map || !window.kakao || !saleData) return;

    const drawGridMarkers = () => {
      const level = map.getLevel();

      // ✅ 레벨이 6보다 크면 (즉, 축소 상태) → grid 적용 안 함
      if (level > 6) {
        overlaysRef.current.forEach((o) => o.setMap(null));
        overlaysRef.current = [];
        return;
      }
    
      // ✅ 6 이하부터 grid 클러스터링 적용
      const gridSize =
        level <= 4 ? 0.001 :
        level <= 5 ? 0.002 :
        0.003;
    

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
