import { useMutation, useQueryClient, useQuery } from "@tanstack/react-query";
import { createApp, deleteApp, getUserApps } from "@/api/app";
import type { App, CreateAppRequest } from "@/types/app";
import { toast } from "sonner";

/**
 * 获取用户所有应用列表
 * @param enabled 是否启用查询，默认 true
 */
export function useUserApps(enabled: boolean = true) {
    return useQuery({
        queryKey: ["user-apps"],
        queryFn: getUserApps,
        enabled,
    });
}

/**
 * 创建应用 mutation
 */
export function useCreateApp() {
    const queryClient = useQueryClient();

    return useMutation<App, Error, CreateAppRequest>({
        mutationFn: createApp,
        onSuccess: () => {
            toast.success("App created successfully!");
            queryClient.invalidateQueries({ queryKey: ["user-apps"] });
        },
        onError: (error) => {
            console.error("Failed to create app:", error);
            toast.error(error.message || "Failed to create app");
        },
    });
}

/**
 * 删除应用 mutation
 */
export function useDeleteApp() {
    const queryClient = useQueryClient();

    return useMutation<void, Error, number>({
        mutationFn: deleteApp,
        onSuccess: () => {
            toast.success("App deleted successfully!");
            queryClient.invalidateQueries({ queryKey: ["user-apps"] });
        },
        onError: (error) => {
            console.error("Failed to delete app:", error);
            toast.error(error.message || "Failed to delete app");
        },
    });
}