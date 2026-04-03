import { ChevronRightIcon } from "lucide-react"
import { useCurrentUser } from "@/hooks/useUser"
import { useNavigate } from "react-router"

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
  const navigate = useNavigate()
  const currentPage = useAppStore((state) => state.currentPage)
  const selectedSessionId = useAppStore((state) => state.selectedSessionId)
  const setSelectedSessionId = useAppStore((state) => state.setSelectedSessionId)

  const { data: user, isLoading } = useCurrentUser()

  // Get avatar URL - handle empty string case
  const getAvatarUrl = () => {
    if (!user?.avatarBase64 || user.avatarBase64 === "") {
      // Use DiceBear pixel-art avatar with user's name as seed
      const seed = encodeURIComponent(user?.name || "user")
      return `https://api.dicebear.com/7.x/pixel-art/svg?seed=${seed}`
    }
    // If it's already a data URL, use it directly
    if (user.avatarBase64.startsWith("data:")) {
      return user.avatarBase64
    }
    // Otherwise, assume it's a base64 string and wrap in data URL
    return `data:image/png;base64,${user.avatarBase64}`
  }

  const avatarUrl = getAvatarUrl()

  // 主题颜色
  const isYuanMengPage = currentPage === "yuanmeng"
  const themeStyle = isYuanMengPage
    ? {
        logoGradient: "from-[#EC4899] to-[#BE185D]",
        textColor: "text-gray-800",
        activeText: "text-[#EC4899]",
      }
    : {
        logoGradient: "from-[#007AFF] to-[#0056CC]",
        textColor: "text-gray-800",
        activeText: "text-[#007AFF]",
      }

  return (
    <Sidebar>
      <SidebarHeader className="pt-12 pb-2">
        <div className="flex items-center justify-between px-2">
          <div className="flex items-center gap-2">
            <img src="/logo.png" alt="Logo" className="h-7 w-7 p-1" />
            <span className={`text-lg font-bold ${themeStyle.textColor}`}>元创</span>
          </div>
          <button className="h-6 w-6 p-0 flex items-center justify-center hover:bg-gray-100 rounded-lg transition-colors">
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
          <button
            className="flex items-center justify-between px-3 w-full hover:bg-gray-50 rounded-xl transition-colors py-1"
            onClick={() => navigate("/profile")}
          >
            <div className="flex items-center gap-2 min-w-0">
              <img
                src={avatarUrl}
                alt={user?.name || "用户头像"}
                className="h-8 w-8 rounded-full object-cover flex-shrink-0"
              />
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
            <ChevronRightIcon className="h-4 w-4 text-gray-400 flex-shrink-0" />
          </button>
        )}
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>
  )
}