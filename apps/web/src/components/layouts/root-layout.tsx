import QueryProvider from "@/providers/QueryProvider";
import { Outlet } from "react-router";
import { Toaster } from "sonner";
import { SidebarProvider } from "../ui/sidebar";
import { AppSidebar } from "@/components/sidebar";
import Navbar from "@/components/navbar";

export default function RootLayout() {
  return (
    <QueryProvider>
      <Toaster position={"top-center"} />
      <SidebarProvider>
        <div className="flex h-screen w-screen overflow-hidden">
          <AppSidebar />
          <div className="flex flex-col flex-1 overflow-hidden ">
            <Navbar />
            <div className="flex-1 overflow-auto px-1 py-4">
              <Outlet />
            </div>
          </div>
        </div>
      </SidebarProvider>
    </QueryProvider>
  );
}

