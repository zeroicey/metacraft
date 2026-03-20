import { useMemo } from "react"
import { MessageSquareIcon } from "lucide-react"
import { useUserSessions } from "@/hooks/useChatSession"
import type { ChatSession } from "@/types/session"

// 时间分组
interface SessionGroup {
  label: string
  sessions: ChatSession[]
}

// 按时间分组会话
function groupSessionsByTime(sessions: ChatSession[]): SessionGroup[] {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)
  const weekAgo = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000)
  const monthAgo = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000)

  const groups: SessionGroup[] = [
    { label: "今天", sessions: [] },
    { label: "昨天", sessions: [] },
    { label: "一星期内", sessions: [] },
    { label: "三十天前", sessions: [] },
  ]

  sessions.forEach((session) => {
    const sessionDate = new Date(session.updatedAt)

    if (sessionDate >= today) {
      groups[0].sessions.push(session)
    } else if (sessionDate >= yesterday) {
      groups[1].sessions.push(session)
    } else if (sessionDate >= weekAgo) {
      groups[2].sessions.push(session)
    } else {
      groups[3].sessions.push(session)
    }
  })

  // 只返回有会话的组
  return groups.filter((group) => group.sessions.length > 0)
}

// 格式化时间显示
function formatSessionTime(dateString: string): string {
  const date = new Date(dateString)
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const sessionDate = new Date(date.getFullYear(), date.getMonth(), date.getDate())

  if (sessionDate.getTime() === today.getTime()) {
    return date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })
  }

  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)
  if (sessionDate.getTime() === yesterday.getTime()) {
    return "昨天"
  }

  return date.toLocaleDateString("zh-CN", { month: "numeric", day: "numeric" })
}

interface SessionItemProps {
  session: ChatSession
  isSelected: boolean
  onSelect: (sessionId: string) => void
}

function SessionItem({ session, isSelected, onSelect }: SessionItemProps) {
  return (
    <div
      className={`flex items-center gap-2 px-2 py-1.5 rounded-md cursor-pointer transition-colors ${isSelected ? "bg-gray-200" : "hover:bg-gray-100"
        }`}
      onClick={() => onSelect(session.sessionId)}
    >
      <MessageSquareIcon className="h-4 w-4 text-gray-400 flex-shrink-0" />
      <span className="flex-1 text-sm text-gray-700 truncate">{session.title}</span>
      <span className="text-xs text-gray-400 flex-shrink-0">
        {formatSessionTime(session.updatedAt)}
      </span>
    </div>
  )
}

interface SessionListProps {
  selectedSessionId: string
  onSessionSelect: (sessionId: string) => void
}

export function SessionList({ selectedSessionId, onSessionSelect }: SessionListProps) {
  const { data: sessions = [], isLoading } = useUserSessions()
  const groupedSessions = useMemo(() => groupSessionsByTime(sessions), [sessions])

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="text-sm text-gray-400">加载中...</div>
      </div>
    )
  }

  if (sessions.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-8 text-center">
        <MessageSquareIcon className="h-8 w-8 text-gray-300 mb-2" />
        <div className="text-sm text-gray-400">暂无会话</div>
        <div className="text-xs text-gray-300 mt-1">点击上方"创建新应用"开始对话</div>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      {groupedSessions.map((group) => (
        <div key={group.label}>
          <div className="text-xs font-medium text-gray-400 mb-1 px-2">{group.label}</div>
          <div className="flex flex-col gap-0.5">
            {group.sessions.map((session) => (
              <SessionItem
                key={session.sessionId}
                session={session}
                isSelected={selectedSessionId === session.sessionId}
                onSelect={onSessionSelect}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
