import { SettingsIcon, CircleUserIcon, ChevronRightIcon } from "lucide-react"

import {
  Sidebar,
  SidebarHeader,
  SidebarFooter,
  SidebarRail,
  SidebarContent,
} from "@/components/ui/sidebar"
import { useAppStore } from "@/stores/app-store"
import { YuanChuangSidebarContent, YuanMengSidebarContent } from "@/components/sidebar/sidebar-content"

export function AppSidebar() {
  const currentPage = useAppStore((state) => state.currentPage)
  const selectedSessionId = useAppStore((state) => state.selectedSessionId)
  const setSelectedSessionId = useAppStore((state) => state.setSelectedSessionId)

  return (
    <Sidebar>
      <SidebarHeader className="pt-12 pb-2">
        <div className="flex items-center justify-between px-2">
          <div className="flex items-center gap-2">
            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-[#222222]">
              <span className="text-xs font-bold text-white">元</span>
            </div>
            <span className="text-lg font-bold text-gray-800">元创</span>
          </div>
          <button className="h-6 w-6 p-0 flex items-center justify-center">
            <ChevronRightIcon className="h-4 w-4 text-gray-400" />
          </button>
        </div>
      </SidebarHeader>

      <SidebarContent className="flex-1">
        {currentPage === "yuanchuang" ? (
          <YuanChuangSidebarContent
            selectedSessionId={selectedSessionId}
            onSessionSelect={setSelectedSessionId}
          />
        ) : (
          <YuanMengSidebarContent />
        )}
      </SidebarContent>

      <SidebarFooter className="pb-6 pt-2">
        <div className="flex items-center justify-between px-3">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-[#444444]">
              <CircleUserIcon className="h-4 w-4 text-white" />
            </div>
            <span className="text-sm font-medium">用户名</span>
          </div>
          <button className="h-8 w-8 p-0 flex items-center justify-center">
            <SettingsIcon className="h-5 w-5 text-gray-600" />
          </button>
        </div>
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>
  )
}