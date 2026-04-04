import { Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/auth-store";
import type { StoreComment } from "@/api/store";

interface CommentListProps {
  comments: StoreComment[];
  onDelete: (commentId: number) => Promise<void>;
  isDeleting: boolean;
}

// 获取头像 URL - 处理空值情况
const getAvatarUrl = (avatarBase64: string | null, name?: string) => {
  if (!avatarBase64 || avatarBase64 === "") {
    const seed = encodeURIComponent(name || "user");
    return `https://api.dicebear.com/7.x/pixel-art/svg?seed=${seed}`;
  }
  if (avatarBase64.startsWith("data:")) {
    return avatarBase64;
  }
  return `data:image/png;base64,${avatarBase64}`;
};

function formatTimeAgo(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMinutes = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffMinutes < 1) return "刚刚";
  if (diffMinutes < 60) return `${diffMinutes}分钟前`;
  if (diffHours < 24) return `${diffHours}小时前`;
  if (diffDays < 30) return `${diffDays}天前`;

  return date.toLocaleDateString("zh-CN");
}

export default function CommentList({
  comments,
  onDelete,
  isDeleting,
}: CommentListProps) {
  const user = useAuthStore((state) => state.user);

  if (comments.length === 0) {
    return (
      <p className="text-center text-muted-foreground py-8">
        暂无评论，快来抢先评论吧！
      </p>
    );
  }

  return (
    <div className="space-y-4">
      {comments.map((comment) => {
        const isOwnComment = user?.id === comment.userId;

        return (
          <div
            key={comment.id}
            className="flex gap-3 rounded-lg border p-4"
          >
            {/* Avatar */}
            <div className="h-10 w-10 shrink-0 overflow-hidden rounded-full">
              <img
                src={getAvatarUrl(comment.userAvatar, comment.userName)}
                alt={comment.userName}
                className="h-full w-full object-cover"
              />
            </div>

            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between gap-2">
                <span className="font-medium">{comment.userName}</span>
                <span className="text-xs text-muted-foreground">
                  {formatTimeAgo(comment.createdAt)}
                </span>
              </div>

              <p className="mt-1 text-sm whitespace-pre-wrap break-words">
                {comment.content}
              </p>

              {isOwnComment && (
                <div className="mt-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-muted-foreground hover:text-destructive"
                    onClick={() => onDelete(comment.id)}
                    disabled={isDeleting}
                  >
                    <Trash2 className="size-4 mr-1" />
                    删除
                  </Button>
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}