import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getCurrentUser, updateUser, type UserUpdateRequest } from "@/api/user";
import { toast } from "sonner";

/**
 * 获取当前用户信息
 */
export function useCurrentUser() {
  return useQuery({
    queryKey: ["current-user"],
    queryFn: getCurrentUser,
  });
}

/**
 * 更新用户信息
 */
export function useUpdateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UserUpdateRequest) => updateUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["current-user"] });
      toast.success("更新成功");
    },
    onError: (error: Error) => {
      toast.error(error.message || "更新失败");
    },
  });
}