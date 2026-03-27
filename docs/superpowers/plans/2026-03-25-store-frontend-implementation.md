# 元应用商店前端实现计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 MetaCraft 添加"元应用商店"前端页面，支持浏览、评分、评论已发布应用

**Architecture:** 创建商店 API、Hooks、组件和页面，使用 React Router 管理路由，复用现有 StandaloneLayout

**Tech Stack:** React, TypeScript, shadcn/ui, React Query, Lucide Icons

---

## 文件结构

```
apps/web/src/
├── api/
│   └── store.ts                    # 新增: 商店 API 接口
├── hooks/
│   └── useStore.ts                 # 新增: 商店 Hooks
├── pages/
│   ├── store.tsx                   # 新增: 商店首页
│   └── store-detail.tsx            # 新增: 应用详情页
├── components/
│   └── store/
│       ├── StoreAppCard.tsx        # 新增: 应用卡片组件
│       ├── StarRating.tsx         # 新增: 评分组件
│       └── CommentList.tsx        # 新增: 评论列表组件
```

---

## Chunk 1: API 和类型定义

### Task 1: 创建商店 API

**Files:**
- Create: `apps/web/src/api/store.ts`

- [ ] **Step 1: 创建 API 文件**

```typescript
import http, { type ApiResponse } from "@/lib/http";

/**
 * 商店应用列表项
 */
export interface StoreAppItem {
    id: number;
    uuid: string;
    name: string;
    description: string;
    logo: string | null;
    averageRating: number | null;
    ratingCount: number;
    author: {
        id: number;
        name: string;
        avatarBase64: string | null;
    };
    createdAt: string;
}

/**
 * 评论项
 */
export interface StoreComment {
    id: number;
    userId: number;
    userName: string;
    userAvatar: string | null;
    content: string;
    createdAt: string;
}

/**
 * 商店应用详情
 */
export interface StoreAppDetail extends StoreAppItem {
    comments: StoreComment[];
}

/**
 * 评分结果
 */
export interface RatingResult {
    averageRating: number;
    ratingCount: number;
}

/**
 * 获取商店应用列表
 */
export const getStoreApps = async (): Promise<StoreAppItem[]> => {
    const response = await http.get("store/apps").json<ApiResponse<StoreAppItem[]>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch store apps");
    }
    return response.data;
};

/**
 * 获取应用详情
 */
export const getStoreAppDetail = async (appId: number): Promise<StoreAppDetail> => {
    const response = await http.get(`store/apps/${appId}`).json<ApiResponse<StoreAppDetail>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to fetch app detail");
    }
    return response.data;
};

/**
 * 提交评分
 */
export const rateApp = async (appId: number, rating: number): Promise<RatingResult> => {
    const response = await http
        .post(`store/apps/${appId}/ratings`, { json: { rating } })
        .json<ApiResponse<RatingResult>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to submit rating");
    }
    return response.data;
};

/**
 * 提交评论
 */
export const commentApp = async (appId: number, content: string): Promise<StoreComment> => {
    const response = await http
        .post(`store/apps/${appId}/comments`, { json: { content } })
        .json<ApiResponse<StoreComment>>();
    if (!response.data) {
        throw new Error(response.message || "Failed to submit comment");
    }
    return response.data;
};

/**
 * 删除评论
 */
export const deleteComment = async (appId: number, commentId: number): Promise<void> => {
    const response = await http
        .delete(`store/apps/${appId}/comments/${commentId}`)
        .json<ApiResponse<void>>();
    if (response.message && !response.data) {
        throw new Error(response.message);
    }
};
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/api/store.ts
git commit -m "feat(web): add store API functions"
```

---

## Chunk 2: Hooks

### Task 2: 创建商店 Hooks

**Files:**
- Create: `apps/web/src/hooks/useStore.ts`

- [ ] **Step 1: 创建 Hooks 文件**

```typescript
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
    getStoreApps,
    getStoreAppDetail,
    rateApp,
    commentApp,
    deleteComment,
    type StoreAppItem,
    type StoreAppDetail,
    type RatingResult,
    type StoreComment,
} from "@/api/store";

/**
 * 获取商店应用列表
 */
export function useStoreApps() {
    return useQuery<StoreAppItem[]>({
        queryKey: ["store", "apps"],
        queryFn: getStoreApps,
    });
}

/**
 * 获取应用详情
 */
export function useStoreAppDetail(appId: number) {
    return useQuery<StoreAppDetail>({
        queryKey: ["store", "app", appId],
        queryFn: () => getStoreAppDetail(appId),
        enabled: !!appId,
    });
}

/**
 * 提交评分
 */
export function useRateApp(appId: number) {
    const queryClient = useQueryClient();

    return useMutation<RatingResult, Error, number>({
        mutationFn: (rating: number) => rateApp(appId, rating),
        onSuccess: () => {
            // 刷新应用详情
            queryClient.invalidateQueries({ queryKey: ["store", "app", appId] });
        },
    });
}

/**
 * 提交评论
 */
export function useCommentApp(appId: number) {
    const queryClient = useQueryClient();

    return useMutation<StoreComment, Error, string>({
        mutationFn: (content: string) => commentApp(appId, content),
        onSuccess: () => {
            // 刷新应用详情
            queryClient.invalidateQueries({ queryKey: ["store", "app", appId] });
        },
    });
}

/**
 * 删除评论
 */
export function useDeleteComment(appId: number) {
    const queryClient = useQueryClient();

    return useMutation<void, Error, number>({
        mutationFn: (commentId: number) => deleteComment(appId, commentId),
        onSuccess: () => {
            // 刷新应用详情
            queryClient.invalidateQueries({ queryKey: ["store", "app", appId] });
        },
    });
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/hooks/useStore.ts
git commit -m "feat(web): add store hooks"
```

---

## Chunk 3: 组件

### Task 3: 创建 StoreAppCard 组件

**Files:**
- Create: `apps/web/src/components/store/StoreAppCard.tsx`

- [ ] **Step 1: 创建应用卡片组件**

```tsx
import { useNavigate } from "react-router";
import { StoreAppItem } from "@/api/store";
import { StarIcon } from "lucide-react";

interface StoreAppCardProps {
    app: StoreAppItem;
}

export function StoreAppCard({ app }: StoreAppCardProps) {
    const navigate = useNavigate();

    // 处理头像
    const getAvatarUrl = (avatarBase64: string | null) => {
        if (!avatarBase64 || avatarBase64 === "") {
            return undefined;
        }
        if (avatarBase64.startsWith("data:")) {
            return avatarBase64;
        }
        return `data:image/png;base64,${avatarBase64}`;
    };

    // 处理 Logo
    const getLogoUrl = (logo: string | null) => {
        if (!logo) return null;
        if (logo.startsWith("data:")) return logo;
        return `data:image/png;base64,${logo}`;
    };

    const handleClick = () => {
        navigate(`/store/${app.id}`);
    };

    const authorAvatar = getAvatarUrl(app.author?.avatarBase64);
    const logoUrl = getLogoUrl(app.logo);

    return (
        <div
            className="cursor-pointer rounded-xl border border-gray-200 bg-white p-3 transition-shadow hover:shadow-md"
            onClick={handleClick}
        >
            {/* Logo */}
            <div className="flex h-16 items-center justify-center rounded-lg bg-gray-100 mb-2">
                {logoUrl ? (
                    <img
                        src={logoUrl}
                        alt={app.name}
                        className="h-14 w-14 object-contain"
                    />
                ) : (
                    <span className="text-2xl">📱</span>
                )}
            </div>

            {/* 名称 */}
            <h3 className="mb-1 line-clamp-2 text-sm font-medium text-gray-900">
                {app.name}
            </h3>

            {/* 评分 */}
            <div className="mb-2 flex items-center gap-1">
                <StarIcon className="h-3.5 w-3.5 fill-yellow-400 text-yellow-400" />
                <span className="text-xs font-medium text-gray-700">
                    {app.averageRating?.toFixed(1) || "0.0"}
                </span>
                <span className="text-xs text-gray-500">
                    ({app.ratingCount})
                </span>
            </div>

            {/* 作者 */}
            <div className="flex items-center gap-2">
                {authorAvatar ? (
                    <img
                        src={authorAvatar}
                        alt={app.author?.name}
                        className="h-5 w-5 rounded-full object-cover"
                    />
                ) : (
                    <div className="h-5 w-5 rounded-full bg-gray-200" />
                )}
                <span className="truncate text-xs text-gray-500">
                    {app.author?.name || "未知"}
                </span>
            </div>
        </div>
    );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/components/store/StoreAppCard.tsx
git commit -m "feat(web): add StoreAppCard component"
```

### Task 4: 创建 StarRating 组件

**Files:**
- Create: `apps/web/src/components/store/StarRating.tsx`

- [ ] **Step 1: 创建评分组件**

```tsx
import { useState } from "react";
import { StarIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Loader2Icon } from "lucide-react";

interface StarRatingProps {
    initialRating?: number;
    onRate: (rating: number) => Promise<void>;
    disabled?: boolean;
}

export function StarRating({ initialRating = 0, onRate, disabled = false }: StarRatingProps) {
    const [rating, setRating] = useState(initialRating);
    const [hoverRating, setHoverRating] = useState(0);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [hasRated, setHasRated] = useState(initialRating > 0);

    const handleClick = (value: number) => {
        if (disabled || isSubmitting) return;
        setRating(value);
    };

    const handleSubmit = async () => {
        if (rating === 0 || isSubmitting) return;

        setIsSubmitting(true);
        try {
            await onRate(rating);
            setHasRated(true);
        } catch (error) {
            console.error("Failed to submit rating:", error);
        } finally {
            setIsSubmitting(false);
        }
    };

    const displayRating = hoverRating || rating;

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center gap-1">
                {[1, 2, 3, 4, 5].map((value) => (
                    <button
                        key={value}
                        type="button"
                        disabled={disabled || isSubmitting}
                        className="p-0.5 transition-transform hover:scale-110 disabled:pointer-events-none"
                        onClick={() => handleClick(value)}
                        onMouseEnter={() => !disabled && setHoverRating(value)}
                        onMouseLeave={() => setHoverRating(0)}
                    >
                        <StarIcon
                            className={`h-6 w-6 ${
                                value <= displayRating
                                    ? "fill-yellow-400 text-yellow-400"
                                    : "fill-gray-200 text-gray-200"
                            }`}
                        />
                    </button>
                ))}
                {rating > 0 && !hasRated && (
                    <Button
                        size="sm"
                        onClick={handleSubmit}
                        disabled={isSubmitting}
                        className="ml-2 bg-[#007AFF] text-white hover:bg-[#007AFF]/90"
                    >
                        {isSubmitting ? (
                            <Loader2Icon className="h-4 w-4 animate-spin" />
                        ) : (
                            "确认"
                        )}
                    </Button>
                )}
                {hasRated && (
                    <span className="ml-2 text-sm text-green-600">评分成功</span>
                )}
            </div>
            {disabled && (
                <span className="text-xs text-gray-500">登录后可评分</span>
            )}
        </div>
    );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/components/store/StarRating.tsx
git commit -m "feat(web): add StarRating component"
```

### Task 5: 创建 CommentList 组件

**Files:**
- Create: `apps/web/src/components/store/CommentList.tsx`

- [ ] **Step 1: 创建评论列表组件**

```tsx
import { StoreComment } from "@/api/store";
import { StarIcon, Trash2Icon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/auth-store";

interface CommentListProps {
    comments: StoreComment[];
    onDelete: (commentId: number) => Promise<void>;
    isDeleting: boolean;
}

export function CommentList({ comments, onDelete, isDeleting }: CommentListProps) {
    const currentUserId = useAuthStore((state) => state.user?.id);

    // 处理头像
    const getAvatarUrl = (avatarBase64: string | null) => {
        if (!avatarBase64 || avatarBase64 === "") {
            return undefined;
        }
        if (avatarBase64.startsWith("data:")) {
            return avatarBase64;
        }
        return `data:image/png;base64,${avatarBase64}`;
    };

    // 格式化时间
    const formatTime = (dateString: string) => {
        const date = new Date(dateString);
        const now = new Date();
        const diff = now.getTime() - date.getTime();
        const days = Math.floor(diff / (1000 * 60 * 60 * 24));

        if (days === 0) return "今天";
        if (days === 1) return "昨天";
        if (days < 7) return `${days}天前`;
        return date.toLocaleDateString("zh-CN");
    };

    if (comments.length === 0) {
        return (
            <div className="py-8 text-center text-sm text-gray-500">
                暂无评论，快来抢先评论吧
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {comments.map((comment) => {
                const avatarUrl = getAvatarUrl(comment.userAvatar);
                const isOwn = currentUserId === comment.userId;

                return (
                    <div key={comment.id} className="border-b border-gray-100 pb-4 last:border-0">
                        <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                {avatarUrl ? (
                                    <img
                                        src={avatarUrl}
                                        alt={comment.userName}
                                        className="h-8 w-8 rounded-full object-cover"
                                    />
                                ) : (
                                    <div className="h-8 w-8 rounded-full bg-gray-200" />
                                )}
                                <span className="text-sm font-medium text-gray-900">
                                    {comment.userName}
                                </span>
                            </div>
                            {isOwn && (
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => onDelete(comment.id)}
                                    disabled={isDeleting}
                                    className="text-red-500 hover:text-red-600"
                                >
                                    <Trash2Icon className="h-4 w-4" />
                                </Button>
                            )}
                        </div>
                        <p className="mt-2 text-sm text-gray-700">{comment.content}</p>
                        <span className="mt-1 block text-xs text-gray-400">
                            {formatTime(comment.createdAt)}
                        </span>
                    </div>
                );
            })}
        </div>
    );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/components/store/CommentList.tsx
git commit -m "feat(web): add CommentList component"
```

---

## Chunk 4: 页面

### Task 6: 创建商店首页

**Files:**
- Create: `apps/web/src/pages/store.tsx`

- [ ] **Step 1: 创建商店首页**

```tsx
import { useNavigate } from "react-router";
import { ArrowLeftIcon, Loader2Icon, SearchIcon } from "lucide-react";
import { useStoreApps } from "@/hooks/useStore";
import { StoreAppCard } from "@/components/store/StoreAppCard";
import { Button } from "@/components/ui/button";

export default function StorePage() {
    const navigate = useNavigate();
    const { data: apps, isLoading, error } = useStoreApps();

    return (
        <div className="flex h-full flex-col bg-[#F5F7FA]">
            {/* Header */}
            <div className="flex h-14 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4">
                <button
                    onClick={() => navigate(-1)}
                    className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
                >
                    <ArrowLeftIcon className="h-5 w-5 text-gray-900" />
                </button>
                <h1 className="text-base font-medium text-gray-900">元应用商店</h1>
                <div className="w-8" />
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto px-4 py-4">
                {/* Loading */}
                {isLoading && (
                    <div className="flex flex-col items-center justify-center gap-3 py-20">
                        <Loader2Icon className="h-8 w-8 animate-spin text-[#007AFF]" />
                        <span className="text-sm text-gray-500">加载中...</span>
                    </div>
                )}

                {/* Error */}
                {error && (
                    <div className="flex flex-col items-center justify-center gap-3 py-20">
                        <span className="text-sm text-red-500">加载失败，请稍后重试</span>
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => window.location.reload()}
                        >
                            重试
                        </Button>
                    </div>
                )}

                {/* Empty */}
                {!isLoading && !error && apps && apps.length === 0 && (
                    <div className="flex flex-col items-center justify-center gap-4 py-20">
                        <SearchIcon className="h-12 w-12 text-gray-300" />
                        <div className="text-center">
                            <p className="text-base font-medium text-gray-900">
                                暂无发布的应用
                            </p>
                            <p className="mt-1 text-sm text-gray-500">
                                快去创建你的第一个元应用吧
                            </p>
                        </div>
                    </div>
                )}

                {/* App Grid */}
                {!isLoading && !error && apps && apps.length > 0 && (
                    <div className="grid grid-cols-2 gap-3">
                        {apps.map((app) => (
                            <StoreAppCard key={app.id} app={app} />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/pages/store.tsx
git commit -m "feat(web): add store page"
```

### Task 7: 创建应用详情页

**Files:**
- Create: `apps/web/src/pages/store-detail.tsx`

- [ ] **Step 1: 创建详情页**

```tsx
import { useState } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeftIcon, Loader2Icon, StarIcon, SendIcon, ShareIcon } from "lucide-react";
import { useStoreAppDetail, useRateApp, useCommentApp, useDeleteComment } from "@/hooks/useStore";
import { StarRating } from "@/components/store/StarRating";
import { CommentList } from "@/components/store/CommentList";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { useAuthStore } from "@/stores/auth-store";
import { API_BASE_URL } from "@/lib/config";

export default function StoreDetailPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const appId = Number(id);

    const isAuthenticated = useAuthStore((state) => !!state.user);

    const { data: app, isLoading, error } = useStoreAppDetail(appId);
    const rateApp = useRateApp(appId);
    const commentApp = useCommentApp(appId);
    const deleteComment = useDeleteComment(appId);

    const [commentContent, setCommentContent] = useState("");

    // 处理头像
    const getAvatarUrl = (avatarBase64: string | null) => {
        if (!avatarBase64 || avatarBase64 === "") {
            return undefined;
        }
        if (avatarBase64.startsWith("data:")) {
            return avatarBase64;
        }
        return `data:image/png;base64,${avatarBase64}`;
    };

    // 处理 Logo
    const getLogoUrl = (logo: string | null) => {
        if (!logo) return null;
        if (logo.startsWith("data:")) return logo;
        return `data:image/png;base64,${logo}`;
    };

    const handleCommentSubmit = async () => {
        if (!commentContent.trim()) return;

        try {
            await commentApp.mutateAsync(commentContent.trim());
            setCommentContent("");
        } catch (error) {
            console.error("Failed to submit comment:", error);
        }
    };

    const handleDeleteComment = async (commentId: number) => {
        try {
            await deleteComment.mutateAsync(commentId);
        } catch (error) {
            console.error("Failed to delete comment:", error);
        }
    };

    const handleShare = () => {
        // 复制链接
        const url = window.location.href;
        navigator.clipboard.writeText(url);
        alert("链接已复制");
    };

    // 预览 URL
    const previewUrl = app ? `/api/preview/${app.uuid}` : null;
    const resolvedPreviewUrl = previewUrl?.startsWith("/")
        ? `${API_BASE_URL}${previewUrl}`
        : previewUrl;

    if (isLoading) {
        return (
            <div className="flex h-full items-center justify-center bg-[#F5F7FA]">
                <Loader2Icon className="h-8 w-8 animate-spin text-[#007AFF]" />
            </div>
        );
    }

    if (error || !app) {
        return (
            <div className="flex h-full flex-col bg-[#F5F7FA]">
                <div className="flex h-14 shrink-0 items-center border-b border-gray-200 bg-white px-4">
                    <button
                        onClick={() => navigate(-1)}
                        className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
                    >
                        <ArrowLeftIcon className="h-5 w-5 text-gray-900" />
                    </button>
                </div>
                <div className="flex flex-1 items-center justify-center">
                    <span className="text-sm text-red-500">加载失败</span>
                </div>
            </div>
        );
    }

    const authorAvatar = getAvatarUrl(app.author?.avatarBase64);
    const logoUrl = getLogoUrl(app.logo);

    return (
        <div className="flex h-full flex-col bg-[#F5F7FA]">
            {/* Header */}
            <div className="flex h-14 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4">
                <button
                    onClick={() => navigate(-1)}
                    className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
                >
                    <ArrowLeftIcon className="h-5 w-5 text-gray-900" />
                </button>
                <button
                    onClick={handleShare}
                    className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
                >
                    <ShareIcon className="h-5 w-5 text-gray-900" />
                </button>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto">
                {/* App Info */}
                <div className="bg-white px-4 py-4">
                    <div className="flex items-center gap-3">
                        <div className="flex h-16 w-16 items-center justify-center rounded-xl bg-gray-100">
                            {logoUrl ? (
                                <img
                                    src={logoUrl}
                                    alt={app.name}
                                    className="h-14 w-14 object-contain"
                                />
                            ) : (
                                <span className="text-2xl">📱</span>
                            )}
                        </div>
                        <div>
                            <h1 className="text-lg font-medium text-gray-900">{app.name}</h1>
                            <div className="mt-1 flex items-center gap-1">
                                <StarIcon className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                                <span className="text-sm font-medium text-gray-700">
                                    {app.averageRating?.toFixed(1) || "0.0"}
                                </span>
                                <span className="text-sm text-gray-500">
                                    ({app.ratingCount}人评分)
                                </span>
                            </div>
                            <div className="mt-1 flex items-center gap-2">
                                {authorAvatar ? (
                                    <img
                                        src={authorAvatar}
                                        alt={app.author?.name}
                                        className="h-5 w-5 rounded-full object-cover"
                                    />
                                ) : (
                                    <div className="h-5 w-5 rounded-full bg-gray-200" />
                                )}
                                <span className="text-xs text-gray-500">
                                    by {app.author?.name || "未知"}
                                </span>
                            </div>
                        </div>
                    </div>
                    {app.description && (
                        <p className="mt-3 text-sm text-gray-600">{app.description}</p>
                    )}
                </div>

                {/* Preview */}
                {resolvedPreviewUrl && (
                    <div className="mt-2 bg-white px-4 py-4">
                        <h2 className="mb-3 text-sm font-medium text-gray-900">应用预览</h2>
                        <div className="h-[300px] overflow-hidden rounded-lg border border-gray-200">
                            <iframe
                                src={resolvedPreviewUrl}
                                className="h-full w-full border-0"
                                title={`${app.name} Preview`}
                                sandbox="allow-scripts allow-same-origin allow-forms"
                            />
                        </div>
                    </div>
                )}

                {/* Rating */}
                <div className="mt-2 bg-white px-4 py-4">
                    <h2 className="mb-3 text-sm font-medium text-gray-900">我要评分</h2>
                    <StarRating
                        onRate={async (rating) => {
                            await rateApp.mutateAsync(rating);
                        }}
                        disabled={!isAuthenticated}
                    />
                </div>

                {/* Comment Input */}
                <div className="mt-2 bg-white px-4 py-4">
                    <h2 className="mb-3 text-sm font-medium text-gray-900">发表评价</h2>
                    {isAuthenticated ? (
                        <div className="space-y-3">
                            <Textarea
                                placeholder="分享你的使用体验..."
                                value={commentContent}
                                onChange={(e) => setCommentContent(e.target.value)}
                                maxLength={1000}
                                className="min-h-[80px] resize-none"
                            />
                            <div className="flex justify-end">
                                <Button
                                    onClick={handleCommentSubmit}
                                    disabled={!commentContent.trim() || commentApp.isPending}
                                    className="bg-[#007AFF] text-white hover:bg-[#007AFF]/90"
                                >
                                    {commentApp.isPending ? (
                                        <Loader2Icon className="mr-2 h-4 w-4 animate-spin" />
                                    ) : (
                                        <SendIcon className="mr-2 h-4 w-4" />
                                    )}
                                    发布
                                </Button>
                            </div>
                        </div>
                    ) : (
                        <p className="text-sm text-gray-500">登录后可发表评论</p>
                    )}
                </div>

                {/* Comments */}
                <div className="mt-2 bg-white px-4 py-4">
                    <h2 className="mb-3 text-sm font-medium text-gray-900">
                        全部评论 ({app.comments?.length || 0})
                    </h2>
                    <CommentList
                        comments={app.comments || []}
                        onDelete={handleDeleteComment}
                        isDeleting={deleteComment.isPending}
                    />
                </div>
            </div>
        </div>
    );
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/pages/store-detail.tsx
git commit -m "feat(web): add store detail page"
```

---

## Chunk 5: 路由和侧边栏

### Task 8: 更新路由

**Files:**
- Modify: `apps/web/src/router.ts`

- [ ] **Step 1: 添加商店路由**

在 router.ts 中添加:

```typescript
const StorePage = lazy(() => import("@/pages/store"));
const StoreDetailPage = lazy(() => import("@/pages/store-detail"));
```

添加路由:

```typescript
{
    path: "/store",
    Component: StandaloneLayout,
    ErrorBoundary: ErrorPage,
    children: [
        { index: true, Component: StorePage },
        { path: ":id", Component: StoreDetailPage },
    ],
},
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/router.ts
git commit -m "feat(web): add store routes"
```

### Task 9: 修复侧边栏跳转

**Files:**
- Modify: `apps/web/src/components/sidebar/sidebar-content.tsx`

- [ ] **Step 1: 添加跳转**

修改 navItems 中"元应用商店"项:

```typescript
{ icon: SearchIcon, label: "元应用商店", onClick: () => navigate("/store") },
```

- [ ] **Step 2: Commit**

```bash
git add apps/web/src/components/sidebar/sidebar-content.tsx
git commit -m "feat(web): add store navigation to sidebar"
```

---

## 完成

```bash
git add -A
git commit -m "feat(web): add app store pages - store list, detail, rating, and comments"
```