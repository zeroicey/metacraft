import { useNavigate } from "react-router";
import { ArrowLeftIcon, Loader2Icon, SearchIcon, Store, TrendingUp, Star } from "lucide-react";
import { useStoreApps } from "@/hooks/useStore";
import StoreAppCard from "@/components/store/StoreAppCard";
import { Button } from "@/components/ui/button";

export default function StorePage() {
  const navigate = useNavigate();
  const { data: apps, isLoading, error, refetch } = useStoreApps();

  // Calculate stats
  const totalApps = apps?.length || 0;
  const totalRatings = apps?.reduce((acc, app) => acc + app.ratingCount, 0) || 0;
  const avgRating = totalApps > 0
    ? (apps?.reduce((acc, app) => acc + (app.averageRating || 0), 0) || 0) / totalApps
    : 0;

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
          <Store className="h-5 w-5 text-cyan-500" />
          <h1 className="text-lg font-semibold text-slate-800">元应用商店</h1>
        </div>

        <div className="h-10 w-10" />
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
              <span className="text-sm text-slate-500">正在加载商店...</span>
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
                onClick={() => refetch()}
                className="border-slate-200 text-slate-600 hover:bg-slate-50"
              >
                重试
              </Button>
            </div>
          )}

          {/* Empty State */}
          {!isLoading && !error && apps && apps.length === 0 && (
            <div className="flex flex-col items-center justify-center gap-6 py-32">
              <div className="relative">
                <div className="flex h-32 w-32 items-center justify-center rounded-full bg-gradient-to-br from-slate-100 to-slate-200 shadow-lg">
                  <SearchIcon className="h-16 w-16 text-slate-400" />
                </div>
              </div>

              <div className="text-center">
                <h2 className="text-xl font-semibold text-slate-800">暂无应用</h2>
                <p className="mt-2 text-sm text-slate-500">
                  快去创建第一个元应用吧
                </p>
              </div>
            </div>
          )}

          {/* Stats */}
          {!isLoading && !error && apps && apps.length > 0 && (
            <div className="mb-8 grid grid-cols-3 gap-4">
              <div className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:shadow-md hover:border-cyan-200">
                <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-cyan-50 transition-all group-hover:bg-cyan-100" />
                <div className="relative flex items-center gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-cyan-50">
                    <Store className="h-6 w-6 text-cyan-500" />
                  </div>
                  <div>
                    <p className="text-3xl font-bold text-slate-800">{totalApps}</p>
                    <p className="mt-1 text-xs text-slate-500">商店应用</p>
                  </div>
                </div>
              </div>

              <div className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:shadow-md hover:border-amber-200">
                <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-amber-50 transition-all group-hover:bg-amber-100" />
                <div className="relative flex items-center gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-50">
                    <Star className="h-6 w-6 text-amber-500" />
                  </div>
                  <div>
                    <p className="text-3xl font-bold text-slate-800">
                      {avgRating > 0 ? avgRating.toFixed(1) : "0.0"}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">平均评分</p>
                  </div>
                </div>
              </div>

              <div className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:shadow-md hover:border-purple-200">
                <div className="absolute -right-8 -top-8 h-24 w-24 rounded-full bg-purple-50 transition-all group-hover:bg-purple-100" />
                <div className="relative flex items-center gap-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-50">
                    <TrendingUp className="h-6 w-6 text-purple-500" />
                  </div>
                  <div>
                    <p className="text-3xl font-bold text-slate-800">{totalRatings}</p>
                    <p className="mt-1 text-xs text-slate-500">总评分次数</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Section Title */}
          {!isLoading && !error && apps && apps.length > 0 && (
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-lg font-semibold text-slate-800">全部应用</h2>
              <span className="text-sm text-slate-500">{totalApps} 个应用</span>
            </div>
          )}

          {/* Grid of Apps */}
          {!isLoading && !error && apps && apps.length > 0 && (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {apps.map((app, index) => (
                <div
                  key={app.id}
                  className="animate-fade-in-up"
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <StoreAppCard app={app} />
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

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