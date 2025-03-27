import { useEffect, useRef } from "react";
import { fetchGridSaleCountsByType } from "../../../../../common/api/api";
import "./GridClustering.css";
import { generateGridCells } from "./useGridCells";


function GridClustering({ map }) {
  const polygonsRef = useRef([]);
  const overlaysRef = useRef([]);

  useEffect(() => {
    if (!map || !window.kakao) return;

    const drawGridClusters = async () => {
      
      const level = map.getLevel();
      if (level > 3) return;

      polygonsRef.current.forEach(p => p.setMap(null));
      overlaysRef.current.forEach(o => o.setMap(null));
      polygonsRef.current = [];
      overlaysRef.current = [];

      const proj = map.getProjection();
      const bounds = map.getBounds();
      const sw = bounds.getSouthWest();
      const ne = bounds.getNorthEast();

      const swPoint = proj.containerPointFromCoords(sw);
      const nePoint = proj.containerPointFromCoords(ne);

      const latPerPx = Math.abs(ne.getLat() - sw.getLat()) / Math.abs(nePoint.y - swPoint.y);
      const lngPerPx = Math.abs(ne.getLng() - sw.getLng()) / Math.abs(nePoint.x - swPoint.x);

      const gridSizePx = 80;
      const gridSizeLat = latPerPx * gridSizePx;
      const gridSizeLng = lngPerPx * gridSizePx;

      const cells = generateGridCells(bounds, gridSizeLat, gridSizeLng);

      const selectedType = "one-room";

      console.log("grid: ", cells)

      const result = await fetchGridSaleCountsByType(cells, selectedType);

      console.log("result: ", result)

      result.forEach(cell => {
        const { minLat, maxLat, minLng, maxLng, count } = cell;

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

        const centerLat = (minLat + maxLat) / 2;
        const centerLng = (minLng + maxLng) / 2;

        const content = `
          <div class="grid-count">${count}</div>
        `;

        const overlay = new window.kakao.maps.CustomOverlay({
          position: new window.kakao.maps.LatLng(centerLat, centerLng),
          content,
          yAnchor: 1,
          map,
        });

        overlaysRef.current.push(overlay);
      });
    };

    drawGridClusters();
    window.kakao.maps.event.addListener(map, "idle", drawGridClusters);

    return () => {
      polygonsRef.current.forEach(p => p.setMap(null));
      overlaysRef.current.forEach(o => o.setMap(null));
      polygonsRef.current = [];
      overlaysRef.current = [];
      window.kakao.maps.event.removeListener(map, "idle", drawGridClusters);
    };
  }, [map]);

  return null;
}

export default GridClustering;
