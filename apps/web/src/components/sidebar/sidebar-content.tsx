import { ChevronRightIcon, SearchIcon, FolderIcon, PlusIcon, AppWindowIcon } from "lucide-react"
import { SidebarMenu, SidebarMenuItem, SidebarMenuButton, useSidebar } from "@/components/ui/sidebar"
import { SessionList } from "./session-list"
import { useUserSessions, useCreateSession } from "@/hooks/useChatSession"
import { getSessionMessages } from "@/api/session"
import { useState } from "react"

// 获取会话消息的辅助函数
const getSessionMessagesById = async (sessionId: string) => {
  try {
    return await getSessionMessages(sessionId)
  } catch {
    return []
  }
}

// 源创侧边栏内容
export interface YuanChuangSidebarProps {
  selectedSessionId: string
  onSessionSelect: (sessionId: string) => void
}

export function YuanChuangSidebarContent({ selectedSessionId, onSessionSelect }: YuanChuangSidebarProps) {
  const [isCreating, setIsCreating] = useState(false)
  const { open, setOpen, toggleSidebar } = useSidebar()
  const { data: sessions = [] } = useUserSessions()
  const createSession = useCreateSession()

  const DEFAULT_TITLE = "未命名会话"

  // 检查会话是否为空（没有消息）
  const checkAndCreateSession = async () => {
    if (isCreating) return
    setIsCreating(true)

    try {
      // 查找是否有已存在的空会话
      let targetSessionId = ""

      for (const session of sessions) {
        if (session.title === DEFAULT_TITLE) {
          // 获取该会话的消息
          const messages = await getSessionMessagesById(session.sessionId)
          if (messages.length === 0) {
            targetSessionId = session.sessionId
            break
          }
        }
      }

      if (targetSessionId) {
        // 使用已存在的空会话
        onSessionSelect(targetSessionId)
        toggleSidebar()
      } else {
        // 创建新会话
        const newSession = await createSession.mutateAsync({ title: DEFAULT_TITLE })
        if (newSession) {
          onSessionSelect(newSession.sessionId)
          toggleSidebar()
        }
      }
    } catch (error) {
      console.error("[YuanChuangSidebar] Failed to create session:", error)
    } finally {
      setIsCreating(false)
    }
  }

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
          <button
            className="w-full h-10 bg-[#F2F2F2] hover:bg-[#E5E5E5] text-gray-800 rounded-lg flex items-center justify-center gap-2"
            onClick={checkAndCreateSession}
            disabled={isCreating}
          >
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
      <div className="flex-1 px-3 py-4 overflow-y-auto no-scrollbar">
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
