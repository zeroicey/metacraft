import { useState, useEffect, useRef } from "react"
import { LineChart, Line, XAxis, YAxis, ResponsiveContainer } from "recharts"
import { Progress } from "@/components/ui/progress"
import { ChevronRightIcon, SearchIcon, FolderIcon, PlusIcon, AppWindowIcon, HardDriveIcon, CpuIcon, MemoryStickIcon, NetworkIcon } from "lucide-react"
import { SidebarMenu, SidebarMenuItem, SidebarMenuButton } from "@/components/ui/sidebar"

// ============ 源创侧边栏内容 ============
export function YuanChuangSidebarContent() {
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

      {/* Session List Placeholder */}
      <div className="flex-1 px-3 py-4">
        <div className="text-sm text-gray-400 text-center py-8">
          会话列表区域
        </div>
      </div>
    </>
  )
}

// ============ 元梦侧边栏沙盒监控 ============

interface SandboxData {
  status: "running" | "stopped"
  duration: number // 秒
  cpu: number // 0-100
  memory: number // 0-100
  diskUsed: number // GB
  diskTotal: number // GB
  networkHistory: { time: number; up: number; down: number }[] // 最近30秒
}

// 随机生成 0-100 之间的值，带平滑变化（幅度更小）
const randomValue = (prev: number, minVal: number = 5, maxChange: number = 8): number => {
  const change = (Math.random() - 0.5) * maxChange * 2
  return Math.max(minVal, Math.min(95, prev + change))
}

// 格式化网速
const formatSpeed = (kbPerSec: number): string => {
  if (kbPerSec >= 1024) return `${(kbPerSec / 1024).toFixed(1)} MB/s`
  return `${kbPerSec.toFixed(0)} KB/s`
}

// 格式化时长
const formatDuration = (seconds: number): string => {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${h.toString().padStart(2, "0")}:${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`
}

function SandboxStatus({ status, duration }: { status: string; duration: number }) {
  const isRunning = status === "running"
  return (
    <div className="flex items-center justify-between px-3 py-2">
      <div className="flex items-center gap-2">
        <div className={`w-2 h-2 rounded-full ${isRunning ? "bg-green-500" : "bg-gray-400"}`} />
        <span className="text-sm font-medium">{isRunning ? "运行中" : "已停止"}</span>
      </div>
      <span className="text-sm text-gray-500 font-mono">{formatDuration(duration)}</span>
    </div>
  )
}

function ResourceBar({
  label,
  icon: Icon,
  value,
  showValue = true
}: {
  label: string
  icon: React.ComponentType<{ className?: string }>
  value: number
  showValue?: boolean
}) {
  const getColor = (v: number) => {
    if (v < 60) return "bg-green-500"
    if (v < 80) return "bg-yellow-500"
    return "bg-red-500"
  }

  return (
    <div className="px-3 py-2">
      <div className="flex items-center gap-2 mb-1">
        <Icon className="h-4 w-4 text-gray-500" />
        <span className="text-sm text-gray-600">{label}</span>
        {showValue && <span className="ml-auto text-sm font-medium">{value.toFixed(1)}%</span>}
      </div>
      <Progress value={value} className="h-2" />
    </div>
  )
}

function DiskInfo({ used, total }: { used: number; total: number }) {
  return (
    <div className="px-3 py-2 flex items-center justify-between">
      <div className="flex items-center gap-2">
        <HardDriveIcon className="h-4 w-4 text-gray-500" />
        <span className="text-sm text-gray-600">磁盘</span>
      </div>
      <span className="text-sm text-gray-500">{used.toFixed(1)} / {total} GB</span>
    </div>
  )
}

function NetworkChart({ data }: { data: { time: number; up: number; down: number }[] }) {
  const latestData = data[data.length - 1]

  return (
    <div className="px-3 py-2">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <NetworkIcon className="h-4 w-4 text-gray-500" />
          <span className="text-sm text-gray-600">网络</span>
        </div>
        {latestData && (
          <div className="flex items-center gap-3 text-xs">
            <span className="text-green-600">{formatSpeed(latestData.down)}</span>
            <span className="text-blue-600">{formatSpeed(latestData.up)}</span>
          </div>
        )}
      </div>
      <div className="h-24">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 5, right: 5, bottom: 5, left: 5 }}>
            <XAxis dataKey="time" hide />
            <YAxis hide domain={[0, "auto"]} />
            <Line
              type="monotone"
              dataKey="down"
              stroke="#22c55e"
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
            <Line
              type="monotone"
              dataKey="up"
              stroke="#3b82f6"
              strokeWidth={2}
              dot={false}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <div className="flex justify-center gap-4 mt-1">
        <div className="flex items-center gap-1">
          <div className="w-2 h-2 rounded-full bg-green-500" />
          <span className="text-xs text-gray-500">下载</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-2 h-2 rounded-full bg-blue-500" />
          <span className="text-xs text-gray-500">上传</span>
        </div>
      </div>
    </div>
  )
}

export function YuanMengSidebarContent() {
  const [sandbox, setSandbox] = useState<SandboxData>({
    status: "running",
    duration: 0,
    cpu: 30,
    memory: 45,
    diskUsed: 2.5,
    diskTotal: 10,
    networkHistory: [],
  })

  const networkRef = useRef<{ up: number; down: number }>({ up: 100, down: 200 })

  useEffect(() => {
    // 初始化一些网络历史数据
    const now = Date.now()
    const initialHistory = Array.from({ length: 30 }, (_, i) => ({
      time: now - (29 - i) * 1000,
      up: 100 + Math.random() * 50,
      down: 200 + Math.random() * 80,
    }))
    setSandbox(prev => ({ ...prev, networkHistory: initialHistory }))

    const interval = setInterval(() => {
      setSandbox((prev) => {
        const newNetworkRef = {
          up: Math.max(10, networkRef.current.up + (Math.random() - 0.5) * 50),
          down: Math.max(10, networkRef.current.down + (Math.random() - 0.5) * 80),
        }
        networkRef.current = newNetworkRef

        const now = Date.now()
        const newHistory = [
          ...prev.networkHistory.filter((h) => now - h.time < 30000),
          { time: now, up: newNetworkRef.up, down: newNetworkRef.down },
        ]

        return {
          ...prev,
          duration: prev.duration + 1,
          cpu: randomValue(prev.cpu),
          memory: randomValue(prev.memory),
          networkHistory: newHistory,
        }
      })
    }, 1000)

    return () => clearInterval(interval)
  }, [])

  return (
    <div className="flex flex-col">
      <div className="border-b">
        <SandboxStatus status={sandbox.status} duration={sandbox.duration} />
      </div>
      <div className="border-b">
        <ResourceBar label="CPU" icon={CpuIcon} value={sandbox.cpu} />
      </div>
      <div className="border-b">
        <ResourceBar label="内存" icon={MemoryStickIcon} value={sandbox.memory} />
      </div>
      <div className="border-b">
        <DiskInfo used={sandbox.diskUsed} total={sandbox.diskTotal} />
      </div>
      <div>
        <NetworkChart data={sandbox.networkHistory} />
      </div>
    </div>
  )
}