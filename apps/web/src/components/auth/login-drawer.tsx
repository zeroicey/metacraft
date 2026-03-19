import { useState } from "react"
import {
  Drawer,
  DrawerContent,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer"
import { Button } from "@/components/ui/button"
import { useAuthStore } from "@/stores/auth-store"
import { login } from "@/api/auth"
import { toast } from "sonner"

const FIXED_PHONE = "1913xxxx7249"
const FIXED_EMAIL = "user@metacraft.com"
const FIXED_PASSWORD = "testuser"

export function LoginDrawer() {
  const isLoginDrawerOpen = useAuthStore((state) => state.isLoginDrawerOpen)
  const closeLoginDrawer = useAuthStore((state) => state.closeLoginDrawer)
  const [agreeProtocol, setAgreeProtocol] = useState(false)
  const [agreePrivacy, setAgreePrivacy] = useState(false)
  const [isLoading, setIsLoading] = useState(false)

  const handleLogin = async () => {
    if (!agreeProtocol || !agreePrivacy) {
      toast.error("请先同意相关协议")
      return
    }

    setIsLoading(true)
    try {
      const result = await login({
        email: FIXED_EMAIL,
        password: FIXED_PASSWORD,
      })
      localStorage.setItem("token", result.token)
      toast.success("登录成功")
      closeLoginDrawer()
    } catch (error) {
      console.error("Login failed:", error)
      toast.error(error instanceof Error ? error.message : "登录失败")
    } finally {
      setIsLoading(false)
    }
  }

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      closeLoginDrawer()
    }
  }

  return (
    <Drawer open={isLoginDrawerOpen} onOpenChange={handleOpenChange} direction="bottom">
      <DrawerContent className="mx-auto max-w-md rounded-t-2xl">
        <DrawerHeader className="text-center">
          <DrawerTitle className="text-lg font-semibold">
            手机号一键登录
          </DrawerTitle>
        </DrawerHeader>

        <div className="px-6 pb-8">
          {/* 手机号显示 */}
          <div className="text-center py-4">
            <span className="text-2xl font-medium text-gray-800">
              {FIXED_PHONE}
            </span>
          </div>

          {/* 协议勾选 */}
          <div className="space-y-3 py-4">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={agreeProtocol}
                onChange={(e) => setAgreeProtocol(e.target.checked)}
                className="w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary"
              />
              <span className="text-sm text-gray-600">
                我同意《元创用户协议》
              </span>
            </label>

            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={agreePrivacy}
                onChange={(e) => setAgreePrivacy(e.target.checked)}
                className="w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary"
              />
              <span className="text-sm text-gray-600">
                我同意《隐私政策》
              </span>
            </label>
          </div>

          {/* 登录按钮 */}
          <Button
            className="w-full h-12 text-base"
            onClick={handleLogin}
            disabled={isLoading || !agreeProtocol || !agreePrivacy}
          >
            {isLoading ? "登录中..." : "登录"}
          </Button>
        </div>
      </DrawerContent>
    </Drawer>
  )
}