import { useState } from "react";
import { useNavigate } from "react-router";
import { EyeIcon, Trash2Icon, SendIcon, UnplugIcon } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useDeleteApp } from "@/hooks/useApps";
import { usePublishApp, useUnpublishApp } from "@/hooks/useStore";
import { API_BASE_URL } from "@/lib/config";
import type { App } from "@/types/app";

interface AppActionMenuProps {
  app: App;
  children?: React.ReactNode;
}

export function AppActionMenu({ app, children }: AppActionMenuProps) {
  const navigate = useNavigate();
  const deleteApp = useDeleteApp();
  const publishApp = usePublishApp();
  const unpublishApp = useUnpublishApp();
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  const handlePreview = () => {
    const previewUrl = `${API_BASE_URL}/api/preview/${app.uuid}`;
    const params = new URLSearchParams();
    params.set("url", previewUrl);
    params.set("appName", app.name);
    if (app.logo) {
      params.set("logoUrl", app.logo);
    }
    navigate(`/preview?${params.toString()}`);
  };

  const handleDelete = () => {
    deleteApp.mutate(app.id);
    setShowDeleteDialog(false);
  };

  const handlePublish = () => {
    publishApp.mutate(app.id);
  };

  const handleUnpublish = () => {
    unpublishApp.mutate(app.id);
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>{children}</DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={handlePreview}>
            <EyeIcon className="mr-2 h-4 w-4" />
            预览
          </DropdownMenuItem>
          {app.isPublic ? (
            <DropdownMenuItem onClick={handleUnpublish}>
              <UnplugIcon className="mr-2 h-4 w-4" />
              下架
            </DropdownMenuItem>
          ) : (
            <DropdownMenuItem onClick={handlePublish}>
              <SendIcon className="mr-2 h-4 w-4" />
              发布到商店
            </DropdownMenuItem>
          )}
          <DropdownMenuItem
            onClick={() => setShowDeleteDialog(true)}
            className="text-red-600 focus:text-red-600"
          >
            <Trash2Icon className="mr-2 h-4 w-4" />
            删除
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <Dialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>确认删除</DialogTitle>
            <DialogDescription>
              确定要删除应用 "{app.name}" 吗？此操作无法撤销。
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowDeleteDialog(false)}>
              取消
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              删除
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}