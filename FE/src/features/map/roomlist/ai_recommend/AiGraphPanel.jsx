import React from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Legend,
  CartesianGrid,
} from "recharts";
import "./AiRecommend.css";

const AiGraphPanel = ({ room, values }) => {
  const categories = [
    { key: "cafeScore", label: "카페" },
    { key: "chickenScore", label: "치킨집" },
    { key: "convenienceScore", label: "편의" },
    { key: "healthScore", label: "의료" },
    { key: "leisureScore", label: "여가" },
    { key: "transportScore", label: "대중교통" },
    { key: "restaurantScore", label: "식당" },
  ];

  const chartData = categories.map(({ key, label }) => ({
    name: label,
    사용자점수: values[label] || 0,
    매물점수: (room[key] || 0), // AI 점수는 소수 → 100배
  }));

  return (
    <div className="graph-container">
      <h4 className="result-title">AI 추천 점수 비교</h4>
      <ResponsiveContainer width="100%" height={400}>
        <BarChart
          layout="vertical"
          data={chartData}
          margin={{ top: 20, right: 50, left: 50, bottom: 20 }}
          barCategoryGap={30}
        >
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis type="number" domain={[0, 100]} hide />
          <YAxis
            type="category"
            dataKey="name"
            axisLine={false}
            tickLine={false}
            tick={{ fontWeight: "bold", fontSize: 14 }}
          />
          <Tooltip formatter={(value, name) => [`${value.toFixed(2)}점`, name]} />
          <Legend verticalAlign="bottom" height={36} wrapperStyle={{ paddingTop: 20 }} />
          <Bar
            dataKey="사용자점수"
            fill="#8884d8"
            barSize={12}
            radius={[0, 10, 10, 0]}
          />
          <Bar
            dataKey="매물점수"
            fill="#82ca9d"
            barSize={12}
            radius={[0, 10, 10, 0]}
          />
        </BarChart>
      </ResponsiveContainer>
      {room.similarity !== undefined && (
        <p className="similarity-text">
          <strong>내 취향과의 유사도:</strong> {(room.similarity * 100).toFixed(2)}%
        </p>
      )}
    </div>
  );
};

export default AiGraphPanel;
