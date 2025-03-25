import { useEffect, useState, useRef } from "react";
import "./Map.css";
import CurrentLocationLabel from "./currentlocation/CurrentLocationLabel";
import SaleCountMarkers from "./salecountmarkers/SaleCountMarkers";

const Map = () => {
  const [map, setMap] = useState(null); // 👈 map 객체 저장용 상태
  const polygonsRef = useRef([]); // 폴리곤 저장용 ref
  const geoDataRef = useRef(null); // GeoJSON 데이터를 저장할 ref

  useEffect(() => {
    const loadGeoJSON = async () => {
      const res = await fetch("/data/seoul_boundary_wgs84.geojson");
      const data = await res.json();
      geoDataRef.current = data;
    };
    loadGeoJSON();
  }, []);

  useEffect(() => {
    const kakaoMapScript = document.createElement("script");
    kakaoMapScript.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${
      import.meta.env.VITE_APP_KAKAO_MAP_API_KEY
    }&libraries=services&autoload=false`;
    kakaoMapScript.async = true;

    console.log("카카오 API 스크립트 추가 시도:", kakaoMapScript.src);
    document.head.appendChild(kakaoMapScript);

    console.log("카카오 API 키:", import.meta.env.VITE_APP_KAKAO_MAP_API_KEY);

    kakaoMapScript.onload = () => {
      console.log("카카오 SDK 로드됨!");
      window.kakao.maps.load(() => {
        const container = document.getElementById("map");
        const options = {
          center: new window.kakao.maps.LatLng(37.5665, 126.978),
          level: 3,
        };

        const mapInstance = new window.kakao.maps.Map(container, options);
        window.map = mapInstance; // 👈 전역에 저장
        setMap(mapInstance); // 👈 상태에 저장

        // 마커는 예시
        new window.kakao.maps.Marker({
          position: options.center,
          map: mapInstance,
        });

        // 🌐 지도가 이동할 때마다 보이는 동들만 폴리곤으로 그리기
        window.kakao.maps.event.addListener(mapInstance, "idle", () => {
          if (!geoDataRef.current) return;

          const bounds = mapInstance.getBounds();
          const level = mapInstance.getLevel();

          if (level > 5) {
            polygonsRef.current.forEach((polygon) => polygon.setMap(null));
            polygonsRef.current = [];
            return;
          }

          geoDataRef.current.features.forEach((feature) => {
            // const name = feature.properties.ADM_NM;
            const coords = feature.geometry.coordinates;
            const [ring] = coords;

            // 대략적인 중심좌표 계산
            const centerX = ring.reduce((sum, [x]) => sum + x, 0) / ring.length;
            const centerY =
              ring.reduce((sum, [, y]) => sum + y, 0) / ring.length;

            const center = new window.kakao.maps.LatLng(centerY, centerX);

            // 현재 bounds 내에 있으면 그리기
            if (bounds.contain(center)) {
              const path = ring.map(
                ([x, y]) => new window.kakao.maps.LatLng(y, x)
              );

              const polygon = new window.kakao.maps.Polygon({
                path,
                strokeWeight: 1,
                strokeColor: "#004c80",
                strokeOpacity: 0,
                fillColor: "#A2D1FF",
                fillOpacity: 0.02,
              });

              polygon.setMap(map);

              polygonsRef.current.push(polygon);

              // ✅ 마우스 올릴 때 경계선 표시
              window.kakao.maps.event.addListener(polygon, "mouseover", () => {
                polygon.setOptions({
                  strokeOpacity: 1,
                  fillOpacity: 0.6,
                  fillColor: "#F1FAD3",
                });
              });

              // ✅ 마우스 나갈 때 경계선 숨김
              window.kakao.maps.event.addListener(polygon, "mouseout", () => {
                polygon.setOptions({
                  strokeOpacity: 0,
                  fillOpacity: 0.02,
                });
              });

              polygon.setMap(mapInstance);
              polygonsRef.current.push(polygon);
            }
          });
        });
      });
    };
  }, []);

  return (
    <div className="map-container" style={{ position: "relative" }}>
      <div id="map" className="map-box" />
      {map && (
        <>
          <CurrentLocationLabel map={map} />
          <SaleCountMarkers map={map} /> {/* 👈 여기 추가 */}
        </>
      )}
    </div>
  );
};

export default Map;
