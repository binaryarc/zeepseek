import React, { useState } from "react";
import "./MainSearchBar.css";
import { FiSearch } from "react-icons/fi"; // 검색 아이콘
import { useNavigate } from "react-router-dom";
import { useDispatch } from "react-redux";
import {
    setCurrentDongId,
    setSearchLock,
    fetchRoomList,
  } from "../../store/slices/roomListSlice";
import { searchProperties } from "../api/api";

function MainSearchbar() {
  const [searchText, setSearchText] = useState("");
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleSearch = async () => {
        if (!searchText.trim()) return;
            navigate('/map');
        try {
        const res = await searchProperties(searchText);
        console.log("검색결과", res);
        const properties = res?.properties || [];

        if (properties.length > 0) {
            const first = properties[0];
            const geocoder = new window.kakao.maps.services.Geocoder();

            // 주소를 좌표로 변환하여 지도 이동
            geocoder.addressSearch(first.address, (result, status) => {
            if (status === window.kakao.maps.services.Status.OK) {
                const { x, y } = result[0];
                const latLng = new window.kakao.maps.LatLng(y, x);
                const map = window.map;
                // 검색 결과로 지도 이동
                if (map) {
                // ✅ 이동 전 플래그 켜기
                window.isMovingBySearch = true;

                map.setCenter(latLng);

                // ✅ 다음 idle 발생 전에 false로 꺼줌 (약간의 delay로)
                setTimeout(() => {
                    window.isMovingBySearch = false;
                }, 500);
                }
                // 💡 강제로 idle 이벤트 트리거
                // setTimeout(() => {
                //   window.kakao.maps.event.trigger(map, "idle");
                // }, 50); // 500ms 정도면 충분
                // map.setCenter(latLng);
            }
            });

            // 현재 동 코드를 제거하여 다음 지도 idle 시에 다시 요청될 수 있게 함
            dispatch(setCurrentDongId(null));

            // 검색 결과를 rooms에 반영 (덮어쓰기)
            dispatch(fetchRoomList(searchText));

            dispatch(setSearchLock(true)); // 🔐 검색으로 인해 이동 발생
        } else {
            alert("검색 결과가 없습니다.");
        }
        } catch (err) {
        console.error("검색 실패:", err);
        }
    };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") handleSearch();
  };

  return (
    <div className="main-searchbox">
  <input
    type="text"
    placeholder="지역, 매물번호를 검색하세요!"
    value={searchText}
    onChange={(e) => setSearchText(e.target.value)}
    onKeyDown={handleKeyDown}
  />
  <FiSearch className="search-icon" onClick={handleSearch} />
</div>
  );
}

export default MainSearchbar;