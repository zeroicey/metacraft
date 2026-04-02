import { create } from "zustand";
import { persist } from "zustand/middleware";
import { login as apiLogin, register as apiRegister, getCurrentUser } from "@/api/auth";
import type { RegisterRequest } from "@/types/auth";
import type { User } from "@/api/user";

interface AuthStore {
    token: string | null;
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;

    login: (email: string, password: string) => Promise<void>;
    register: (data: RegisterRequest) => Promise<void>;
    logout: () => void;
    initialize: () => Promise<void>;
}

export const useAuthStore = create<AuthStore>()(
    persist(
        (set) => ({
            token: null,
            user: null,
            isAuthenticated: false,
            isLoading: false,
            error: null,

            login: async (email: string, password: string) => {
                set({ isLoading: true, error: null });
                try {
                    const data = await apiLogin({ email, password });
                    localStorage.setItem("token", data.token);
                    // Fetch user info after login
                    const user = await getCurrentUser();
                    set({ token: data.token, user, isAuthenticated: true, isLoading: false });
                } catch (error: unknown) {
                    const message = error instanceof Error ? error.message : "登录失败";
                    set({ error: message, isLoading: false });
                    throw new Error(message);
                }
            },

            register: async (data: RegisterRequest) => {
                set({ isLoading: true, error: null });
                try {
                    const authData = await apiRegister(data);
                    localStorage.setItem("token", authData.token);
                    // Fetch user info after registration
                    const user = await getCurrentUser();
                    set({ token: authData.token, user, isAuthenticated: true, isLoading: false });
                } catch (error: unknown) {
                    const message = error instanceof Error ? error.message : "注册失败";
                    set({ error: message, isLoading: false });
                    throw new Error(message);
                }
            },

            logout: () => {
                localStorage.removeItem("token");
                set({ token: null, user: null, isAuthenticated: false, error: null });
            },

            initialize: async () => {
                const token = localStorage.getItem("token");
                if (token) {
                    set({ token, isAuthenticated: true });
                    try {
                        const user = await getCurrentUser();
                        set({ user });
                    } catch {
                        // Token invalid, clear auth state
                        localStorage.removeItem("token");
                        set({ token: null, user: null, isAuthenticated: false });
                    }
                }
            },
        }),
        {
            name: "auth-storage",
            partialize: (state) => ({ token: state.token, isAuthenticated: state.isAuthenticated }),
        }
    )
);