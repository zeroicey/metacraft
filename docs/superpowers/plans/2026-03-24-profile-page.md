# 个人中心页面实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建个人中心页面，用于用户查看和编辑个人信息，以及退出登录

**Architecture:** 创建 profile 页面组件，使用现有的 useCurrentUser 和 useUpdateUser hooks，添加路由和侧边栏跳转

**Tech Stack:** React, shadcn/ui, Zustand

---

## 文件结构

| 操作 | 文件路径 |
|------|----------|
| 新增 | `apps/web/src/pages/profile.tsx` |
| 修改 | `apps/web/src/router.ts` |
| 修改 | `apps/web/src/components/sidebar/index.tsx` |

---

## Chunk 1: 创建 Profile 页面

### Task 1: 创建 profile.tsx

**Files:**
- Create: `apps/web/src/pages/profile.tsx`

- [ ] **Step 1: 创建 profile 页面**

```tsx
import { useState, useRef } from "react";
import { useNavigate } from "react-router";
import { useCurrentUser, useUpdateUser } from "@/hooks/useUser";
import { useAuthStore } from "@/stores/auth-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { ArrowLeftIcon, Loader2Icon, CameraIcon } from "lucide-react";
import { toast } from "sonner";

// 获取头像 URL
const getAvatarUrl = (avatarBase64?: string, name?: string) => {
    if (!avatarBase64 || avatarBase64 === "") {
        const seed = encodeURIComponent(name || "user");
        return `https://api.dicebear.com/7.x/pixel-art/svg?seed=${seed}`;
    }
    if (avatarBase64.startsWith("data:")) {
        return avatarBase64;
    }
    return `data:image/png;base64,${avatarBase64}`;
};

export default function ProfilePage() {
    const navigate = useNavigate();
    const { data: user, isLoading, refetch } = useCurrentUser();
    const updateUser = useUpdateUser();
    const logout = useAuthStore((state) => state.logout);

    const [name, setName] = useState("");
    const [bio, setBio] = useState("");
    const [avatarPreview, setAvatarPreview] = useState("");
    const [avatarBase64, setAvatarBase64] = useState("");
    const fileInputRef = useRef<HTMLInputElement>(null);

    // 当用户数据加载完成后初始化表单
    useState(() => {
        if (user) {
            setName(user.name || "");
            setBio(user.bio || "");
            setAvatarPreview(getAvatarUrl(user.avatarBase64, user.name));
        }
    });

    const handleAvatarClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // 检查文件类型
        if (!file.type.startsWith("image/")) {
            toast.error("请选择图片文件");
            return;
        }

        // 检查文件大小 (最大 2MB)
        if (file.size > 2 * 1024 * 1024) {
            toast.error("图片大小不能超过 2MB");
            return;
        }

        // 转换为 base64
        const reader = new FileReader();
        reader.onload = (event) => {
            const result = event.target?.result as string;
            setAvatarPreview(result);
            setAvatarBase64(result);
        };
        reader.readAsDataURL(file);
    };

    const handleSave = async () => {
        try {
            await updateUser.mutateAsync({
                name,
                bio,
                avatarBase64: avatarBase64 || undefined,
            });
            toast.success("保存成功");
            refetch();
        } catch (error) {
            // Error handled by hook
        }
    };

    const handleLogout = () => {
        logout();
        navigate("/login");
    };

    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <Loader2Icon className="h-8 w-8 animate-spin text-gray-400" />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50">
            {/* 头部 */}
            <div className="bg-white border-b px-4 py-3 flex items-center gap-3 sticky top-0 z-10">
                <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
                    <ArrowLeftIcon className="h-5 w-5" />
                </Button>
                <h1 className="text-lg font-semibold">个人中心</h1>
            </div>

            <div className="max-w-md mx-auto p-4 space-y-4">
                {/* 头像区域 */}
                <div className="flex flex-col items-center py-6">
                    <div className="relative">
                        <Avatar className="h-24 w-24 cursor-pointer" onClick={handleAvatarClick}>
                            <AvatarImage src={avatarPreview} alt={user?.name} />
                            <AvatarFallback className="text-2xl">
                                {user?.name?.charAt(0)?.toUpperCase() || "U"}
                            </AvatarFallback>
                        </Avatar>
                        <div className="absolute bottom-0 right-0 bg-primary text-white p-1.5 rounded-full">
                            <CameraIcon className="h-4 w-4" />
                        </div>
                    </div>
                    <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/*"
                        className="hidden"
                        onChange={handleFileChange}
                    />
                    <p className="mt-2 text-sm text-gray-500">点击上传头像</p>
                </div>

                {/* 用户信息 */}
                <Card>
                    <CardHeader>
                        <CardTitle className="text-base">账户信息</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div>
                            <p className="text-sm text-gray-500 mb-1">用户名</p>
                            <Input
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="请输入用户名"
                            />
                        </div>
                        <div>
                            <p className="text-sm text-gray-500 mb-1">邮箱</p>
                            <Input value={user?.email || ""} disabled className="bg-gray-50" />
                        </div>
                        <div>
                            <p className="text-sm text-gray-500 mb-1">简介</p>
                            <Textarea
                                value={bio}
                                onChange={(e) => setBio(e.target.value)}
                                placeholder="请输入简介"
                                rows={3}
                            />
                        </div>

                        <Button
                            className="w-full"
                            onClick={handleSave}
                            disabled={updateUser.isPending}
                        >
                            {updateUser.isPending ? (
                                <>
                                    <Loader2Icon className="mr-2 h-4 w-4 animate-spin" />
                                    保存中...
                                </>
                            ) : (
                                "保存修改"
                            )}
                        </Button>
                    </CardContent>
                </Card>

                {/* 退出登录 */}
                <Button
                    variant="outline"
                    className="w-full text-red-600 hover:text-red-600 hover:bg-red-50"
                    onClick={handleLogout}
                >
                    退出登录
                </Button>
            </div>
        </div>
    );
}
```

- [ ] **Step 2: 检查 Avatar 组件是否存在**

查看 `apps/web/src/components/ui/avatar.tsx` 是否存在，如不存在需要创建

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/profile.tsx
git commit -m "feat(web): create profile page"
```

---

## Chunk 2: 添加路由和侧边栏跳转

### Task 2: 添加路由

**Files:**
- Modify: `apps/web/src/router.ts`

- [ ] **Step 1: 添加 ProfilePage 导入和路由**

在 router.ts 中添加:
```typescript
const ProfilePage = lazy(() => import("@/pages/profile"));

// 在路由数组中添加:
{
    path: "/profile",
    Component: StandaloneLayout,
    ErrorBoundary: ErrorPage,
    children: [
        { index: true, Component: ProfilePage },
    ],
},
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/router.ts
git commit -m "feat(web): add /profile route"
```

---

### Task 3: 修改侧边栏设置按钮

**Files:**
- Modify: `apps/web/src/components/sidebar/index.tsx`

- [ ] **Step 1: 添加 useNavigate 导入**

```typescript
import { useNavigate } from "react-router"
```

- [ ] **Step 2: 在组件中添加 navigate 函数**

```typescript
const navigate = useNavigate()
```

- [ ] **Step 3: 修改设置按钮添加点击事件**

```typescript
<button
    className="h-8 w-8 p-0 flex items-center justify-center flex-shrink-0"
    onClick={() => navigate("/profile")}
>
    <SettingsIcon className="h-5 w-5 text-gray-600" />
</button>
```

- [ ] **Step 4: 提交**

```bash
git add apps/web/src/components/sidebar/index.tsx
git commit -m "feat(web): add navigation to profile page from sidebar"
```

---

## 验收检查清单

- [ ] 点击侧边栏设置按钮跳转到 /profile
- [ ] 页面显示用户头像、姓名、邮箱、简介
- [ ] 可以修改姓名和简介
- [ ] 可以上传头像（图片转 base64）
- [ ] 点击保存后信息更新成功
- [ ] 点击退出登录后跳转登录页
- [ ] 响应式布局正常