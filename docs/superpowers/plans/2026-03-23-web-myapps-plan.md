# Web 端"我的元应用"页面实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Web 端创建"我的元应用"页面，参考华为 HarmonyOS 版本的 MyAppsScreen

**Architecture:** 使用 React + TypeScript + Tailwind CSS，复用现有的 shadcn/ui 组件和 API 接口

**Tech Stack:** React, TypeScript, Tailwind CSS, React Router, React Query

---

## 文件结构

```
apps/web/src/
├── api/
│   └── app.ts              # 应用 API 接口（新建）
├── types/
│   └── app.ts              # 应用类型定义（新建）
├── components/
│   └── app/
│       ├── AppItem.tsx     # 应用卡片组件（新建）
│       └── AppActionMenu.tsx  # 操作菜单（新建）
├── hooks/
│   └── useApps.ts          # 应用相关 hooks（新建）
└── pages/
    └── myapps.tsx          # 我的元应用页面（新建）
```

---

## Chunk 1: 类型定义和 API 接口

### Task 1: 创建应用类型定义

**Files:**
- Create: `apps/web/src/types/app.ts`

- [ ] **Step 1: 创建类型定义文件**

```typescript
// apps/web/src/types/app.ts

export interface AppVersion {
  id: number;
  versionNumber: number;
  storagePath: string;
  changeLog: string;
  createdAt: string;
}

export interface App {
  id: number;
  uuid: string;
  name: string;
  description: string;
  logo?: string;
  isPublic: boolean;
  currentVersionId: number | null;
  versions: AppVersion[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateAppRequest {
  name: string;
  description?: string;
}
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/types/app.ts
git commit -m "feat(web): add app types"
```

---

### Task 2: 创建应用 API 接口

**Files:**
- Create: `apps/web/src/api/app.ts`

- [ ] **Step 1: 创建 API 接口文件**

```typescript
// apps/web/src/api/app.ts
import http, { type ApiResponse } from "@/lib/http";
import type { App, CreateAppRequest } from "@/types/app";

/**
 * 获取用户所有应用
 */
export const getUserApps = async (): Promise<App[]> => {
  const response = await http.get("apps").json<ApiResponse<App[]>>();
  if (!response.data) {
    throw new Error(response.message || "Failed to fetch apps");
  }
  return response.data;
};

/**
 * 根据 UUID 获取应用
 */
export const getAppByUuid = async (uuid: string): Promise<App> => {
  const response = await http.get(`apps/uuid/${uuid}`).json<ApiResponse<App>>();
  if (!response.data) {
    throw new Error(response.message || "Failed to fetch app");
  }
  return response.data;
};

/**
 * 创建新应用
 */
export const createApp = async (data: CreateAppRequest): Promise<App> => {
  const response = await http.post("apps", { json: data }).json<ApiResponse<App>>();
  if (!response.data) {
    throw new Error(response.message || "Failed to create app");
  }
  return response.data;
};

/**
 * 更新应用
 */
export const updateApp = async (appId: number, data: Partial<CreateAppRequest>): Promise<App> => {
  const response = await http.patch(`apps/${appId}`, { json: data }).json<ApiResponse<App>>();
  if (!response.data) {
    throw new Error(response.message || "Failed to update app");
  }
  return response.data;
};

/**
 * 删除应用
 */
export const deleteApp = async (appId: number): Promise<void> => {
  const response = await http.delete(`apps/${appId}`).json<ApiResponse<void>>();
  if (!response.data && response.message) {
    throw new Error(response.message || "Failed to delete app");
  }
};

/**
 * 获取应用版本列表
 */
export const getAppVersions = async (appId: number) => {
  const response = await http.get(`apps/${appId}/versions`).json<ApiResponse<any>>();
  return response.data || [];
};
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/api/app.ts
git commit -m "feat(web): add app API endpoints"
```

---

## Chunk 2: Hooks

### Task 3: 创建应用相关 Hooks

**Files:**
- Create: `apps/web/src/hooks/useApps.ts`

- [ ] **Step 1: 创建 useApps hook**

```typescript
// apps/web/src/hooks/useApps.ts
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getUserApps, createApp, deleteApp, type CreateAppRequest } from "@/api/app";
import type { App } from "@/types/app";
import { toast } from "sonner";

/**
 * 获取用户应用列表
 */
export function useUserApps() {
  return useQuery({
    queryKey: ["user-apps"],
    queryFn: getUserApps,
  });
}

/**
 * 创建应用
 */
export function useCreateApp() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateAppRequest) => createApp(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["user-apps"] });
      toast.success("应用创建成功");
    },
    onError: (error: Error) => {
      toast.error(error.message || "创建应用失败");
    },
  });
}

/**
 * 删除应用
 */
export function useDeleteApp() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (appId: number) => deleteApp(appId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["user-apps"] });
      toast.success("应用删除成功");
    },
    onError: (error: Error) => {
      toast.error(error.message || "删除应用失败");
    },
  });
}
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/hooks/useApps.ts
git commit -m "feat(web): add useApps hooks"
```

---

## Chunk 3: 组件

### Task 4: 创建 AppActionMenu 组件

**Files:**
- Create: `apps/web/src/components/app/AppActionMenu.tsx`

- [ ] **Step 1: 创建操作菜单组件**

```typescript
// apps/web/src/components/app/AppActionMenu.tsx
import { useState } from "react";
import { MoreVerticalIcon, EyeIcon, Trash2Icon } from "lucide-react";
import { useNavigate } from "react-router";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useDeleteApp } from "@/hooks/useApps";
import type { App } from "@/types/app";
import { API_BASE_URL } from "@/lib/config";

interface AppActionMenuProps {
  app: App;
  onRefresh?: () => void;
}

export function AppActionMenu({ app, onRefresh }: AppActionMenuProps) {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const deleteApp = useDeleteApp();

  const handlePreview = () => {
    navigate(`/preview?url=${encodeURIComponent(`${API_BASE_URL}/api/preview/${app.uuid}`)}&appName=${encodeURIComponent(app.name)}`);
  };

  const handleDelete = async () => {
    if (confirm(`确定要删除 "${app.name}" 吗？此操作不可撤销。`)) {
      await deleteApp.mutateAsync(app.id);
      onRefresh?.();
    }
  };

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="h-8 w-8">
          <MoreVerticalIcon className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={handlePreview}>
          <EyeIcon className="mr-2 h-4 w-4" />
          预览
        </DropdownMenuItem>
        <DropdownMenuItem onClick={handleDelete} className="text-red-600">
          <Trash2Icon className="mr-2 h-4 w-4" />
          删除
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
```

- [ ] **Step 2: 检查是否需要创建 DropdownMenu 组件**

如果 `apps/web/src/components/ui/dropdown-menu.tsx` 不存在，需要先创建。检查：

```bash
ls apps/web/src/components/ui/dropdown-menu.tsx
```

如果不存在，使用 shadcn 添加：
```bash
cd apps/web && npx shadcn@latest add dropdown-menu -y
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/components/app/AppActionMenu.tsx
git commit -m "feat(web): add AppActionMenu component"
```

---

### Task 5: 创建 AppItem 组件

**Files:**
- Create: `apps/web/src/components/app/AppItem.tsx`

- [ ] **Step 1: 创建应用卡片组件**

```typescript
// apps/web/src/components/app/AppItem.tsx
import { useState } from "react";
import { useNavigate } from "react-router";
import type { App } from "@/types/app";
import { API_BASE_URL } from "@/lib/config";
import { AppActionMenu } from "./AppActionMenu";

interface AppItemProps {
  app: App;
  onRefresh?: () => void;
}

// 根据名称生成渐变背景色
function getLogoColors(name: string): string[] {
  const colorPairs: string[][] = [
    ["#007AFF", "#5856D6"],
    ["#5856D6", "#AF52DE"],
    ["#AF52DE", "#FF2D55"],
    ["#FF2D55", "#FF3B30"],
    ["#FF3B30", "#FF9500"],
    ["#FF9500", "#FFCC00"],
    ["#FFCC00", "#34C759"],
    ["#34C759", "#30B0C7"],
    ["#30B0C7", "#8E8E93"],
    ["#8E8E93", "#007AFF"],
  ];

  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colorPairs[Math.abs(hash) % colorPairs.length];
}

function getLogoInitial(name: string): string {
  return name && name.length > 0 ? name.charAt(0).toUpperCase() : "A";
}

// 格式化时间
function formatTime(isoString: string): string {
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) {
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    if (diffHours === 0) {
      const diffMinutes = Math.floor(diffMs / (1000 * 60));
      return diffMinutes <= 1 ? "刚刚" : `${diffMinutes}分钟前`;
    }
    return `${diffHours}小时前`;
  } else if (diffDays === 1) {
    return "昨天";
  } else if (diffDays < 7) {
    return `${diffDays}天前`;
  } else if (diffDays < 30) {
    return `${Math.floor(diffDays / 7)}周前`;
  } else if (diffDays < 365) {
    return `${Math.floor(diffDays / 30)}月前`;
  } else {
    return `${Math.floor(diffDays / 365)}年前`;
  }
}

function getLogoUrl(app: App): string {
  if (!app.logo || app.logo.length === 0) {
    return "";
  }

  if (app.logo.startsWith("http://") || app.logo.startsWith("https://")) {
    return app.logo;
  }

  const dotIndex = app.logo.lastIndexOf(".");
  const logoUuid = dotIndex > 0 ? app.logo.substring(0, dotIndex) : app.logo;
  return `${API_BASE_URL}/api/logo/${logoUuid}`;
}

export function AppItem({ app, onRefresh }: AppItemProps) {
  const navigate = useNavigate();
  const [logoLoadFailed, setLogoLoadFailed] = useState(false);

  const logoUrl = getLogoUrl(app);
  const colors = getLogoColors(app.name);

  const handleClick = () => {
    const previewUrl = `${API_BASE_URL}/api/preview/${app.uuid}`;
    navigate(`/preview?url=${encodeURIComponent(previewUrl)}&appName=${encodeURIComponent(app.name)}`);
  };

  return (
    <div
      className="flex items-center gap-4 rounded-2xl bg-white p-4 transition-colors hover:bg-gray-50 cursor-pointer"
      onClick={handleClick}
    >
      {/* Logo */}
      <div className="flex-shrink-0">
        {!logoLoadFailed && logoUrl ? (
          <img
            src={logoUrl}
            alt={app.name}
            className="h-14 w-14 rounded-[14px] object-cover"
            onError={() => setLogoLoadFailed(true)}
          />
        ) : (
          <div
            className="flex h-14 w-14 items-center justify-center rounded-[14px]"
            style={{
              background: `linear-gradient(135deg, ${colors[0]}, ${colors[1]})`,
            }}
          >
            <span className="text-2xl font-bold text-white">{getLogoInitial(app.name)}</span>
          </div>
        )}
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1">
          <h3 className="text-[17px] font-medium text-gray-900 truncate">{app.name}</h3>
          {app.versions && app.versions.length > 0 && (
            <span className="text-[13px] text-[#007AFF]">v{app.versions[0].versionNumber}</span>
          )}
        </div>
        <p className="text-sm text-gray-500 truncate mt-0.5">
          {app.description || "暂无描述"}
        </p>
        <div className="text-xs text-gray-400 mt-1.5 flex items-center gap-1">
          <span>创建于 {formatTime(app.createdAt)}</span>
          {app.updatedAt !== app.createdAt && (
            <>
              <span> · </span>
              <span>更新 {formatTime(app.updatedAt)}</span>
            </>
          )}
          {app.versions && app.versions.length > 1 && (
            <>
              <span> · </span>
              <span>{app.versions.length}个版本</span>
            </>
          )}
        </div>
      </div>

      {/* Action Menu */}
      <div onClick={(e) => e.stopPropagation()}>
        <AppActionMenu app={app} onRefresh={onRefresh} />
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/components/app/AppItem.tsx
git commit -m "feat(web): add AppItem component"
```

---

## Chunk 4: 页面和路由

### Task 6: 创建 MyApps 页面

**Files:**
- Create: `apps/web/src/pages/myapps.tsx`

- [ ] **Step 1: 创建我的元应用页面**

```typescript
// apps/web/src/pages/myapps.tsx
import { useState } from "react";
import { useNavigate } from "react-router";
import { ArrowLeftIcon, PlusIcon, SmartphoneIcon } from "lucide-react";
import { useUserApps, useCreateApp } from "@/hooks/useApps";
import { AppItem } from "@/components/app/AppItem";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { toast } from "sonner";

export default function MyAppsPage() {
  const navigate = useNavigate();
  const { data: apps = [], isLoading, refetch } = useUserApps();
  const createApp = useCreateApp();

  const [showNewAppDialog, setShowNewAppDialog] = useState(false);
  const [newAppName, setNewAppName] = useState("");
  const [newAppDescription, setNewAppDescription] = useState("");

  const handleCreateApp = async () => {
    if (!newAppName.trim()) {
      toast.error("请输入应用名称");
      return;
    }

    try {
      await createApp.mutateAsync({
        name: newAppName.trim(),
        description: newAppDescription.trim() || undefined,
      });
      setShowNewAppDialog(false);
      setNewAppName("");
      setNewAppDescription("");
      refetch();
    } catch (error) {
      console.error("Failed to create app:", error);
    }
  };

  return (
    <div className="flex flex-col h-full bg-[#F5F7FA]">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 bg-white border-b">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
          >
            <ArrowLeftIcon className="h-5 w-5" />
          </Button>
          <h1 className="text-lg font-medium">我的元应用</h1>
        </div>

        <Dialog open={showNewAppDialog} onOpenChange={setShowNewAppDialog}>
          <DialogTrigger asChild>
            <Button size="icon" className="h-9 w-9 bg-[#007AFF] hover:bg-[#0056CC]">
              <PlusIcon className="h-5 w-5" />
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>新建应用</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Input
                  placeholder="应用名称"
                  value={newAppName}
                  onChange={(e) => setNewAppName(e.target.value)}
                />
              </div>
              <div>
                <Textarea
                  placeholder="应用描述（可选）"
                  value={newAppDescription}
                  onChange={(e) => setNewAppDescription(e.target.value)}
                  className="h-20"
                />
              </div>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  className="flex-1"
                  onClick={() => setShowNewAppDialog(false)}
                >
                  取消
                </Button>
                <Button
                  className="flex-1 bg-[#007AFF]"
                  onClick={handleCreateApp}
                  disabled={createApp.isPending}
                >
                  {createApp.isPending ? "创建中..." : "创建"}
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center h-full gap-3">
            <div className="h-10 w-10 animate-spin rounded-full border-4 border-[#007AFF] border-t-transparent" />
            <span className="text-sm text-gray-500">加载中...</span>
          </div>
        ) : apps.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center">
            <SmartphoneIcon className="h-12 w-12 text-gray-300 mb-4" />
            <h3 className="text-lg font-medium text-gray-900">还没有应用</h3>
            <p className="text-sm text-gray-500 mt-1">通过对话创建你的第一个元应用吧</p>
            <Button
              className="mt-4 bg-[#007AFF] hover:bg-[#0056CC]"
              onClick={() => navigate("/")}
            >
              去创建
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            {apps.map((app) => (
              <AppItem
                key={app.id}
                app={app}
                onRefresh={() => refetch()}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: 需要安装额外组件**

检查并安装需要的 UI 组件：
```bash
cd apps/web
npx shadcn@latest add dialog textarea -y
```

- [ ] **Step 3: 提交**

```bash
git add apps/web/src/pages/myapps.tsx
git commit -m "feat(web): add MyApps page"
```

---

### Task 7: 添加路由

**Files:**
- Modify: `apps/web/src/router.ts`

- [ ] **Step 1: 添加路由**

在 router.ts 中添加 MyApps 页面路由：

```typescript
const MyAppsPage = lazy(() => import("@/pages/myapps"));

// 在 children 数组中添加
{ path: "myapps", Component: MyAppsPage },
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/router.ts
git commit -m "feat(web): add myapps route"
```

---

## Chunk 5: 侧边栏导航

### Task 8: 更新侧边栏导航

**Files:**
- Modify: `apps/web/src/components/sidebar/sidebar-content.tsx`

- [ ] **Step 1: 更新导航项**

将"我的元应用"导航项改为可点击，指向 `/myapps` 路由：

```typescript
// 在 navItems 中，修改 AppWindowIcon 行的 onClick
{
  icon: AppWindowIcon,
  label: "我的元应用",
  onClick: () => navigate("/myapps"),
}
```

- [ ] **Step 2: 提交**

```bash
git add apps/web/src/components/sidebar/sidebar-content.tsx
git commit -m "feat(web): add myapps navigation in sidebar"
```

---

## 完成

所有任务完成后，提交最终更改并测试页面。

```bash
git status
git log --oneline
```

测试步骤：
1. 启动后端服务 `cd apps/api && ./mvnw spring-boot:run`
2. 启动前端服务 `cd apps/web && bun run dev`
3. 登录后访问 `/myapps` 页面
4. 验证加载状态、空状态、应用列表、创建对话框等功能