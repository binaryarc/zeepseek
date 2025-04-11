import { useEffect, useRef } from "react";
import { fetchGridSaleCountsByType } from "../../../../../common/api/api";
import "./GridClustering.css";
import { useSelector, useDispatch } from "react-redux";
import {
  setRoomsFromGridResult,
  setGridRoomList,
} from "../../../../../store/slices/roomListSlice";

// 절대 좌표 기준의 그리드 셀 생성 함수 (위도/경도 간격)
function generateFixedGridCells(
  bounds,
  cellSizeLat = 0.01,
  cellSizeLng = 0.01
) {
  const sw = bounds.getSouthWest();
  const ne = bounds.getNorthEast();

  const startLat = Math.floor(sw.getLat() / cellSizeLat) * cellSizeLat;
  const startLng = Math.floor(sw.getLng() / cellSizeLng) * cellSizeLng;
  const endLat = Math.ceil(ne.getLat() / cellSizeLat) * cellSizeLat;
  const endLng = Math.ceil(ne.getLng() / cellSizeLng) * cellSizeLng;

  const cells = [];
  for (let lat = startLat; lat < endLat; lat += cellSizeLat) {
    for (let lng = startLng; lng < endLng; lng += cellSizeLng) {
      cells.push({
        minLat: lat,
        maxLat: lat + cellSizeLat,
        minLng: lng,
        maxLng: lng + cellSizeLng,
      });
    }
  }
  return cells;
}

// 줌 레벨에 따른 셀 크기 반환 함수
function getCellSizeByZoom(zoomLevel) {
  // 줌 레벨 3, 2, 1에 따라 다른 셀 크기를 반환합니다.
  if (zoomLevel === 3) {
    return { cellSizeLat: 0.002, cellSizeLng: 0.002 };
  } else if (zoomLevel === 2) {
    return { cellSizeLat: 0.001, cellSizeLng: 0.001 };
  } else if (zoomLevel === 1) {
    return { cellSizeLat: 0.0005, cellSizeLng: 0.0005 };
  } else {
    // 허용 범위 외의 줌 레벨일 때는 0을 반환하여 그리드를 표시하지 않음
    return { cellSizeLat: 0, cellSizeLng: 0 };
  }
}

function GridClustering({ map }) {
  // 캐싱 객체: 각 그리드 클러스터(폴리곤과 오버레이)를 고유 키로 저장
  const gridClusterCacheRef = useRef({});
  const dispatch = useDispatch();
  const popupRef = useRef(null);
  const selectedRoomType = useSelector(
    (state) => state.roomList.selectedRoomType
  );

  const roomTypeMap = {
    "원룸/투룸": "one-room",
    "주택/빌라": "house",
    오피스텔: "office",
  };
  const filterKey = roomTypeMap[selectedRoomType];

  useEffect(() => {
    if (!map || !window.kakao) return;

    const clearGridClusters = () => {
      Object.keys(gridClusterCacheRef.current).forEach((cellKey) => {
        const { polygon, overlay } = gridClusterCacheRef.current[cellKey];
        polygon.setMap(null);
        overlay.setMap(null);
      });
    };

    // 줌 변경 이벤트: 줌이 바뀌면 기존 그리드 클러스터를 바로 숨깁니다.
    const zoomListener = window.kakao.maps.event.addListener(
      map,
      "zoom_changed",
      () => {
        clearGridClusters();
      }
    );

    const drawGridClusters = async () => {
      const level = map.getLevel();
      // 줌 레벨이 1~3 범위가 아니라면 기존 그리드 클러스터 제거 후 리턴
      if (level > 3 || level < 1) {
        clearGridClusters();
        return;
      }
      // AI 추천 탭일 경우 표시하지 않음
      if (selectedRoomType === "AI 추천") {
        clearGridClusters();
        return;
      }

      const bounds = map.getBounds();

      // 현재 줌 레벨에 따른 셀 크기 계산
      const { cellSizeLat, cellSizeLng } = getCellSizeByZoom(level);
      if (!cellSizeLat || !cellSizeLng) {
        clearGridClusters();
        return;
      }

      // 절대 좌표 기준의 고정된 셀 생성
      const cells = generateFixedGridCells(bounds, cellSizeLat, cellSizeLng);

      // 각 셀에 대한 매물 집계 API 호출
      const result = await fetchGridSaleCountsByType(cells, filterKey);
      dispatch(setRoomsFromGridResult(result));

      // 업데이트된 셀 키를 추적 (현재 화면에 보이는 셀)
      const updatedKeys = new Set();

      result.forEach((item) => {
        const { cell, properties } = item;
        const { minLat, maxLat, minLng, maxLng } = cell;
        if (!properties || properties.length === 0) return;

        // 셀의 고유 키 생성 (절대 좌표 기준)
        const cellKey = `${minLat}-${minLng}-${maxLat}-${maxLng}`;
        updatedKeys.add(cellKey);

        // 셀의 중앙 좌표 계산
        const centerLat = (minLat + maxLat) / 2;
        const centerLng = (minLng + maxLng) / 2;
        const centerLatLng = new window.kakao.maps.LatLng(centerLat, centerLng);

        // 사각형 경계(폴리곤) 경로 생성
        const rectPath = [
          new window.kakao.maps.LatLng(minLat, minLng),
          new window.kakao.maps.LatLng(minLat, maxLng),
          new window.kakao.maps.LatLng(maxLat, maxLng),
          new window.kakao.maps.LatLng(maxLat, minLng),
        ];

        let gridCluster = gridClusterCacheRef.current[cellKey];
        if (gridCluster) {
          // 이미 생성된 클러스터가 있다면, 매물 수 업데이트 등 필요한 내용 갱신
          const div = gridCluster.overlay.getContent();
          div.querySelector(".grid-count").innerText = properties.length;
          gridCluster.polygon.setMap(map);
          gridCluster.overlay.setMap(map);
        } else {
          // 클러스터가 없다면 새롭게 생성
          const polygon = new window.kakao.maps.Polygon({
            path: [rectPath],
            strokeWeight: 1,
            strokeColor: "#fb8c00",
            strokeOpacity: 0.8,
            fillColor: "rgba(255,167,38,0.3)",
            fillOpacity: 0.5,
          });
          polygon.setMap(map);

          const div = document.createElement("div");
          div.innerHTML = `
            <div class="grid-count-wrapper">
              <div class="grid-count">${properties.length}</div>
            </div>
          `;
          div.onclick = (e) => {
            e.preventDefault();
            e.stopPropagation();
            dispatch(setGridRoomList(properties));
          };

          const overlay = new window.kakao.maps.CustomOverlay({
            position: centerLatLng,
            content: div,
            xAnchor: 0.5,
            yAnchor: 0.5,
            map,
          });
          window.kakao.maps.event.addListener(overlay, "click", () => {
            if (popupRef.current) popupRef.current.setMap(null);
            dispatch(setGridRoomList(properties));
          });

          gridCluster = { polygon, overlay };
          gridClusterCacheRef.current[cellKey] = gridCluster;
        }
      });

      // 캐시에 남아있는, 현재 화면에 보이지 않는 클러스터는 숨깁니다.
      Object.keys(gridClusterCacheRef.current).forEach((cellKey) => {
        if (!updatedKeys.has(cellKey)) {
          const { polygon, overlay } = gridClusterCacheRef.current[cellKey];
          polygon.setMap(null);
          overlay.setMap(null);
        }
      });
    };

    drawGridClusters();
    window.kakao.maps.event.addListener(map, "idle", drawGridClusters);

    return () => {
      // 제거 시 모든 리스너와 그리드 클러스터 제거
      Object.values(gridClusterCacheRef.current).forEach(
        ({ polygon, overlay }) => {
          polygon.setMap(null);
          overlay.setMap(null);
        }
      );
      gridClusterCacheRef.current = {};
      if (popupRef.current) popupRef.current.setMap(null);
      window.kakao.maps.event.removeListener(map, "idle", drawGridClusters);
      window.kakao.maps.event.removeListener(map, "zoom_changed", zoomListener);
    };
  }, [map, selectedRoomType]);

  return null;
}

export default GridClustering;
