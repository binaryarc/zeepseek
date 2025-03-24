import React, { useEffect, useState } from "react";
import "./CurrentLocationLabel.css";

function CurrentLocationLabel({ map }) {
  const [locationName, setLocationName] = useState("");

  useEffect(() => {
    if (!map || !window.kakao) return;

    const geocoder = new window.kakao.maps.services.Geocoder();

    const updateCenterAddress = () => {
      const center = map.getCenter();
      geocoder.coord2RegionCode(center.getLng(), center.getLat(), (result, status) => {
        if (status === window.kakao.maps.services.Status.OK) {
          // 법정동 중 '동' 또는 '구' 정보 추출
          const region = result.find(r => r.region_type === 'H' || r.region_type === 'B');
          if (region) {
            setLocationName(region.region_3depth_name || region.region_2depth_name);
          }
        }
      });
    };

    updateCenterAddress();

    // 지도 이동 시 주소 갱신
    window.kakao.maps.event.addListener(map, "idle", updateCenterAddress);

    return () => {
      window.kakao.maps.event.removeListener(map, "idle", updateCenterAddress);
    };
  }, [map]);

  if (!locationName) return null;

  return (
    <div className="location-label">
      {locationName}
    </div>
  );
}

export default CurrentLocationLabel;
