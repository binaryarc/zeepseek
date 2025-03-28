import Navbar from "../../common/navbar/Navbar"
import { NavLink, Outlet } from "react-router-dom";
import "./ComparePage.css"


function ComparePage() {
  return (
    <div className="compare-container">
      <Navbar />
      <div className="compare-tab-header">
        <NavLink to="region" className={({ isActive }) => isActive ? "active-tab" : ""}>
          동네 비교 🏘
        </NavLink>
        <NavLink to="estate" className={({ isActive }) => isActive ? "active-tab" : ""}>
          매물 비교 🏡
        </NavLink>
      </div>
      <Outlet />
    </div>
  )

}

export default ComparePage;