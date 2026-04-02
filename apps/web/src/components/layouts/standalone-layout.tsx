import QueryProvider from "@/providers/QueryProvider";
import { Outlet } from "react-router";
import { Toaster } from "sonner";

/**
 * 独立布局 - 用于不需要侧边栏和导航栏的页面
 * 例如：预览页面、个人中心、设置等
 */
export default function StandaloneLayout() {
  return (
    <QueryProvider>
      <Toaster position={"top-center"} />
      <div className="h-screen w-screen overflow-hidden bg-white">
        <Outlet />
      </div>
    </QueryProvider>
  );
}