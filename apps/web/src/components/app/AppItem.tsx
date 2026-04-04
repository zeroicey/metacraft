import { useState } from "react";
import { useNavigate } from "react-router";
import { API_BASE_URL } from "@/lib/config";
import { AppActionMenu } from "./AppActionMenu";
import type { App } from "@/types/app";

interface AppItemProps {
  app: App;
}

const LOGO_COLORS = [
  "#06b6d4", // cyan
  "#8b5cf6", // violet
  "#f43f5e", // rose
  "#f59e0b", // amber
  "#10b981", // emerald
  "#ec4899", // pink
  "#3b82f6", // blue
  "#6366f1", // indigo
];

function getLogoColor(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return LOGO_COLORS[Math.abs(hash) % LOGO_COLORS.length];
}

function getLogoInitial(name: string): string {
  return name && name.length > 0 ? name.charAt(0).toUpperCase() : "A";
}

function getLogoUrl(logo: string | undefined): string {
  if (!logo || logo.length === 0) {
    return "";
  }

  if (logo.startsWith("http://") || logo.startsWith("https://")) {
    return logo;
  }

  const dotIndex = logo.lastIndexOf(".");
  const logoUuid = dotIndex > 0 ? logo.substring(0, dotIndex) : logo;
  return `${API_BASE_URL}/api/logo/${logoUuid}`;
}

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

export function AppItem({ app }: AppItemProps) {
  const navigate = useNavigate();
  const [logoLoadFailed, setLogoLoadFailed] = useState(false);

  const logoUrl = getLogoUrl(app.logo);
  const showLogo = !logoLoadFailed && logoUrl.length > 0;
  const versionCount = app.versions?.length || 0;
  const latestVersion = versionCount > 0 ? app.versions[0].versionNumber : null;
  const logoColor = getLogoColor(app.name);

  const handlePreview = () => {
    const previewUrl = `${API_BASE_URL}/api/preview/${app.uuid}`;
    const params = new URLSearchParams();
    params.set("url", previewUrl);
    params.set("appName", app.name);
    if (app.logo && !logoLoadFailed) {
      params.set("logoUrl", logoUrl);
    }
    navigate(`/preview?${params.toString()}`);
  };

  const handleImageError = () => {
    setLogoLoadFailed(true);
  };

  return (
    <div
      className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition-all duration-300 hover:shadow-lg hover:border-slate-300"
      onClick={handlePreview}
    >
      {/* Subtle glow on hover */}
      <div
        className="absolute -right-16 -top-16 h-32 w-32 rounded-full opacity-0 transition-opacity duration-300 group-hover:opacity-100"
        style={{ backgroundColor: `${logoColor}20` }}
      />

      <div className="relative flex items-start gap-4 p-5">
        {/* Logo */}
        <div className="relative h-16 w-16 shrink-0 overflow-hidden rounded-2xl shadow-md">
          {showLogo ? (
            <img
              src={logoUrl}
              alt={app.name}
              className="h-full w-full object-cover"
              onError={handleImageError}
            />
          ) : (
            <div
              className="flex h-full w-full items-center justify-center"
              style={{ backgroundColor: logoColor }}
            >
              <span className="text-2xl font-bold text-white">
                {getLogoInitial(app.name)}
              </span>
            </div>
          )}
        </div>

        {/* Content */}
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          {/* Name and Version */}
          <div className="flex items-start gap-2">
            <span className="line-clamp-1 text-base font-semibold text-slate-800">
              {app.name}
            </span>
            {latestVersion !== null && (
              <span className="shrink-0 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                v{latestVersion}
              </span>
            )}
          </div>

          {/* Description */}
          <p className="line-clamp-2 text-sm text-slate-500">
            {app.description || "暂无描述"}
          </p>

          {/* Time and Version Count */}
          <div className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-400">
            <span>创建于 {formatTime(app.createdAt)}</span>
            {app.updatedAt !== app.createdAt && (
              <>
                <span className="text-slate-300">·</span>
                <span>更新 {formatTime(app.updatedAt)}</span>
              </>
            )}
            {versionCount > 1 && (
              <>
                <span className="text-slate-300">·</span>
                <span>{versionCount}个版本</span>
              </>
            )}
          </div>
        </div>

        {/* Action Menu */}
        <div onClick={(e) => e.stopPropagation()}>
          <AppActionMenu app={app}>
            <button className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-400 transition-all hover:bg-slate-100 hover:text-slate-600">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <circle cx="12" cy="12" r="1" />
                <circle cx="12" cy="5" r="1" />
                <circle cx="12" cy="19" r="1" />
              </svg>
            </button>
          </AppActionMenu>
        </div>
      </div>

      {/* Bottom Accent Line */}
      <div
        className="h-0.5 w-full origin-left scale-x-0 transition-transform duration-300 group-hover:scale-x-100"
        style={{ backgroundColor: logoColor }}
      />
    </div>
  );
}