import { useState } from "react";
import { useNavigate } from "react-router";
import { API_BASE_URL } from "@/lib/config";
import { AppActionMenu } from "./AppActionMenu";
import type { App } from "@/types/app";

interface AppItemProps {
  app: App;
}

const LOGO_COLORS = [
  "#007AFF",
  "#5856D6",
  "#AF52DE",
  "#FF2D55",
  "#FF3B30",
  "#FF9500",
  "#FFCC00",
  "#34C759",
  "#30B0C7",
  "#8E8E93",
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
      className="flex cursor-pointer items-start gap-4 rounded-2xl bg-card p-4 text-card-foreground ring-1 ring-foreground/10 transition-colors hover:bg-accent/50"
      onClick={handlePreview}
    >
      {/* Logo */}
      <div className="relative h-14 w-14 shrink-0 overflow-hidden rounded-[14px]">
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
            style={{ backgroundColor: getLogoColor(app.name) }}
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
        <div className="flex items-center gap-1">
          <span className="truncate text-[17px] font-medium leading-snug">
            {app.name}
          </span>
          {latestVersion !== null && (
            <span className="shrink-0 text-[13px] text-[#007AFF]">
              v{latestVersion}
            </span>
          )}
        </div>

        {/* Description */}
        <p className="truncate text-sm text-[#8E8E93]">
          {app.description || "暂无描述"}
        </p>

        {/* Time and Version Count */}
        <div className="mt-1 flex flex-wrap items-center gap-x-1 gap-y-1 text-xs text-[#C7C7CC]">
          <span>创建于 {formatTime(app.createdAt)}</span>
          {app.updatedAt !== app.createdAt && (
            <>
              <span> · </span>
              <span>更新 {formatTime(app.updatedAt)}</span>
            </>
          )}
          {versionCount > 1 && (
            <>
              <span> · </span>
              <span>{versionCount}个版本</span>
            </>
          )}
        </div>
      </div>

      {/* Action Menu */}
      <div onClick={(e) => e.stopPropagation()}>
        <AppActionMenu app={app}>
          <button className="flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground">
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
  );
}