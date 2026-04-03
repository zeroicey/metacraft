import { useNavigate, useLocation } from "react-router"
import { SidebarTrigger } from "@/components/ui/sidebar"
import { SettingsIcon } from "lucide-react"
import { useState, useRef, useEffect } from "react"
import { useAppStore } from "@/stores/app-store"

export default function Navbar() {
  const navigate = useNavigate()
  const location = useLocation()
  const currentPage = useAppStore((state) => state.currentPage)
  const setCurrentPage = useAppStore((state) => state.setCurrentPage)
  const [slidePos, setSlidePos] = useState(0)
  const yuanChuangRef = useRef<HTMLButtonElement>(null)
  const yuanMengRef = useRef<HTMLButtonElement>(null)

  const isYuanChuang = location.pathname === "/" || location.pathname === "/yuanchuang"
  const isYuanMeng = location.pathname === "/yuanmeng"

  // 主题颜色
  const isYuanMengPage = currentPage === "yuanmeng"
  const themeColors = isYuanMengPage
    ? {
      primary: "#EC4899",
      light: "#FDF2F8",
      lightEnd: "#FEF1F7",
      gradient: "from-[#FDF2F8] to-[#FEF1F7]",
    }
    : {
      primary: "#007AFF",
      light: "#E8F0FE",
      lightEnd: "#F0F4F8",
      gradient: "from-[#E8F0FE] to-[#F0F4F8]",
    }

  useEffect(() => {
    if (isYuanChuang && yuanChuangRef.current) {
      setSlidePos(0)
      setCurrentPage("yuanchuang")
    } else if (isYuanMeng && yuanMengRef.current) {
      setSlidePos(1)
      setCurrentPage("yuanmeng")
    }
  }, [isYuanChuang, isYuanMeng, setCurrentPage])

  return (
    <header className="flex items-center justify-between border-[#E5E7EB] bg-white px-4 h-16">
      {/* 左侧：Sidebar Trigger */}
      <div className="flex items-center">
        <SidebarTrigger />
      </div>

      {/* 中间：Tab 源创 | 元梦 */}
      <div className={`relative flex items-center gap-1 bg-gradient-to-br ${themeColors.gradient} rounded-xl p-1`}>
        {/* 滑动背景 */}
        <div
          className="absolute top-1 h-[calc(100%-8px)] bg-white rounded-lg shadow-sm transition-all duration-300 ease-out"
          style={{
            left: "4px",
            width: "calc(50% - 4px)",
            transform: `translateX(${slidePos * 100}%)`,
          }}
        />

        <button
          ref={yuanChuangRef}
          onClick={() => {
            navigate("/yuanchuang")
            setCurrentPage("yuanchuang")
            setSlidePos(0)
          }}
          className={`relative z-10 px-5 py-2 text-sm font-medium rounded-lg transition-all ${currentPage === "yuanchuang" || (!currentPage && isYuanChuang)
            ? "text-[#007AFF]"
            : "text-gray-600 hover:text-gray-900"
            }`}
        >
          源创
        </button>
        <button
          ref={yuanMengRef}
          onClick={() => {
            navigate("/yuanmeng")
            setCurrentPage("yuanmeng")
            setSlidePos(1)
          }}
          className={`relative z-10 px-5 py-2 text-sm font-medium rounded-lg transition-all ${currentPage === "yuanmeng" || isYuanMeng
            ? "text-[#EC4899]"
            : "text-gray-600 hover:text-gray-900"
            }`}
        >
          元梦
        </button>
      </div>

      {/* 右侧：设置按钮 */}
      <div className="flex items-center gap-3">
        <button className="h-9 w-9 p-0 flex items-center justify-center hover:bg-gray-100 rounded-xl transition-colors">
          <SettingsIcon className="h-5 w-5 text-gray-600" />
        </button>
      </div>
    </header>
  )
}
