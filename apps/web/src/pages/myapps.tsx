import { useState } from "react";
import { useNavigate } from "react-router";
import { ArrowLeftIcon, PlusIcon, Loader2Icon } from "lucide-react";
import { useUserApps, useCreateApp } from "@/hooks/useApps";
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
    const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
    const [appName, setAppName] = useState("");
    const [appDescription, setAppDescription] = useState("");

    const { data: apps, isLoading, error } = useUserApps();
    const createApp = useCreateApp();

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

    const handleGoToCreate = () => {
        navigate("/yuanmeng");
    };

    const handleOpenCreateDialog = () => {
        setIsCreateDialogOpen(true);
    };

    const handleCloseCreateDialog = () => {
        setIsCreateDialogOpen(false);
        setAppName("");
        setAppDescription("");
    };

    return (
        <div className="flex h-full flex-col bg-[#F5F7FA]">
            {/* Header */}
            <div className="flex h-14 shrink-0 items-center justify-between border-b border-gray-200 bg-white px-4">
                {/* Back Button */}
                <button
                    onClick={() => navigate(-1)}
                    className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
                >
                    <ArrowLeftIcon className="h-5 w-5 text-gray-900" />
                </button>

                {/* Title */}
                <h1 className="text-base font-medium text-gray-900">我的元应用</h1>

                {/* Add Button */}
                <button
                    onClick={handleOpenCreateDialog}
                    className="flex h-8 w-8 items-center justify-center rounded-lg transition-colors hover:bg-gray-100"
                >
                    <PlusIcon className="h-5 w-5 text-[#007AFF]" />
                </button>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto px-4 py-4">
                {/* Loading State */}
                {isLoading && (
                    <div className="flex flex-col items-center justify-center gap-3 py-20">
                        <Loader2Icon className="h-8 w-8 animate-spin text-[#007AFF]" />
                        <span className="text-sm text-gray-500">加载中...</span>
                    </div>
                )}

                {/* Error State */}
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

                {/* Empty State */}
                {!isLoading && !error && apps && apps.length === 0 && (
                    <div className="flex flex-col items-center justify-center gap-4 py-20">
                        <span className="text-5xl">📱</span>
                        <div className="text-center">
                            <p className="text-base font-medium text-gray-900">
                                还没有应用
                            </p>
                            <p className="mt-1 text-sm text-gray-500">
                                通过对话创建你的第一个元应用吧
                            </p>
                        </div>
                        <Button
                            onClick={handleGoToCreate}
                            className="mt-2 bg-[#007AFF] text-white hover:bg-[#007AFF]/90"
                        >
                            去创建
                        </Button>
                    </div>
                )}

                {/* Normal State - List of Apps */}
                {!isLoading && !error && apps && apps.length > 0 && (
                    <div className="space-y-3">
                        {apps.map((app) => (
                            <AppItem key={app.id} app={app} />
                        ))}
                    </div>
                )}
            </div>

            {/* Create App Dialog */}
            <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
                <DialogContent className="sm:max-w-[360px]">
                    <DialogHeader>
                        <DialogTitle>创建新应用</DialogTitle>
                    </DialogHeader>
                    <div className="space-y-4 py-2">
                        <div className="space-y-2">
                            <label className="text-sm font-medium text-gray-700">
                                应用名称 <span className="text-red-500">*</span>
                            </label>
                            <Input
                                placeholder="请输入应用名称"
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
                            <label className="text-sm font-medium text-gray-700">
                                应用描述
                            </label>
                            <textarea
                                className="min-h-[80px] w-full rounded-lg border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 resize-none"
                                placeholder="请输入应用描述（可选）"
                                value={appDescription}
                                onChange={(e) => setAppDescription(e.target.value)}
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={handleCloseCreateDialog}
                        >
                            取消
                        </Button>
                        <Button
                            onClick={handleCreateApp}
                            disabled={!appName.trim() || createApp.isPending}
                            className="bg-[#007AFF] text-white hover:bg-[#007AFF]/90"
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
        </div>
    );
}