import QueryProvider from "@/providers/QueryProvider";
import { Outlet } from "react-router";
import { Toaster } from "sonner";

export default function PreviewLayout() {
  return (
    <QueryProvider>
      <Toaster position={"top-center"} />
      <div className="h-screen w-screen overflow-hidden bg-white">
        <Outlet />
      </div>
    </QueryProvider>
  );
}