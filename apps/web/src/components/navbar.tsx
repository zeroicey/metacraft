import { useNavigate, useLocation } from "react-router"
import { SidebarTrigger } from "@/components/ui/sidebar"
import { NetworkIcon } from "lucide-react"
import { useState, useRef, useEffect } from "react"
import { useAppStore } from "@/stores/app-store"

export default function Navbar() {
  const navigate = useNavigate()
  const location = useLocation()
  const setCurrentPage = useAppStore((state) => state.setCurrentPage)
  const [slidePos, setSlidePos] = useState(0)
  const yuanChuangRef = useRef<HTMLButtonElement>(null)
  const yuanMengRef = useRef<HTMLButtonElement>(null)

  const isYuanChuang = location.pathname === "/" || location.pathname === "/yuanchuang"
  const isYuanMeng = location.pathname === "/yuanmeng"

  useEffect(() => {
    if (isYuanChuang && yuanChuangRef.current) {
      setSlidePos(0)
      setCurrentPage("yuanchuang")
    } else if (isYuanMeng && yuanMengRef.current) {
      setSlidePos(100)
      setCurrentPage("yuanmeng")
    }
  }, [isYuanChuang, isYuanMeng, setCurrentPage])

  return (
    <header className="flex items-center justify-between border-b px-4 h-16">
      {/* 左侧：Sidebar Trigger */}
      <div className="flex items-center">
        <SidebarTrigger />
      </div>

      {/* 中间：Tab 源创 | 元梦 */}
      <div className="relative flex items-center gap-1 bg-[#F2F2F2] rounded-lg p-1">
        {/* 滑动背景 */}
        <div
          className="absolute top-1 h-[calc(100%-8px)] bg-white rounded-md shadow-sm transition-all duration-300 ease-out"
          style={{
            left: "4px",
            width: "calc(50% - 4px)",
            transform: `translateX(${slidePos}%)`,
          }}
        />

        <button
          ref={yuanChuangRef}
          onClick={() => {
            navigate("/yuanchuang")
            setCurrentPage("yuanchuang")
          }}
          className="relative z-10 px-4 py-1.5 text-sm font-medium rounded-md transition-all text-gray-800"
        >
          源创
        </button>
        <button
          ref={yuanMengRef}
          onClick={() => {
            navigate("/yuanmeng")
            setCurrentPage("yuanmeng")
          }}
          className="relative z-10 px-4 py-1.5 text-sm font-medium rounded-md transition-all text-gray-500"
        >
          元梦
        </button>
      </div>

      {/* 右侧：详情按钮 */}
      <div className="flex items-center">
        <button className="h-8 w-8 p-0 flex items-center justify-center hover:bg-gray-100 rounded-md">
          <NetworkIcon className="h-5 w-5 text-gray-600" />
        </button>
      </div>
    </header>
  )
}