import { create } from "zustand"

type PageType = "yuanchuang" | "yuanmeng"

interface AppStore {
  currentPage: PageType
  setCurrentPage: (page: PageType) => void
}

export const useAppStore = create<AppStore>((set) => ({
  currentPage: "yuanchuang",
  setCurrentPage: (page) => set({ currentPage: page }),
}))