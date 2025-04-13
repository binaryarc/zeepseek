import React, { useState } from "react";
import "./ComparePage.css";
import EstateCompare from "./estate/EstateCompare";
import RegionCompare from "./region/RegionCompare";
import Navbar from "../../common/navbar/Navbar"

const ComparePage = () => {
  const [activeTab, setActiveTab] = useState("region"); // region 또는 estate

  return (
    <div className="compare-page-container">
      <Navbar />
      <div className="compare-page">
        <div className="compare-section">
          {/* 탭: 컨테이너 바로 위에 붙음 */}
          <div className="compare-tabs">
            <button
              className={activeTab === "region" ? "active" : ""}
              onClick={() => setActiveTab("region")}
            >
              동네 비교 🏘️
            </button>
            <button
              className={activeTab === "estate" ? "active" : ""}
              onClick={() => setActiveTab("estate")}
            >
              매물 비교 🏠
            </button>
          </div>

          {/* 여기서는 어떤 컴포넌트를 보여줄지만 결정 */}
          {activeTab === "region" ? <RegionCompare /> : <EstateCompare />}
        </div>
      </div>
    </div>
  );
};

export default ComparePage;
