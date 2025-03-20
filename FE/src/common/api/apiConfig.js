const API_BASE_URL = import.meta.env.VITE_API_BASE_URL; 

export const apiConfig = {
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
    Accept: "application/json",
  },
};
