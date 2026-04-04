import { useState } from "react";
import { useNavigate } from "react-router";
import { Star } from "lucide-react";
import type { StoreAppItem } from "@/api/store";
import { API_BASE_URL } from "@/lib/config";

interface StoreAppCardProps {
  app: StoreAppItem;
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

/**
 * Get the full logo URL from stored logo path
 */
function getLogoUrl(logo: string | null): string {
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

/**
 * Get author avatar URL with fallback to DiceBear
 */
function getAvatarUrl(avatarBase64: string | null, name?: string): string {
  if (!avatarBase64 || avatarBase64 === "") {
    const seed = encodeURIComponent(name || "user");
    return `https://api.dicebear.com/7.x/pixel-art/svg?seed=${seed}`;
  }
  if (avatarBase64.startsWith("data:")) {
    return avatarBase64;
  }
  return `data:image/png;base64,${avatarBase64}`;
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

export default function StoreAppCard({ app }: StoreAppCardProps) {
  const navigate = useNavigate();
  const [logoLoadFailed, setLogoLoadFailed] = useState(false);

  const logoUrl = getLogoUrl(app.logo);
  const showLogo = !logoLoadFailed && logoUrl.length > 0;
  const logoColor = getLogoColor(app.name);

  const handleImageError = () => {
    setLogoLoadFailed(true);
  };

  return (
    <div
      className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition-all duration-300 hover:shadow-lg hover:border-slate-300 cursor-pointer"
      onClick={() => navigate(`/store/${app.id}`)}
    >
      {/* Subtle glow on hover */}
      <div
        className="absolute -right-16 -top-16 h-32 w-32 rounded-full opacity-0 transition-opacity duration-300 group-hover:opacity-100"
        style={{ backgroundColor: `${logoColor}15` }}
      />

      <div className="relative p-5">
        <div className="flex items-start gap-4">
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

          <div className="flex min-w-0 flex-1 flex-col gap-1">
            {/* Name */}
            <h3 className="line-clamp-1 text-base font-semibold text-slate-800">
              {app.name}
            </h3>

            {/* Rating */}
            <div className="flex items-center gap-1">
              <Star className="size-4 fill-amber-400 text-amber-400" />
              <span className="text-sm font-medium text-slate-700">
                {app.averageRating !== null
                  ? app.averageRating.toFixed(1)
                  : "0.0"}
              </span>
              <span className="text-xs text-slate-400">({app.ratingCount})</span>
            </div>

            {/* Author */}
            <div className="flex items-center gap-2">
              <div className="h-6 w-6 shrink-0 overflow-hidden rounded-full">
                <img
                  src={getAvatarUrl(app.author.avatarBase64, app.author.name)}
                  alt={app.author.name}
                  className="h-full w-full object-cover"
                />
              </div>
              <span className="line-clamp-1 text-sm text-slate-500">
                {app.author.name}
              </span>
            </div>
          </div>
        </div>

        {/* Description */}
        {app.description && (
          <p className="mt-3 line-clamp-2 text-sm text-slate-500">
            {app.description}
          </p>
        )}

        {/* Time */}
        <div className="mt-3 text-xs text-slate-400">
          发布于 {formatTime(app.createdAt)}
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