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
  const polygonsRef = useRef([]);
  const overlaysRef = useRef([]);

  useEffect(() => {
    if (!map || !window.kakao) return;

    const drawGridMarkers = () => {
      const level = map.getLevel();

      if (level > 3) return; // ✅ 3이하일 때만 그리기


      // 지도에서 기존 폴리곤 및 오버레이 제거
      polygonsRef.current.forEach(p => p.setMap(null));
      overlaysRef.current.forEach(o => o.setMap(null));
      polygonsRef.current = [];
      overlaysRef.current = [];
      

      const saleData = generateRandomSaleData(500);
      const gridMap = new Map();

      const proj = map.getProjection();
      const bounds = map.getBounds();
      const sw = bounds.getSouthWest();
      const ne = bounds.getNorthEast();

      const swPoint = proj.containerPointFromCoords(sw);
      const nePoint = proj.containerPointFromCoords(ne);

      const latPerPx = Math.abs(ne.getLat() - sw.getLat()) / Math.abs(nePoint.y - swPoint.y);
      const lngPerPx = Math.abs(ne.getLng() - sw.getLng()) / Math.abs(nePoint.x - swPoint.x);

      const gridSizePx = 80; // 원하는 픽셀 크기
      const gridSizeLat = latPerPx * gridSizePx;
      const gridSizeLng = lngPerPx * gridSizePx;


      saleData.forEach(({ lat, lng }) => {
        const latIdx = Math.floor(lat / gridSizeLat);
        const lngIdx = Math.floor(lng / gridSizeLng);
        const key = `${latIdx}_${lngIdx}`;
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

      for (const [key, cell] of gridMap.entries()) {
        if (cell.count === 0) continue;

        const [latIdx, lngIdx] = key.split("_").map(Number);

        // const latIdx = +key.split("_")[0];
        // const lngIdx = +key.split("_")[1];

        const minLat = latIdx * gridSizeLat;
        const maxLat = (latIdx + 1) * gridSizeLat;
        const minLng = lngIdx * gridSizeLng;
        const maxLng = (lngIdx + 1) * gridSizeLng;

        const rectPath = [
          new window.kakao.maps.LatLng(minLat, minLng),
          new window.kakao.maps.LatLng(minLat, maxLng),
          new window.kakao.maps.LatLng(maxLat, maxLng),
          new window.kakao.maps.LatLng(maxLat, minLng),
        ];

        const polygon = new window.kakao.maps.Polygon({
          path: [rectPath],
          strokeWeight: 1,
          strokeColor: "#fb8c00",
          strokeOpacity: 0.8,
          fillColor: "rgba(255,167,38,0.3)",
          fillOpacity: 0.5,
        });
        polygon.setMap(map);
        polygonsRef.current.push(polygon);

        const centerLat = cell.latSum / cell.count;
        const centerLng = cell.lngSum / cell.count;

        const content = `
          <div class="grid-count">${cell.count}</div>
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
      polygonsRef.current.forEach(p => p.setMap(null));
      overlaysRef.current.forEach(o => o.setMap(null));
      polygonsRef.current = [];
      overlaysRef.current = [];
      window.kakao.maps.event.removeListener(map, "idle", drawGridMarkers);
    };
  }, [map]);

  return null;
}

export default GridClusterMarkers;
