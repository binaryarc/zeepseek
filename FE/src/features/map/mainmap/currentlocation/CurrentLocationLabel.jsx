import React, { useEffect, useState } from "react";
import "./CurrentLocationLabel.css";

function CurrentLocationLabel({ map }) {
  const [locationName, setLocationName] = useState("");

  useEffect(() => {
    if (!map || !window.kakao) return;

    const geocoder = new window.kakao.maps.services.Geocoder();

    const updateCenterAddress = () => {
      const center = map.getCenter();
      const level = map.getLevel();

      geocoder.coord2RegionCode(center.getLng(), center.getLat(), (result, status) => {
        if (status === window.kakao.maps.services.Status.OK) {
          const regionData = result[0];

          if (level >= 6) {
            setLocationName(regionData.region_2depth_name); // 구
          } else {
            setLocationName(regionData.region_3depth_name); // 동
          }
        }
      });
    };

    updateCenterAddress();

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
