import { useEffect, useState } from "react";
import "./Map.css";
import CurrentLocationLabel from "./currentlocation/CurrentLocationLabel";
import SaleCountMarkers from "./salecountmarkers/SaleCountMarkers";

const Map = () => {
  const [map, setMap] = useState(null); // 👈 map 객체 저장용 상태

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
        setMap(mapInstance); // 👈 상태에 저장

        // 마커는 예시
        new window.kakao.maps.Marker({
          position: options.center,
          map: mapInstance,
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
