import { ChevronRightIcon, SearchIcon, FolderIcon, PlusIcon, AppWindowIcon } from "lucide-react"
import { SidebarMenu, SidebarMenuItem, SidebarMenuButton } from "@/components/ui/sidebar"
import { SessionList } from "./session-list"

// ============ 源创侧边栏内容 ============
export interface YuanChuangSidebarProps {
  selectedSessionId: string
  onSessionSelect: (sessionId: string) => void
}

export function YuanChuangSidebarContent({ selectedSessionId, onSessionSelect }: YuanChuangSidebarProps) {
  const navItems = [
    { icon: AppWindowIcon, label: "我的元应用" },
    { icon: SearchIcon, label: "元应用商店" },
    { icon: FolderIcon, label: "元数据中心" },
  ]

  return (
    <>
      {/* Create New App Button */}
      <div className="px-3 py-2">
        <SidebarMenuButton asChild>
          <button className="w-full h-10 bg-[#F2F2F2] hover:bg-[#E5E5E5] text-gray-800 rounded-lg flex items-center justify-center gap-2">
            <PlusIcon className="h-4 w-4" />
            <span className="text-sm">创建新应用</span>
          </button>
        </SidebarMenuButton>
      </div>

      {/* Navigation Menu */}
      <div className="px-3 py-2">
        <SidebarMenu>
          {navItems.map((item) => (
            <SidebarMenuItem key={item.label}>
              <SidebarMenuButton asChild>
                <button className="flex items-center gap-2 w-full">
                  <item.icon className="h-4 w-4 text-gray-500" />
                  <span className="flex-1 text-sm text-gray-700">{item.label}</span>
                  <ChevronRightIcon className="h-4 w-4 text-gray-400" />
                </button>
              </SidebarMenuButton>
            </SidebarMenuItem>
          ))}
        </SidebarMenu>
      </div>

      {/* Session List */}
      <div className="flex-1 px-3 py-4 overflow-y-auto">
        <SessionList
          selectedSessionId={selectedSessionId}
          onSessionSelect={onSessionSelect}
        />
      </div>
    </>
  )
}

// 重新导出沙盒监控组件
export { YuanMengSidebarContent } from "./sandbox-monitor"