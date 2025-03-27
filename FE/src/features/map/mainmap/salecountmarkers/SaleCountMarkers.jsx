import { useEffect, useRef } from "react";
import guData from "../../../../assets/data/seoul_gu_centroids_from_geojson.json";
import dongData from "../../../../assets/data/seoul_dong_centroids_from_geojson.json";
import "./SaleCountMarkers.css";
import {
  fetchDongPropertyCounts,
  fetchGuPropertyCounts,
} from "../../../../common/api/api";
// import { useState } from "react";
import { useSelector } from "react-redux";

function SaleCountMarkers({ map }) {
  const overlaysRef = useRef([]); // ✅ useRef로 오버레이 관리
  const selectedRoomType = useSelector(
    (state) => state.roomList.selectedRoomType
  );
  const roomTypeMap = {
    "원룸/투룸": "one-room",
    "주택/빌라": "house",
    오피스텔: "office",
  };
  const filterKey = roomTypeMap[selectedRoomType];

  // const [dongId, setDongId] = useState(null);

  useEffect(() => {
    if (!map || !window.kakao) return;

    const drawMarkers = async () => {
      const level = map.getLevel();
      const isGuLevel = level >= 6;

      // ✅ 기존 오버레이 모두 제거
      overlaysRef.current.forEach((o) => o.setMap(null));
      overlaysRef.current = [];

      const targetData = isGuLevel ? guData : dongData;

      let countData = [];

      if (isGuLevel) {
        countData = await fetchGuPropertyCounts();
      } else {
        if (!filterKey) return;
        console.log("dhsl?");
        countData = await fetchDongPropertyCounts(filterKey); // ✅ 파라미터 전달
      }

      // 📌 데이터 매핑
      const countMap = {};
      countData.forEach((item) => {
        // console.log('11',countData)
        if (isGuLevel) {
          countMap[item.guName] = item.propertyCount;
        } else {
          // console.log(item.dongId)
          countMap[String(item.dongId)] = item.propertyCount;
        }
      });

      targetData.forEach((region) => {
        // console.log(targetData)
        const position = new window.kakao.maps.LatLng(region.lat, region.lng);

        // ✅ '공덕동', '마포구' 등 마지막 단어만 추출
        const splitName = region.name.trim().split(" ");
        const displayName = splitName[splitName.length - 1];

        let count = 0;
        if (isGuLevel) {
          count = countMap[region.name] || 0;
        } else {
          // console.log(region)
          const dongId = region.dongId;
          count = countMap[dongId] || 0;
          // console.log("동별 매물 개수:", dongId, count);
        }

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
  }, [map, filterKey]);

  return null;
}

export default SaleCountMarkers;
