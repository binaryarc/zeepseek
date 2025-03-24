import { useEffect, useRef } from "react";
import guData from "../../../../assets/data/seoul_gu_centroids_from_geojson.json";
import dongData from "../../../../assets/data/seoul_dong_centroids_from_geojson.json";
import "./SaleCountMarkers.css";

function SaleCountMarkers({ map }) {
  const overlaysRef = useRef([]); // ✅ useRef로 오버레이 관리

  useEffect(() => {
    if (!map || !window.kakao) return;

    const drawMarkers = () => {
      const level = map.getLevel();
      const isGuLevel = level >= 7;

      // ✅ 기존 오버레이 모두 제거
      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];

      const targetData = isGuLevel ? guData : dongData;

      targetData.forEach((region) => {
        const count = Math.floor(Math.random() * 5000 + 100);
        const position = new window.kakao.maps.LatLng(region.lat, region.lng);

        // ✅ '공덕동', '마포구' 등 마지막 단어만 추출
        const splitName = region.name.trim().split(" ");
        const displayName = splitName[splitName.length - 1];

        const content = `
          <div class="marker-container">
            <div class="circle-count">${count}</div>
            <div class="region-label">${displayName}</div>
          </div>
        `;

        const overlay = new window.kakao.maps.CustomOverlay({
          position,
          content,
          yAnchor: 1,
          map,
        });

        overlaysRef.current.push(overlay); // ✅ 최신 목록에 추가
      });
    };

    drawMarkers();
    window.kakao.maps.event.addListener(map, "idle", drawMarkers);

    return () => {
      overlaysRef.current.forEach((o) => o.setMap(null));
      window.kakao.maps.event.removeListener(map, "idle", drawMarkers);
    };
  }, [map]);

  return null;
}

export default SaleCountMarkers;
