import { Trash2 } from "lucide-react";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/auth-store";
import type { StoreComment } from "@/api/store";

interface CommentListProps {
  comments: StoreComment[];
  onDelete: (commentId: number) => Promise<void>;
  isDeleting: boolean;
}

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
            <Avatar>
              <AvatarImage src={comment.userAvatar || undefined} />
              <AvatarFallback>{comment.userName.charAt(0)}</AvatarFallback>
            </Avatar>

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