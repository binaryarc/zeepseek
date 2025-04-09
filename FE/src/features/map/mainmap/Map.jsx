import { useEffect, useState, useRef } from "react";
import { useDispatch } from "react-redux";
import "./Map.css";
import CurrentLocationLabel from "./currentlocation/CurrentLocationLabel";
import SaleCountMarkers from "./salecountmarkers/SaleCountMarkers";
import ReactDOM from "react-dom/client";
import DetailRegion from "../detailregion/DetailRegion";
import { Provider } from "react-redux";
import store from "../../../store/store";
import {
  fetchRoomListByBounds,
  setMapReady,
  setSelectedPropertyId,
  setSelectedRoomType,
} from "../../../store/slices/roomListSlice";
import { useSelector } from "react-redux";
import { useLocation } from "react-router-dom";
import { useLayoutEffect } from "react";
import { debounce } from "lodash"; // debounce 임포트
import centroidData from "../../../assets/data/seoul_dong_centroids_from_geojson.json";

const Map = () => {
  const [locationWarning, setLocationWarning] = useState(false);

  function getDongIdFromGeojsonDongName(admNm) {
    const clickedDongName = admNm.trim(); // 예: "가회동"

    const match = centroidData.find((item) => {
      const nameDong = item.name.trim().split(" ").pop(); // 예: "서울특별시 종로구 가회동" → "가회동"
      return nameDong === clickedDongName;
    });
    console.log(match.dongId);

    return match?.dongId ?? null;
  }

  const [map, setMap] = useState(null); // map 객체 저장용 상태
  // 각 폴리곤을 feature 고유 id를 키로 캐싱
  const polygonCacheRef = useRef({});
  const geoDataRef = useRef(null); // GeoJSON 데이터를 저장할 ref
  const markerRef = useRef(null);
  const overlayRef = useRef(null);
  const selectedPolygonRef = useRef(null);
  const selectedDongIdRef = useRef(null);
  const hoverMarkerRef = useRef(null);
  // dong이 선택된 상태를 나타내는 플래그 (dong 영역이 활성화되어 있으면 true)
  const isDongSelectedRef = useRef(false);

  const dispatch = useDispatch();
  const { currentGuName, currentDongName, selectedRoomType } = useSelector(
    (state) => state.roomList
  );
  const user = useSelector((state) => state.auth.user);
  const location = useLocation();
  // 컴포넌트 상단: 지도 준비 여부 플래그
  window.isMapReady = false;

  useLayoutEffect(() => {
    if (location.state?.roomType) {
      dispatch(setSelectedRoomType(location.state.roomType));
    }
    if (location.state?.selectedPropertyId) {
      console.log(location.state?.selectedPropertyId);
      dispatch(setSelectedPropertyId(location.state.selectedPropertyId));
    }
  }, []);

  // window 객체에 hover 마커 관련 함수 등록
  window.setHoverMarker = (lat, lng) => {
    console.log("마커실행", lat, lng);
    const map = window.map;
    if (!map) return;

    const position = new window.kakao.maps.LatLng(lat, lng);

    if (hoverMarkerRef.current) {
      hoverMarkerRef.current.setMap(null);
    }

    // 마커 div 생성
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

  // GeoJSON 데이터 로드
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
        // 기본 좌표: 서울 시청
        let centerLatLng = new window.kakao.maps.LatLng(37.5665, 126.978);
        let level = 5;

        // 찜 매물 등에서 이동 시: 초기 center 좌표 및 zoom level 설정
        if (location.state?.lat && location.state?.lng) {
          centerLatLng = new window.kakao.maps.LatLng(
            location.state.lat,
            location.state.lng
          );
          level = 4;
        }

        const options = {
          center: centerLatLng,
          level: level,
        };

        const mapInstance = new window.kakao.maps.Map(container, options);
        window.map = mapInstance; // 전역에 저장
        setMap(mapInstance); // 상태에 저장
        dispatch(setMapReady(true)); // 지도 준비 완료 처리

        window.kakao.maps.event.addListener(mapInstance, "zoom_changed", () => {
          if (overlayRef.current) {
            overlayRef.current.setMap(null);
            overlayRef.current = null;
          }
          if (markerRef.current) {
            markerRef.current.setMap(null);
            markerRef.current = null;
          }
          if (selectedPolygonRef.current) {
            selectedPolygonRef.current.setOptions({
              strokeOpacity: 0,
              fillOpacity: 0.02,
            });
            selectedPolygonRef.current = null;
          }
          selectedDongIdRef.current = null;
          isDongSelectedRef.current = false;
        });

        // idle 이벤트 시 실행될 업데이트 함수: debounce로 호출 빈도 제한, 폴리곤 재사용 적용
        const updateMapElements = () => {
          // 맵 최초 준비 체크
          if (!window.isMapReady) {
            console.log("✅ 지도 준비 완료!");
            window.isMapReady = true;
          }

          // 만약 dong 영역이 선택되어 있다면 idle에서 초기화하지 않음.
          if (!isDongSelectedRef.current) {
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
          }

          if (!geoDataRef.current) return;

          const bounds = mapInstance.getBounds();
          const currentLevel = mapInstance.getLevel();

          // 지도 레벨이 4 이상일 때 매물 리스트 업데이트
          if (currentLevel > 3) {
            if (currentGuName && currentDongName && selectedRoomType) {
              if (currentLevel >= 6) {
                dispatch(
                  fetchRoomListByBounds({
                    guName: currentGuName,
                    dongName: "",
                    filter: selectedRoomType,
                    userId: user?.idx ?? null,
                  })
                );
              } else {
                dispatch(
                  fetchRoomListByBounds({
                    guName: currentGuName,
                    dongName: currentDongName,
                    filter: selectedRoomType,
                    userId: user?.idx ?? null,
                  })
                );
              }
            }
          }

          // 지도 레벨이 너무 높거나 낮으면, 모든 폴리곤 숨김 처리
          if (currentLevel > 6 || currentLevel <= 3) {
            Object.values(polygonCacheRef.current).forEach((polygon) => {
              polygon.setMap(null);
            });
            return;
          }

          // ✅ 서울 외 지역 경고 체크 (📌 여기에 추가)
          const center = mapInstance.getCenter();
          const geocoder = new window.kakao.maps.services.Geocoder();
          geocoder.coord2RegionCode(center.getLng(), center.getLat(), (result, status) => {
            if (status === window.kakao.maps.services.Status.OK) {
              const city = result[0].region_1depth_name;
              setLocationWarning(city !== "서울특별시");
            }
          });

          // GeoJSON의 각 feature에 대해 폴리곤 캐싱/재사용 처리
          geoDataRef.current.features.forEach((feature) => {
            const featureId = feature.properties.ADM_CD;
            const coords = feature.geometry.coordinates;
            const [ring] = coords;

            // 대략적인 중심 좌표 계산
            const centerX = ring.reduce((sum, [x]) => sum + x, 0) / ring.length;
            const centerY =
              ring.reduce((sum, [, y]) => sum + y, 0) / ring.length;
            const center = new window.kakao.maps.LatLng(centerY, centerX);

            if (bounds.contain(center)) {
              // 캐시에 해당 폴리곤이 있으면 지도에 표시, 없으면 새로 생성 후 캐싱
              if (polygonCacheRef.current[featureId]) {
                polygonCacheRef.current[featureId].setMap(mapInstance);
              } else {
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

                // 이벤트 리스너는 최초 생성 시 한 번 등록
                window.kakao.maps.event.addListener(polygon, "click", () => {
                  const clickedDongId = feature.properties.ADM_CD;
                  const dongNameFromGeojson = feature.properties.ADM_NM;
                  const dongId =
                    getDongIdFromGeojsonDongName(dongNameFromGeojson);
                  // 이미 선택된 동이면 → 토글 방식으로 선택 해제
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
                    isDongSelectedRef.current = false; // 선택 해제
                    return;
                  }

                  // 새로운 동 클릭 시: 기존 선택 상태를 갱신하고 플래그를 true로 설정
                  selectedDongIdRef.current = clickedDongId;
                  isDongSelectedRef.current = true;
                  mapInstance.setCenter(center);

                  // 기존 마커 제거
                  if (markerRef.current) markerRef.current.setMap(null);

                  const imageSrc = "/images/zeep.png"; // public 기준 경로
                  const imageSize = new window.kakao.maps.Size(60, 60); // 마커 이미지 크기
                  const imageOption = {
                    offset: new window.kakao.maps.Point(center),
                  };

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

                  const root = ReactDOM.createRoot(content);
                  if (dongId) {
                    console.log(dongId);
                    root.render(
                      <Provider store={store}>
                        <DetailRegion dongId={dongId} />
                      </Provider>
                    );
                  } else {
                    alert("동 ID를 찾을 수 없습니다.");
                  }

                  const overlay = new window.kakao.maps.CustomOverlay({
                    position: center,
                    content,
                    yAnchor: 2,
                    zIndex: 2000,
                  });

                  overlay.setMap(mapInstance);
                  overlayRef.current = overlay;

                  // 기존 선택된 폴리곤 스타일 초기화
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

                window.kakao.maps.event.addListener(
                  polygon,
                  "mouseover",
                  () => {
                    if (selectedPolygonRef.current === polygon) return;
                    polygon.setOptions({
                      strokeOpacity: 1,
                      fillOpacity: 0.5,
                      fillColor: "#F1FAD3",
                    });
                  }
                );

                window.kakao.maps.event.addListener(polygon, "mouseout", () => {
                  if (selectedPolygonRef.current === polygon) return;
                  polygon.setOptions({
                    strokeOpacity: 0,
                    fillOpacity: 0.02,
                  });
                });

                polygon.setMap(mapInstance);
                polygonCacheRef.current[featureId] = polygon;
              }
            } else {
              // 보이지 않는 영역에 있는 폴리곤은 숨김 처리
              if (polygonCacheRef.current[featureId]) {
                polygonCacheRef.current[featureId].setMap(null);
              }
            }
          });
        };

        // debounce를 적용하여 idle 이벤트 핸들러 호출 빈도 제한 (300ms delay)
        const debouncedUpdateMapElements = debounce(updateMapElements, 300);
        window.kakao.maps.event.addListener(
          mapInstance,
          "idle",
          debouncedUpdateMapElements
        );
      });
    };
  }, []);

  // 지도 로딩 후 location.state에 있는 좌표로 이동 처리
  useEffect(() => {
    if (!map || !location.state) return;

    const { lat, lng } = location.state;
    if (lat && lng) {
      const moveLatLng = new window.kakao.maps.LatLng(lat, lng);
      map.setLevel(5);
      map.setCenter(moveLatLng);
    }
  }, [map, location.state]);

  return (
    <div className="map-container" style={{ position: "relative" }}>
      {/* ✅ 경고 박스 추가 */}
      {locationWarning && (
        <div className="location-warning-banner">
          ⚠️ 서비스 이용 불가능 지역입니다.
        </div>
      )}
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
