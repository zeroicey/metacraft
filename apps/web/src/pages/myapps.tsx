import { useState } from "react";
import { useNavigate } from "react-router";
import { ArrowLeftIcon, PlusIcon, Loader2Icon, Sparkles, Rocket } from "lucide-react";
import { useUserApps, useCreateApp } from "@/hooks/useApps";
import { useAppStore } from "@/stores/app-store";
import { useUserSessions, useCreateSession } from "@/hooks/useChatSession";
import { getSessionMessages } from "@/api/session";
import { AppItem } from "@/components/app/AppItem";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { toast } from "sonner";

export default function MyAppsPage() {
  const navigate = useNavigate();
  const setSelectedSessionId = useAppStore((state) => state.setSelectedSessionId);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [appName, setAppName] = useState("");
  const [appDescription, setAppDescription] = useState("");
  const [isCreating, setIsCreating] = useState(false);

  const { data: apps, isLoading, error } = useUserApps();
  const createApp = useCreateApp();
  const createSession = useCreateSession();
  const { data: sessions = [] } = useUserSessions();

  const getSessionMessagesById = async (sessionId: string) => {
    try {
      return await getSessionMessages(sessionId);
    } catch {
      return [];
    }
  };

  const DEFAULT_TITLE = "未命名会话";

  const handleGoToChat = async () => {
    if (isCreating) return;
    setIsCreating(true);

    try {
      let targetSessionId = "";

      for (const session of sessions) {
        if (session.title === DEFAULT_TITLE) {
          const messages = await getSessionMessagesById(session.sessionId);
          if (messages.length === 0) {
            targetSessionId = session.sessionId;
            break;
          }
        }
      }

      if (targetSessionId) {
        setSelectedSessionId(targetSessionId);
      } else {
        const newSession = await createSession.mutateAsync({ title: DEFAULT_TITLE });
        setSelectedSessionId(newSession.sessionId);
      }

      navigate("/yuanchuang");
    } catch (error) {
      console.error("Failed to create session:", error);
    } finally {
      setIsCreating(false);
    }
  };

  const handleCreateApp = async () => {
    if (!appName.trim()) {
      toast.error("请输入应用名称");
      return;
    }

    try {
      await createApp.mutateAsync({
        name: appName.trim(),
        description: appDescription.trim() || undefined,
      });
      setIsCreateDialogOpen(false);
      setAppName("");
      setAppDescription("");
    } catch (error) {
      console.error("Failed to create app:", error);
    }
  };

  const handleCloseCreateDialog = () => {
    setIsCreateDialogOpen(false);
    setAppName("");
    setAppDescription("");
  };

  return (
    <div className="flex h-full flex-col bg-gradient-to-br from-slate-50 via-white to-slate-100">
      {/* Header */}
      <div className="flex h-16 shrink-0 items-center justify-between border-b border-slate-200/60 bg-white/80 px-6 backdrop-blur-sm">
        <button
          onClick={() => navigate(-1)}
          className="flex h-10 w-10 items-center justify-center rounded-xl transition-all hover:bg-slate-100"
        >
          <ArrowLeftIcon className="h-5 w-5 text-slate-700" />
        </button>

        <div className="flex items-center gap-2">
          <div className="h-2 w-2 animate-pulse rounded-full bg-cyan-500" />
          <h1 className="text-lg font-semibold text-slate-800">我的元应用</h1>
        </div>

        <button
          onClick={handleGoToChat}
          disabled={isCreating}
          className="group relative overflow-hidden rounded-xl bg-gradient-to-r from-cyan-500 to-blue-500 px-4 py-2 text-sm font-medium text-white shadow-md shadow-cyan-500/25 transition-all hover:from-cyan-400 hover:to-blue-400 hover:shadow-lg hover:shadow-cyan-500/30 disabled:opacity-50"
        >
          <span className="relative z-10 flex items-center gap-2">
            {isCreating ? (
              <Loader2Icon className="h-4 w-4 animate-spin" />
            ) : (
              <PlusIcon className="h-4 w-4" />
            )}
            新建
          </span>
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto">
        <div className="mx-auto min-h-full max-w-5xl px-6 py-8">
          {/* Loading State */}
          {isLoading && (
            <div className="flex flex-col items-center justify-center gap-4 py-32">
              <div className="relative">
                <Loader2Icon className="h-12 w-12 animate-spin text-cyan-500" />
                <div className="absolute inset-0 animate-ping rounded-full bg-cyan-500/20" />
              </div>
              <span className="text-sm text-slate-500">正在加载你的创意...</span>
            </div>
          )}

          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center gap-4 py-32">
              <div className="rounded-full bg-red-50 p-4">
                <span className="text-2xl">⚠️</span>
              </div>
              <span className="text-sm text-red-500">加载失败，请稍后重试</span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => window.location.reload()}
                className="border-slate-200 text-slate-600 hover:bg-slate-50"
              >
                重试
              </Button>
            </div>
          )}

          {/* Empty State */}
          {!isLoading && !error && (!apps || apps.length === 0) && (
            <div className="flex flex-col items-center justify-center gap-6 py-32">
              {/* Animated Illustration */}
              <div className="relative">
                <div className="flex h-32 w-32 items-center justify-center rounded-full bg-gradient-to-br from-cyan-50 to-blue-50 shadow-lg">
                  <Rocket className="h-16 w-16 text-cyan-500" />
                </div>
                <div className="absolute -right-4 -top-2 animate-bounce">
                  <Sparkles className="h-8 w-8 text-amber-400" />
                </div>
                <div className="absolute -bottom-2 -left-2 animate-pulse">
                  <div className="h-4 w-4 rounded-full bg-cyan-400/50" />
                </div>
              </div>

              <div className="text-center">
                <h2 className="text-xl font-semibold text-slate-800">还没有元应用</h2>
                <p className="mt-2 text-sm text-slate-500">
                  通过 AI 对话，让创意变为现实
                </p>
              </div>

              <Button
                onClick={handleGoToChat}
                className="mt-4 bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:from-cyan-400 hover:to-blue-400"
              >
                <Sparkles className="mr-2 h-4 w-4" />
                开始创作
              </Button>
            </div>
          )}

          {/* Stats */}
          {!isLoading && !error && apps && apps.length > 0 && (
            <div className="mb-8 grid grid-cols-3 gap-4">
              <div className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:shadow-md hover:border-cyan-200">
                <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-cyan-50 transition-all group-hover:bg-cyan-100" />
                <div className="relative">
                  <p className="text-3xl font-bold text-slate-800">{apps.length}</p>
                  <p className="mt-1 text-xs text-slate-500">已创建应用</p>
                </div>
              </div>

              <div className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:shadow-md hover:border-purple-200">
                <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-purple-50 transition-all group-hover:bg-purple-100" />
                <div className="relative">
                  <p className="text-3xl font-bold text-slate-800">
                    {apps.reduce((acc, app) => acc + (app.versions?.length || 0), 0)}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">总版本数</p>
                </div>
              </div>

              <div className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:shadow-md hover:border-amber-200">
                <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-amber-50 transition-all group-hover:bg-amber-100" />
                <div className="relative">
                  <p className="text-3xl font-bold text-slate-800">
                    {apps.filter(app => {
                      const created = new Date(app.createdAt);
                      const now = new Date();
                      const diffDays = (now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24);
                      return diffDays <= 7;
                    }).length}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">本周新增</p>
                </div>
              </div>
            </div>
          )}

          {/* App Grid */}
          {!isLoading && !error && apps && apps.length > 0 && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {apps.map((app, index) => (
                <div
                  key={app.id}
                  className="animate-fade-in-up"
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <AppItem app={app} />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Create App Dialog */}
      <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
        <DialogContent className="sm:max-w-[400px]">
          <DialogHeader>
            <DialogTitle>创建新应用</DialogTitle>
          </DialogHeader>
          <div className="space-y-5 py-4">
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700">
                应用名称 <span className="text-red-500">*</span>
              </label>
              <Input
                placeholder="给你的应用起个名字"
                value={appName}
                onChange={(e) => setAppName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && appName.trim()) {
                    handleCreateApp();
                  }
                }}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-700">
                应用描述
              </label>
              <textarea
                className="min-h-[100px] w-full rounded-xl border border-input bg-background px-4 py-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring/20 resize-none"
                placeholder="简单描述你的应用功能（可选）"
                value={appDescription}
                onChange={(e) => setAppDescription(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter className="gap-2">
            <Button
              variant="outline"
              onClick={handleCloseCreateDialog}
            >
              取消
            </Button>
            <Button
              onClick={handleCreateApp}
              disabled={!appName.trim() || createApp.isPending}
              className="bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:from-cyan-400 hover:to-blue-400"
            >
              {createApp.isPending ? (
                <>
                  <Loader2Icon className="mr-2 h-4 w-4 animate-spin" />
                  创建中...
                </>
              ) : (
                "创建"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <style>{`
        @keyframes fade-in-up {
          from {
            opacity: 0;
            transform: translateY(20px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
        .animate-fade-in-up {
          animation: fade-in-up 0.4s ease-out forwards;
          opacity: 0;
        }
      `}</style>
    </div>
  );
}