import { useEffect, useState, useRef } from "react";
import { useDispatch } from "react-redux";
import "./Map.css";
import CurrentLocationLabel from "./currentlocation/CurrentLocationLabel";
import SaleCountMarkers from "./salecountmarkers/SaleCountMarkers";
import ReactDOM from "react-dom/client";
import DetailRegion from "../detailregion/DetailRegion";
import { Provider } from "react-redux";
import store from "../../../store/store";
import { fetchRoomListByBounds } from "../../../store/slices/roomListSlice";
import { useSelector } from "react-redux";

const Map = () => {
  const [map, setMap] = useState(null); // 👈 map 객체 저장용 상태
  const polygonsRef = useRef([]); // 폴리곤 저장용 ref
  const geoDataRef = useRef(null); // GeoJSON 데이터를 저장할 ref
  const markerRef = useRef(null);
  const overlayRef = useRef(null);
  const selectedPolygonRef = useRef(null);
  const selectedDongIdRef = useRef(null);
  const hoverMarkerRef = useRef(null);
  const dispatch = useDispatch();
  const { currentGuName, currentDongName, selectedRoomType } = useSelector(
    (state) => state.roomList
  );

  // 아래 window 객체에 등록
  window.setHoverMarker = (lat, lng) => {
    console.log("마커실행", lat, lng);
    const map = window.map;
    if (!map) return;

    const position = new window.kakao.maps.LatLng(lat, lng);

    if (hoverMarkerRef.current) {
      hoverMarkerRef.current.setMap(null);
    }

    // 마커 div 만들기
    const markerEl = document.createElement("div");
    markerEl.className = "hover-marker";

    const overlay = new window.kakao.maps.CustomOverlay({
      position,
      content: markerEl,
      xAnchor: 0.5,
      yAnchor: 1,
      zIndex: 999,
      map,
    });

    hoverMarkerRef.current = overlay;
  };

  window.clearHoverMarker = () => {
    if (hoverMarkerRef.current) {
      hoverMarkerRef.current.setMap(null);
      hoverMarkerRef.current = null;
    }
  };

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

    document.head.appendChild(kakaoMapScript);

    kakaoMapScript.onload = () => {
      window.kakao.maps.load(() => {
        const container = document.getElementById("map");
        const options = {
          center: new window.kakao.maps.LatLng(37.5665, 126.978),
          level: 3,
        };

        const mapInstance = new window.kakao.maps.Map(container, options);
        window.map = mapInstance; // 👈 전역에 저장
        setMap(mapInstance); // 👈 상태에 저장

        // 🌐 지도가 이동할 때마다 보이는 동들만 폴리곤으로 그리기
        window.kakao.maps.event.addListener(mapInstance, "idle", () => {
          if (markerRef.current) {
            markerRef.current.setMap(null);
            markerRef.current = null;
          }
          if (overlayRef.current) {
            overlayRef.current.setMap(null);
            overlayRef.current = null;
          }

          if (selectedPolygonRef.current) {
            selectedPolygonRef.current.setOptions({
              strokeOpacity: 0,
              fillOpacity: 0.02,
            });
            selectedPolygonRef.current = null;
          }

          if (!geoDataRef.current) return;

          const bounds = mapInstance.getBounds();
          const level = mapInstance.getLevel();

          // ✅ 지도 레벨이 4 이상으로 올라갔을 때 매물 리스트 다시 불러오기
          if (level > 3) {
            if (currentGuName && currentDongName && selectedRoomType) {
              if (level >= 6) {
                dispatch(
                  fetchRoomListByBounds({
                    guName: currentGuName,
                    dongName: "",
                    filter: selectedRoomType,
                  })
                );
              } else {
                dispatch(
                  fetchRoomListByBounds({
                    guName: currentGuName,
                    dongName: currentDongName,
                    filter: selectedRoomType,
                  })
                );
              }
            }
          }

          if (level > 6 || level <= 3) {
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
                strokeWeight: 0.8,
                strokeColor: "#3CB371",
                strokeOpacity: 0,
                fillColor: "#A2D1FF",
                fillOpacity: 0.02,
              });

              // polygon.setMap(map);
              // polygonsRef.current.push(polygon);

              window.kakao.maps.event.addListener(polygon, "click", () => {
                const clickedDongId = feature.properties.ADM_CD;
                // ✅ 이미 선택된 동이면 → 오버레이 제거 (토글 방식)
                if (selectedDongIdRef.current === clickedDongId) {
                  if (overlayRef.current) overlayRef.current.setMap(null);
                  overlayRef.current = null;
                  if (markerRef.current) markerRef.current.setMap(null);
                  markerRef.current = null;
                  if (selectedPolygonRef.current) {
                    selectedPolygonRef.current.setOptions({
                      strokeOpacity: 0,
                      fillOpacity: 0.02,
                    });
                    selectedPolygonRef.current = null;
                  }
                  selectedDongIdRef.current = null;
                  return;
                }

                // ✅ 새로운 동 클릭 시 → 기존 오버레이/마커 제거 후 새로 생성
                selectedDongIdRef.current = clickedDongId;
                mapInstance.setCenter(center);

                // 기존 마커 제거
                if (markerRef.current) markerRef.current.setMap(null);
                // ✅ zeep.png 커스텀 마커 설정
                
                const imageSrc = "/images/zeep.png"; // public 기준 경로
                const imageSize = new window.kakao.maps.Size(80, 80); // 마커 이미지 크기
                const imageOption = { offset: new window.kakao.maps.Point(0,0) }; // 마커 기준점

                const markerImage = new window.kakao.maps.MarkerImage(
                  imageSrc,
                  imageSize,
                  imageOption
                );
                                
                const marker = new window.kakao.maps.Marker({
                  position: center,
                  image: markerImage,
                  map: mapInstance,
                  zIndex: 2000,
                });
                markerRef.current = marker;

                // 기존 오버레이 제거
                if (overlayRef.current) overlayRef.current.setMap(null);

                const content = document.createElement("div");
                content.className = "detail-overlay";

                // DetailRegion 컴포넌트 렌더링
                const root = ReactDOM.createRoot(content);
                root.render(
                  <Provider store={store}>
                    <DetailRegion dongId={feature.properties.ADM_CD} />
                  </Provider>
                );

                const overlay = new window.kakao.maps.CustomOverlay({
                  position: center,
                  content,
                  yAnchor: 2,
                  zIndex: 2000,
                });

                overlay.setMap(mapInstance);
                overlayRef.current = overlay;

                // ✅ 선택된 폴리곤 스타일 유지
                if (selectedPolygonRef.current) {
                  selectedPolygonRef.current.setOptions({
                    strokeOpacity: 0,
                    fillOpacity: 0.02,
                  });
                }

                polygon.setOptions({
                  strokeOpacity: 1,
                  fillOpacity: 0.5,
                  fillColor: "#F1FAD3",
                });

                selectedPolygonRef.current = polygon;
              });
              // 나중을 위한 TODO: 해당 동의 매물 리스트 Redux 또는 상위 state에 업데이트
              // e.g. dispatch(setCurrentDong(feature.properties.ADM_CD))

              // ✅ 마우스 올릴 때 경계선 표시
              window.kakao.maps.event.addListener(polygon, "mouseover", () => {
                if (selectedPolygonRef.current === polygon) return; // 선택된 폴리곤이면 무시
                polygon.setOptions({
                  strokeOpacity: 1,
                  fillOpacity: 0.5,
                  fillColor: "#F1FAD3",
                });
              });

              // ✅ 마우스 나갈 때 원래대로
              window.kakao.maps.event.addListener(polygon, "mouseout", () => {
                if (selectedPolygonRef.current === polygon) return; // 선택된 폴리곤이면 무시
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
          <SaleCountMarkers map={map} />
        </>
      )}
    </div>
  );
};

export default Map;
