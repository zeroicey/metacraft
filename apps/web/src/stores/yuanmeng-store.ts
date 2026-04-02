import { create } from "zustand";

export type YuanMengConnectionStatus = "connecting" | "connected" | "disconnected" | "error";

interface YuanMengStore {
  connectionStatus: YuanMengConnectionStatus;
  setConnectionStatus: (status: YuanMengConnectionStatus) => void;
}

export const useYuanMengStore = create<YuanMengStore>((set) => ({
  connectionStatus: "disconnected",
  setConnectionStatus: (status) => set({ connectionStatus: status }),
}));