import { create } from "zustand"

interface AuthStore {
  isLoginDrawerOpen: boolean
  openLoginDrawer: () => void
  closeLoginDrawer: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  isLoginDrawerOpen: false,
  openLoginDrawer: () => set({ isLoginDrawerOpen: true }),
  closeLoginDrawer: () => set({ isLoginDrawerOpen: false }),
}))