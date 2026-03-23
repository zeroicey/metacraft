import { SettingsIcon, ChevronRightIcon } from "lucide-react"
import { useCurrentUser } from "@/hooks/useUser"

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

  const { data: user, isLoading } = useCurrentUser()

  // Get avatar URL - handle empty string case
  const getAvatarUrl = () => {
    if (!user?.avatarBase64 || user.avatarBase64 === "") {
      return null
    }
    // If it's already a data URL, use it directly
    if (user.avatarBase64.startsWith("data:")) {
      return user.avatarBase64
    }
    // Otherwise, assume it's a base64 string and wrap in data URL
    return `data:image/png;base64,${user.avatarBase64}`
  }

  const avatarUrl = getAvatarUrl()

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
        {isLoading ? (
          <div className="flex items-center justify-between px-3">
            <div className="flex items-center gap-2">
              <div className="h-8 w-8 animate-pulse rounded-full bg-gray-200" />
              <div className="h-4 w-20 animate-pulse rounded bg-gray-200" />
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-between px-3">
            <div className="flex items-center gap-2 min-w-0">
              {avatarUrl ? (
                <img
                  src={avatarUrl}
                  alt={user?.name || "用户头像"}
                  className="h-8 w-8 rounded-full object-cover flex-shrink-0"
                />
              ) : (
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-[#667eea] to-[#764ba2] flex-shrink-0">
                  <span className="text-sm font-bold text-white">
                    {user?.name?.charAt(0)?.toUpperCase() || "U"}
                  </span>
                </div>
              )}
              <div className="min-w-0 flex-1">
                <div className="text-sm font-medium text-gray-900 truncate">
                  {user?.name || "用户"}
                </div>
                {user?.bio && (
                  <div className="text-xs text-gray-500 truncate">
                    {user.bio}
                  </div>
                )}
              </div>
            </div>
            <button className="h-8 w-8 p-0 flex items-center justify-center flex-shrink-0">
              <SettingsIcon className="h-5 w-5 text-gray-600" />
            </button>
          </div>
        )}
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>
  )
}