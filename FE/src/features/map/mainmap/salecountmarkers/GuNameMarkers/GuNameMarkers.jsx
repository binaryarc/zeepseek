import { useEffect, useRef } from "react";
import guData from "../../../../../assets/data/seoul_gu_centroids_from_geojson.json";

function GuNameMarkers({ map }) {
  const overlaysRef = useRef([]);

  useEffect(() => {
    if (!map || !window.kakao) return;

    const drawGuNames = () => {
      const level = map.getLevel();
      if (level < 6) {
        overlaysRef.current.forEach((o) => o.setMap(null));
        return;
      }

      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];

      guData.forEach((region) => {
        const position = new window.kakao.maps.LatLng(region.lat, region.lng);
        const displayName = region.name.trim();

        const contentDiv = document.createElement("div");
        contentDiv.className = "gu-name-overlay";
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

    drawGuNames();
    window.kakao.maps.event.addListener(map, "idle", drawGuNames);

    return () => {
      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];
      window.kakao.maps.event.removeListener(map, "idle", drawGuNames);
    };
  }, [map]);

  return null;
}

export default GuNameMarkers;