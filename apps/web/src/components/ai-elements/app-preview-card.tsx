import { Maximize2Icon } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { API_BASE_URL } from "@/lib/config";

interface AppPreviewCardProps {
  previewUrl: string;
  appName?: string;
  logoUrl?: string;
}

export function AppPreviewCard({
  previewUrl,
  appName,
  logoUrl,
}: AppPreviewCardProps) {
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();
  const resolvedUrl = previewUrl.startsWith("/")
    ? `${API_BASE_URL}${previewUrl}`
    : previewUrl;

  const handleOpenNewWindow = () => {
    // 跳转到全屏预览页面
    const params = new URLSearchParams();
    params.set("url", resolvedUrl);
    if (appName) params.set("appName", appName);
    if (logoUrl) params.set("logoUrl", logoUrl);
    navigate(`/preview?${params.toString()}`);
  };

  return (
    <div className="mt-2 relative max-w-lg">
      <button
        onClick={handleOpenNewWindow}
        className="absolute right-3 top-3 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-[#007AFF] text-white transition hover:bg-[#0056CC] shadow-md hover:shadow-lg"
        title="在新窗口打开"
      >
        <Maximize2Icon className="h-4 w-4" />
      </button>
      {resolvedUrl ? (
        <div className="relative h-[400px] overflow-hidden rounded-2xl bg-white shadow-lg border border-[#E8F0FE]">
          <iframe
            src={resolvedUrl}
            className="h-full w-full border-0"
            title={`${appName || "App"} Preview`}
            onLoad={() => setIsLoading(false)}
            sandbox="allow-scripts allow-same-origin allow-forms"
          />
          {isLoading && (
            <div className="absolute inset-0 flex items-center justify-center bg-white/80 backdrop-blur-sm">
              <span className="text-sm text-gray-400">预览加载中...</span>
            </div>
          )}
        </div>
      ) : (
        <div className="flex h-[400px] items-center justify-center rounded-2xl bg-white shadow-lg border border-[#E8F0FE]">
          <span className="text-sm text-gray-400">预览加载中...</span>
        </div>
      )}
    </div>
  );
}