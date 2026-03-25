import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
    getStoreApps,
    getStoreAppDetail,
    rateApp,
    commentApp,
    deleteComment,
    type StoreAppItem,
    type StoreAppDetail,
    type RatingResult,
    type StoreComment,
} from "@/api/store";

/**
 * Get store apps list
 */
export function useStoreApps() {
    return useQuery<StoreAppItem[]>({
        queryKey: ["store", "apps"],
        queryFn: getStoreApps,
    });
}

/**
 * Get app detail by ID
 */
export function useStoreAppDetail(appId: number) {
    return useQuery<StoreAppDetail>({
        queryKey: ["store", "app", appId],
        queryFn: () => getStoreAppDetail(appId),
        enabled: !!appId,
    });
}

/**
 * Submit rating for an app
 */
export function useRateApp(appId: number) {
    const queryClient = useQueryClient();

    return useMutation<RatingResult, Error, number>({
        mutationFn: (rating: number) => rateApp(appId, rating),
        onSuccess: () => {
            // Refresh app detail
            queryClient.invalidateQueries({ queryKey: ["store", "app", appId] });
        },
    });
}

/**
 * Submit comment for an app
 */
export function useCommentApp(appId: number) {
    const queryClient = useQueryClient();

    return useMutation<StoreComment, Error, string>({
        mutationFn: (content: string) => commentApp(appId, content),
        onSuccess: () => {
            // Refresh app detail
            queryClient.invalidateQueries({ queryKey: ["store", "app", appId] });
        },
    });
}

/**
 * Delete a comment
 */
export function useDeleteComment(appId: number) {
    const queryClient = useQueryClient();

    return useMutation<void, Error, number>({
        mutationFn: (commentId: number) => deleteComment(appId, commentId),
        onSuccess: () => {
            // Refresh app detail
            queryClient.invalidateQueries({ queryKey: ["store", "app", appId] });
        },
    });
}