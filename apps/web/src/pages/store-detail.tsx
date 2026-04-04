import { useState } from "react";
import { useParams, useNavigate } from "react-router";
import { ArrowLeftIcon, ShareIcon, Loader2Icon } from "lucide-react";
import { useStoreAppDetail, useRateApp, useCommentApp, useDeleteComment } from "@/hooks/useStore";
import { useAuthStore } from "@/stores/auth-store";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import StarRating from "@/components/store/StarRating";
import CommentList from "@/components/store/CommentList";
import { toast } from "sonner";
import { API_BASE_URL } from "@/lib/config";

// 获取头像 URL
const getAvatarUrl = (avatarBase64?: string | null, name?: string) => {
    if (!avatarBase64 || avatarBase64 === "") {
        const seed = encodeURIComponent(name || "user");
        return `https://api.dicebear.com/7.x/pixel-art/svg?seed=${seed}`;
    }
    if (avatarBase64.startsWith("data:")) {
        return avatarBase64;
    }
    return `data:image/png;base64,${avatarBase64}`;
};

// 获取 Logo URL
const getLogoUrl = (logo: string | null) => {
    if (!logo || logo.length === 0) {
        return "";
    }
    if (logo.startsWith("http://") || logo.startsWith("https://")) {
        return logo;
    }
    const dotIndex = logo.lastIndexOf(".");
    const logoUuid = dotIndex > 0 ? logo.substring(0, dotIndex) : logo;
    return `${API_BASE_URL}/api/logo/${logoUuid}`;
};

export default function StoreDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const appId = Number(id);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  const { data: app, isLoading, error, refetch } = useStoreAppDetail(appId);
  const logoUrl = app ? getLogoUrl(app.logo) : "";
  const rateApp = useRateApp(appId);
  const commentApp = useCommentApp(appId);
  const deleteComment = useDeleteComment(appId);

  const [commentContent, setCommentContent] = useState("");
  const [isSubmittingComment, setIsSubmittingComment] = useState(false);

  const handleShare = async () => {
    if (!app) return;

    const shareUrl = window.location.href;
    try {
      await navigator.clipboard.writeText(shareUrl);
      toast.success("链接已复制到剪贴板");
    } catch {
      toast.error("复制失败");
    }
  };

  const handleRate = async (rating: number) => {
    try {
      await rateApp.mutateAsync(rating);
      toast.success("评分成功");
    } catch (error) {
      console.error("Failed to rate:", error);
      throw error;
    }
  };

  const handleSubmitComment = async () => {
    if (!commentContent.trim()) {
      toast.error("请输入评论内容");
      return;
    }

    setIsSubmittingComment(true);
    try {
      await commentApp.mutateAsync(commentContent.trim());
      setCommentContent("");
      toast.success("评论成功");
    } catch (error) {
      console.error("Failed to comment:", error);
    } finally {
      setIsSubmittingComment(false);
    }
  };

  const handleDeleteComment = async (commentId: number) => {
    try {
      await deleteComment.mutateAsync(commentId);
      toast.success("删除成功");
    } catch (error) {
      console.error("Failed to delete comment:", error);
    }
  };

  // Loading State
  if (isLoading) {
    return (
      <div className="flex h-full flex-col bg-[#F5F7FA]">
        <div className="flex h-14 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4">
          <button
            onClick={() => navigate(-1)}
            className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
          >
            <ArrowLeftIcon className="h-5 w-5 text-gray-900" />
          </button>
          <h1 className="text-base font-medium text-gray-900">应用详情</h1>
          <div className="h-8 w-8" />
        </div>
        <div className="flex flex-1 items-center justify-center">
          <Loader2Icon className="h-8 w-8 animate-spin text-[#007AFF]" />
        </div>
      </div>
    );
  }

  // Error State
  if (error || !app) {
    return (
      <div className="flex h-full flex-col bg-[#F5F7FA]">
        <div className="flex h-14 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4">
          <button
            onClick={() => navigate(-1)}
            className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
          >
            <ArrowLeftIcon className="h-5 w-5 text-gray-900" />
          </button>
          <h1 className="text-base font-medium text-gray-900">应用详情</h1>
          <div className="h-8 w-8" />
        </div>
        <div className="flex flex-1 flex-col items-center justify-center gap-3">
          <span className="text-sm text-red-500">加载失败，请稍后重试</span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
          >
            重试
          </Button>
        </div>
      </div>
    );
  }

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
        <h1 className="text-base font-medium text-gray-900">应用详情</h1>
        <button
          onClick={handleShare}
          className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
        >
          <ShareIcon className="h-5 w-5 text-gray-900" />
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto">
        <div className="mx-auto max-w-2xl">
        {/* App Info Section */}
        <div className="bg-white p-4">
          <div className="flex gap-4">
            {/* Logo */}
            <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-lg bg-muted">
              {logoUrl ? (
                <img
                  src={logoUrl}
                  alt={app.name}
                  className="h-full w-full rounded-lg object-cover"
                />
              ) : (
                <span className="text-3xl font-bold text-muted-foreground">
                  {app.name.charAt(0)}
                </span>
              )}
            </div>

            <div className="flex min-w-0 flex-1 flex-col justify-between">
              {/* Name */}
              <h2 className="truncate text-lg font-semibold text-gray-900">
                {app.name}
              </h2>

              {/* Rating */}
              <div className="flex items-center gap-1 text-sm text-muted-foreground">
                <span className="text-yellow-500">
                  {app.averageRating !== null ? app.averageRating.toFixed(1) : "暂无"}
                </span>
                <span className="text-xs">({app.ratingCount} 评分)</span>
              </div>

              {/* Author */}
              <div className="flex items-center gap-2">
                <Avatar size="sm">
                  <AvatarImage src={getAvatarUrl(app.author.avatarBase64, app.author.name)} />
                  <AvatarFallback>
                    {app.author.name.charAt(0)}
                  </AvatarFallback>
                </Avatar>
                <span className="truncate text-sm text-muted-foreground">
                  {app.author.name}
                </span>
              </div>
            </div>
          </div>

          {/* Description */}
          {app.description && (
            <p className="mt-3 text-sm text-gray-600">{app.description}</p>
          )}
        </div>

        {/* Preview Section */}
        <div className="mt-2 bg-white p-4">
          <h3 className="mb-3 text-base font-medium text-gray-900">预览</h3>
          <div className="h-[400px] w-full overflow-hidden rounded-lg border">
            <iframe
              src={`${API_BASE_URL}/api/preview/${app.uuid}`}
              className="h-full w-full border-0"
              title={`${app.name} Preview`}
              sandbox="allow-scripts allow-same-origin"
            />
          </div>
        </div>

        {/* Rating Section */}
        <div className="mt-2 bg-white p-4">
          <h3 className="mb-3 text-base font-medium text-gray-900">评分</h3>
          {isAuthenticated ? (
            <StarRating
              onRate={handleRate}
              disabled={rateApp.isPending}
            />
          ) : (
            <p className="text-sm text-muted-foreground">登录后可评分</p>
          )}
        </div>

        {/* Comment Section */}
        <div className="mt-2 bg-white p-4">
          <h3 className="mb-3 text-base font-medium text-gray-900">评论</h3>

          {/* Comment Input */}
          {isAuthenticated ? (
            <div className="mb-4 space-y-2">
              <Textarea
                placeholder="写下你的评论..."
                value={commentContent}
                onChange={(e) => setCommentContent(e.target.value)}
                rows={3}
              />
              <div className="flex justify-end">
                <Button
                  onClick={handleSubmitComment}
                  disabled={!commentContent.trim() || isSubmittingComment}
                  size="sm"
                >
                  {isSubmittingComment ? (
                    <>
                      <Loader2Icon className="mr-2 h-4 w-4 animate-spin" />
                      发布中...
                    </>
                  ) : (
                    "发布"
                  )}
                </Button>
              </div>
            </div>
          ) : (
            <p className="mb-4 text-sm text-muted-foreground">登录后可评论</p>
          )}

          {/* Comments List */}
          <CommentList
            comments={app.comments}
            onDelete={handleDeleteComment}
            isDeleting={deleteComment.isPending}
          />
        </div>
        </div>
      </div>
    </div>
  );
}