import { useEffect, useRef } from "react";
import { fetchGridSaleCountsByType } from "../../../../../common/api/api";
import "./GridClustering.css";
import { generateGridCells } from "./useGridCells";
import { useSelector } from "react-redux";


function GridClustering({ map }) {
  const polygonsRef = useRef([]);
  const overlaysRef = useRef([]);
  const popupRef = useRef(null);
  const selectedRoomType = useSelector(
    (state) => state.roomList.selectedRoomType)

  const roomTypeMap = {
      "원룸/투룸": "one-room",
      "주택/빌라": "house",
      "오피스텔": "office",
    };
  const filterKey = roomTypeMap[selectedRoomType];

  useEffect(() => {
    if (!map || !window.kakao) return;

    const drawGridClusters = async () => {
      
      const level = map.getLevel();
      if (level > 3) return;

      polygonsRef.current.forEach(p => p.setMap(null));
      overlaysRef.current.forEach(o => o.setMap(null));
      if (popupRef.current) popupRef.current.setMap(null);
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

      const gridSizePx = 160;
      const gridSizeLat = latPerPx * gridSizePx;
      const gridSizeLng = lngPerPx * gridSizePx;

      const cells = generateGridCells(bounds, gridSizeLat, gridSizeLng);

      console.log("grid: ", cells)

      const result = await fetchGridSaleCountsByType(cells, filterKey);
      console.log(filterKey)

      console.log("result: ", result)

      result.forEach(item => {
        const { cell, properties } = item;
        const { minLat, maxLat, minLng, maxLng } = cell;

        // grid 안에 매물이 없으면 return
        if (!properties || properties.length === 0) return;

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
          <div class="grid-count-wrapper">
            <div class="grid-count">${properties.length}</div>
          </div>
        `;

        const overlay = new window.kakao.maps.CustomOverlay({
          position: new window.kakao.maps.LatLng(centerLat, centerLng),
          content,
          xAnchor: 0.5,
          yAnchor: 0.5,
          map,
        });

        window.kakao.maps.event.addListener(overlay, "click", () => {
          if (popupRef.current) popupRef.current.setMap(null);

          const listHtml = properties
            .map(p => `<li>${p.address} - ${p.price}</li>`) // 필요 시 더 상세하게 구성 가능
            .join("");

          const popup = new window.kakao.maps.CustomOverlay({
            position: new window.kakao.maps.LatLng(centerLat, centerLng),
            content: `<div class="property-popup"><ul>${listHtml}</ul></div>`,
            yAnchor: 1,
            map,
          });

          popupRef.current = popup;
        });

        overlaysRef.current.push(overlay);
      });
    };

    drawGridClusters();
    window.kakao.maps.event.addListener(map, "idle", drawGridClusters);

    return () => {
      polygonsRef.current.forEach(p => p.setMap(null));
      overlaysRef.current.forEach(o => o.setMap(null));
      if (popupRef.current) popupRef.current.setMap(null);
      polygonsRef.current = [];
      overlaysRef.current = [];
      window.kakao.maps.event.removeListener(map, "idle", drawGridClusters);
    };
  }, [map, selectedRoomType]);

  return null;
}

export default GridClustering;
