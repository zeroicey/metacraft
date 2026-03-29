import { useNavigate } from "react-router";
import { ArrowLeftIcon, Loader2Icon, SearchIcon } from "lucide-react";
import { useStoreApps } from "@/hooks/useStore";
import StoreAppCard from "@/components/store/StoreAppCard";
import { Button } from "@/components/ui/button";

export default function StorePage() {
  const navigate = useNavigate();
  const { data: apps, isLoading, error, refetch } = useStoreApps();

  return (
    <div className="flex h-full flex-col bg-gradient-to-b from-[#F0F4F8] to-white">
      {/* Header */}
      <div className="flex h-14 shrink-0 items-center justify-between border-b border-[#E5E7EB] bg-white px-4 shadow-sm">
        {/* Back Button */}
        <button
          onClick={() => navigate(-1)}
          className="flex h-9 w-9 items-center justify-center rounded-xl transition-colors hover:bg-gray-100"
        >
          <ArrowLeftIcon className="h-5 w-5 text-gray-900" />
        </button>

        {/* Title */}
        <h1 className="text-base font-semibold text-gray-900">元应用商店</h1>

        {/* Placeholder for balance */}
        <div className="h-9 w-9" />
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4">
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
              onClick={() => refetch()}
            >
              重试
            </Button>
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !error && apps && apps.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-4 py-20">
            <SearchIcon className="h-12 w-12 text-gray-300" />
            <div className="text-center">
              <p className="text-base font-medium text-gray-900">
                暂无应用
              </p>
              <p className="mt-1 text-sm text-gray-500">
                快去创建第一个元应用吧
              </p>
            </div>
          </div>
        )}

        {/* Normal State - Grid of Apps */}
        {!isLoading && !error && apps && apps.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {apps.map((app) => (
              <StoreAppCard key={app.id} app={app} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
