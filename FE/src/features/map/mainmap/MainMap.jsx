// 📁 src/components/MapComponent.jsx
import { useEffect } from "react";

const MainMap = () => {
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

        const map = new window.kakao.maps.Map(container, options);

        // 예시 마커 추가
        new window.kakao.maps.Marker({
          position: options.center,
          map: map,
        });
      });
    };
  }, []);

  return (
    <div>
      <h2>카카오 지도</h2>
      <div id="map" style={{ width: "100%", height: "1800px" }} />
    </div>
  );
};

export default MainMap;
