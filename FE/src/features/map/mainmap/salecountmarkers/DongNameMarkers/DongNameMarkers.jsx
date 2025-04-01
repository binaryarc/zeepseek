import { useEffect, useRef } from "react";
import dongData from "../../../../../assets/data/seoul_dong_centroids_from_geojson.json";

function DongNameMarkers({ map }) {
  const overlaysRef = useRef([]);

  useEffect(() => {
    if (!map || !window.kakao) return;

    const drawDongNames = () => {
      const level = map.getLevel()
      
      // ✅ 줌 레벨이 6 이상이면 동 이름 안 보이게
      if (level >= 6) {
        overlaysRef.current.forEach((o) => o.setMap(null));
        return;
      }

      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];

      dongData.forEach((region) => {
        const position = new window.kakao.maps.LatLng(region.lat, region.lng);
        const splitName = region.name.trim().split(" ");
        const displayName = splitName[splitName.length - 1];

        const contentDiv = document.createElement("div");
        contentDiv.className = "dong-name-overlay";
        contentDiv.innerHTML = `<div class="region-label">${displayName}</div>`;

        const overlay = new window.kakao.maps.CustomOverlay({
          position,
          content: contentDiv,
          yAnchor: 1,
          map,
        });

        overlaysRef.current.push(overlay);
      });
    };

    drawDongNames();
    window.kakao.maps.event.addListener(map, "idle", drawDongNames);

    return () => {
      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];
      window.kakao.maps.event.removeListener(map, "idle", drawDongNames);
    };
  }, [map]);

  return null;
}

export default DongNameMarkers;
