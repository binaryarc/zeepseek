import { useEffect, useRef } from "react";
import "./GridClustering.css";

// ✅ 랜덤 매물 데이터 생성 함수
const generateRandomSaleData = (count = 500) => {
  const latMin = 37.48;
  const latMax = 37.62;
  const lngMin = 126.90;
  const lngMax = 127.03;

  const data = [];
  for (let i = 0; i < count; i++) {
    const lat = Math.random() * (latMax - latMin) + latMin;
    const lng = Math.random() * (lngMax - lngMin) + lngMin;
    data.push({ id: i + 1, lat, lng });
  }
  return data;
};

function GridClusterMarkers({ map }) {
  const overlaysRef = useRef([]);

  useEffect(() => {
    if (!map || !window.kakao) return;

    const drawGridMarkers = () => {
      const level = map.getLevel();

      if (level > 3) return; // ✅ 3이하일 때만 그리기

      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];

      const gridSize = 0.002;
      const saleData = generateRandomSaleData(500);
      const gridMap = new Map();

      saleData.forEach(({ lat, lng }) => {
        const key = `${Math.floor(lat / gridSize)}_${Math.floor(
          lng / gridSize
        )}`;
        const cell = gridMap.get(key) || {
          count: 0,
          latSum: 0,
          lngSum: 0,
        };
        cell.count++;
        cell.latSum += lat;
        cell.lngSum += lng;
        gridMap.set(key, cell);
      });

      for (const cell of gridMap.values()) {
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
      overlaysRef.current = [];
      window.kakao.maps.event.removeListener(map, "idle", drawGridMarkers);
    };
  }, [map]);

  return null;
}

export default GridClusterMarkers;
