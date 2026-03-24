# 登录注册重构实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将硬编码弹窗登录改为独立路由页面登录/注册流程

**Architecture:** 修改 HTTP 拦截器在 401/403 时跳转登录页，重构 Auth Store 管理认证状态，创建独立的登录和注册页面

**Tech Stack:** React, Zustand, React Router, shadcn/ui

---

## 文件结构

| 操作 | 文件路径 |
|------|----------|
| 修改 | `apps/web/src/lib/http.ts` |
| 重构 | `apps/web/src/stores/auth-store.ts` |
| 删除 | `apps/web/src/components/auth/login-drawer.tsx` |
| 新增 | `apps/web/src/pages/login.tsx` |
| 新增 | `apps/web/src/pages/register.tsx` |
| 修改 | `apps/web/src/router.tsx` (或路由配置文件) |

---

## Chunk 1: HTTP 拦截器和 Auth Store 重构

### Task 1: 修改 HTTP 拦截器

**Files:**
- Modify: `apps/web/src/lib/http.ts:27-34`

- [ ] **Step 1: 修改 401/403 处理逻辑**

将:
```typescript
afterResponse: [
    (_request, _options, response) => {
        if (response.status === 401 || response.status === 403) {
            const openLoginDrawer = useAuthStore.getState().openLoginDrawer;
            openLoginDrawer();
        }
    },
],
```

改为:
```typescript
afterResponse: [
    (_request, _options, response) => {
        if (response.status === 401 || response.status === 403) {
            // 清除本地存储的 token
            localStorage.removeItem('token');
            // 跳转到登录页
            window.location.href = '/login';
        }
    },
],
```

- [ ] **Step 2: 移除未使用的 useAuthStore import**

删除:
```typescript
import { useAuthStore } from "@/stores/auth-store";
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/lib/http.ts
git commit -m "refactor(http): redirect to /login on 401/403 instead of opening drawer"
```

---

### Task 2: 重构 Auth Store

**Files:**
- Modify: `apps/web/src/stores/auth-store.ts`

- [ ] **Step 1: 查看现有 user 类型**

先查看 `apps/web/src/hooks/useUser.ts` 了解 User 类型结构

- [ ] **Step 2: 重写 auth-store.ts**

```typescript
import { create } from "zustand";
import { persist } from "zustand/middleware";
import http from "@/lib/http";
import { ApiResponse } from "@/lib/http";

interface User {
    id: number;
    email: string;
    name: string;
    avatarBase64?: string;
    bio?: string;
}

interface LoginData {
    email: string;
    password: string;
}

interface RegisterData {
    email: string;
    password: string;
    name: string;
    bio: string;
}

interface AuthTokenVO {
    token: string;
    user: User;
}

interface AuthStore {
    token: string | null;
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    error: string | null;

    login: (email: string, password: string) => Promise<void>;
    register: (data: RegisterData) => Promise<void>;
    logout: () => void;
    initialize: () => void;
}

export const useAuthStore = create<AuthStore>()(
    persist(
        (set, get) => ({
            token: null,
            user: null,
            isAuthenticated: false,
            isLoading: false,
            error: null,

            login: async (email: string, password: string) => {
                set({ isLoading: true, error: null });
                try {
                    const response = await http.post<ApiResponse<AuthTokenVO>>("auth/login", {
                        json: { email, password },
                    });
                    if (response.data) {
                        const { token, user } = response.data;
                        localStorage.setItem("token", token);
                        set({ token, user, isAuthenticated: true, isLoading: false });
                    }
                } catch (error: unknown) {
                    const err = error as { response?: { data?: { message?: string } } };
                    const message = err.response?.data?.message || "登录失败";
                    set({ error: message, isLoading: false });
                    throw new Error(message);
                }
            },

            register: async (data: RegisterData) => {
                set({ isLoading: true, error: null });
                try {
                    const response = await http.post<ApiResponse<AuthTokenVO>>("auth/register", {
                        json: data,
                    });
                    if (response.data) {
                        const { token, user } = response.data;
                        localStorage.setItem("token", token);
                        set({ token, user, isAuthenticated: true, isLoading: false });
                    }
                } catch (error: unknown) {
                    const err = error as { response?: { data?: { message?: string } } };
                    const message = err.response?.data?.message || "注册失败";
                    set({ error: message, isLoading: false });
                    throw new Error(message);
                }
            },

            logout: () => {
                localStorage.removeItem("token");
                set({ token: null, user: null, isAuthenticated: false, error: null });
            },

            initialize: () => {
                const token = localStorage.getItem("token");
                if (token) {
                    set({ token, isAuthenticated: true });
                }
            },
        }),
        {
            name: "auth-storage",
            partialize: (state) => ({ token: state.token, user: state.user, isAuthenticated: state.isAuthenticated }),
        }
    )
);
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/stores/auth-store.ts
git commit -m "refactor(auth): rewrite store with full auth state management"
```

---

## Chunk 2: 创建登录和注册页面

### Task 3: 创建登录页面

**Files:**
- Create: `apps/web/src/pages/login.tsx`

- [ ] **Step 1: 创建 login.tsx**

```tsx
import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { useAuthStore } from "@/stores/auth-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2Icon } from "lucide-react";
import { toast } from "sonner";

export default function LoginPage() {
    const navigate = useNavigate();
    const { login, isLoading, error } = useAuthStore();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await login(email, password);
            toast.success("登录成功");
            navigate("/");
        } catch {
            // Error is handled in store
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
            <Card className="w-full max-w-sm">
                <CardHeader className="text-center">
                    <CardTitle className="text-2xl">登录</CardTitle>
                    <CardDescription>欢迎回来！请登录您的账号</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={handleSubmit} className="space-y-4">
                        <div className="space-y-2">
                            <Input
                                type="email"
                                placeholder="邮箱"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                disabled={isLoading}
                            />
                        </div>
                        <div className="space-y-2">
                            <Input
                                type="password"
                                placeholder="密码"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                disabled={isLoading}
                            />
                        </div>

                        {error && (
                            <p className="text-sm text-red-500 text-center">{error}</p>
                        )}

                        <Button type="submit" className="w-full" disabled={isLoading}>
                            {isLoading ? (
                                <>
                                    <Loader2Icon className="mr-2 h-4 w-4 animate-spin" />
                                    登录中...
                                </>
                            ) : (
                                "登录"
                            )}
                        </Button>
                    </form>

                    <p className="mt-4 text-center text-sm text-gray-600">
                        还没有账号？{" "}
                        <Link to="/register" className="text-primary hover:underline font-medium">
                            去注册
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </div>
    );
}
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/pages/login.tsx
git commit -m "feat(web): create login page"
```

---

### Task 4: 创建注册页面

**Files:**
- Create: `apps/web/src/pages/register.tsx`

- [ ] **Step 1: 创建 register.tsx**

```tsx
import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { useAuthStore } from "@/stores/auth-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2Icon, EyeIcon, EyeOffIcon } from "lucide-react";
import { toast } from "sonner";

export default function RegisterPage() {
    const navigate = useNavigate();
    const { register, isLoading, error } = useAuthStore();
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [name, setName] = useState("");
    const [bio, setBio] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [passwordError, setPasswordError] = useState("");

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setPasswordError("");

        if (password !== confirmPassword) {
            setPasswordError("两次输入的密码不一致");
            return;
        }

        try {
            await register({ email, password, name, bio });
            toast.success("注册成功");
            navigate("/");
        } catch {
            // Error is handled in store
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-8">
            <Card className="w-full max-w-sm">
                <CardHeader className="text-center">
                    <CardTitle className="text-2xl">注册</CardTitle>
                    <CardDescription>创建您的账号</CardDescription>
                </CardHeader>
                <CardContent>
                    <form onSubmit={handleSubmit} className="space-y-3">
                        <div className="space-y-2">
                            <Input
                                type="email"
                                placeholder="邮箱"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                                disabled={isLoading}
                            />
                        </div>
                        <div className="space-y-2">
                            <Input
                                type="text"
                                placeholder="姓名"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                required
                                disabled={isLoading}
                            />
                        </div>
                        <div className="space-y-2 relative">
                            <Input
                                type={showPassword ? "text" : "password"}
                                placeholder="密码"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                                disabled={isLoading}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                            >
                                {showPassword ? (
                                    <EyeOffIcon className="h-4 w-4" />
                                ) : (
                                    <EyeIcon className="h-4 w-4" />
                                )}
                            </button>
                        </div>
                        <div className="space-y-2">
                            <Input
                                type={showPassword ? "text" : "password"}
                                placeholder="确认密码"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                required
                                disabled={isLoading}
                            />
                        </div>
                        <div className="space-y-2">
                            <Textarea
                                placeholder="简介"
                                value={bio}
                                onChange={(e) => setBio(e.target.value)}
                                required
                                disabled={isLoading}
                                rows={3}
                            />
                        </div>

                        {(error || passwordError) && (
                            <p className="text-sm text-red-500 text-center">{error || passwordError}</p>
                        )}

                        <Button type="submit" className="w-full" disabled={isLoading}>
                            {isLoading ? (
                                <>
                                    <Loader2Icon className="mr-2 h-4 w-4 animate-spin" />
                                    注册中...
                                </>
                            ) : (
                                "注册"
                            )}
                        </Button>
                    </form>

                    <p className="mt-4 text-center text-sm text-gray-600">
                        已有账号？{" "}
                        <Link to="/login" className="text-primary hover:underline font-medium">
                            去登录
                        </Link>
                    </p>
                </CardContent>
            </Card>
        </div>
    );
}
```

- [ ] **Step 2: 检查是否需要 Textarea 组件**

查看 `apps/web/src/components/ui/textarea.tsx` 是否存在，如不存在需要创建

如果不存在，运行:
```bash
cd apps/web && bunx shadcn@latest add textarea --yes
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/register.tsx
git commit -m "feat(web): create register page"
```

---

## Chunk 3: 路由配置和清理

### Task 5: 更新路由配置

**Files:**
- Modify: `apps/web/src/router.ts`

- [ ] **Step 1: 添加登录和注册页面导入**

在文件顶部添加:
```typescript
const LoginPage = lazy(() => import("@/pages/login"));
const RegisterPage = lazy(() => import("@/pages/register"));
```

- [ ] **Step 2: 添加路由**

在 router 数组中添加新的路由配置:
```typescript
{
    path: "/login",
    Component: StandaloneLayout,
    children: [
        { index: true, Component: LoginPage },
    ],
},
{
    path: "/register",
    Component: StandaloneLayout,
    children: [
        { index: true, Component: RegisterPage },
    ],
},
```

完整 router 配置如下:
```typescript
const router = createHashRouter([
    {
        path: "/",
        Component: RootLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: YuanChuangPage },
            { path: "yuanchuang", Component: YuanChuangPage },
            { path: "yuanmeng", Component: YuanMengPage },
            { path: "*", Component: NotFoundPage },
        ],
    },
    {
        path: "/preview",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: PreviewPage },
        ],
    },
    {
        path: "/myapps",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: MyAppsPage },
        ],
    },
    {
        path: "/login",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: LoginPage },
        ],
    },
    {
        path: "/register",
        Component: StandaloneLayout,
        ErrorBoundary: ErrorPage,
        children: [
            { index: true, Component: RegisterPage },
        ],
    },
]);
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/router.ts
git commit -m "feat(web): add /login and /register routes"
```

---

### Task 6: 删除 login-drawer 组件

**Files:**
- Delete: `apps/web/src/components/auth/login-drawer.tsx`

- [ ] **Step 1: 检查是否有其他地方引用 login-drawer**

```bash
grep -r "login-drawer" apps/web/src --include="*.tsx" --include="*.ts"
```

- [ ] **Step 2: 删除文件**

```bash
rm apps/web/src/components/auth/login-drawer.tsx
```

- [ ] **Step 3: 提交**

```bash
git rm apps/web/src/components/auth/login-drawer.tsx
git commit -m "feat(web): remove login drawer component"
```

---

## Chunk 4: 验证和测试

### Task 7: 验证构建

- [ ] **Step 1: 运行 TypeScript 编译**

```bash
cd apps/web && bunx tsc -b
```

- [ ] **Step 2: 运行构建**

```bash
cd apps/web && bun vite build
```

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "chore(web): verify build passes"
```

---

## 验收检查清单

- [ ] HTTP 拦截器在 401/403 时跳转到 /login
- [ ] 登录页面可以正常登录
- [ ] 注册页面可以正常注册
- [ ] 注册成功后自动登录并跳转首页
- [ ] 登录/注册页面可以相互跳转
- [ ] 响应式布局在移动端和桌面端正常工作
- [ ] 登录成功后 token 存储到 localStorage
- [ ] 登出功能正常（需要在侧边栏添加登出按钮）

---

## 额外任务（可选）

如果需要完善登出功能，可以添加:

**修改侧边栏:**
- 在 `apps/web/src/components/sidebar/index.tsx` 添加登出按钮
- 点击后调用 `useAuthStore.getState().logout()` 并跳转到登录页