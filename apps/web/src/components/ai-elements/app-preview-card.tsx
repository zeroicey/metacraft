import { Maximize2Icon } from "lucide-react";
import { useState } from "react";

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
  const resolvedUrl = previewUrl.startsWith("/")
    ? `http://100.101.157.4:8080${previewUrl}`
    : previewUrl;

  const handleOpenNewWindow = () => {
    window.open(resolvedUrl, "_blank");
  };

  return (
    <div className="mt-2 rounded-xl border border-[#EEEEEE] bg-[#F5F7FA] p-2.5">
      <div className="mb-2.5 flex items-center justify-end">
        <button
          onClick={handleOpenNewWindow}
          className="flex h-7 w-7 items-center justify-center rounded-full bg-[#007AFF] text-white transition hover:bg-[#0056CC]"
          title="在新窗口打开"
        >
          <Maximize2Icon className="h-4 w-4" />
        </button>
      </div>
      {resolvedUrl ? (
        <div className="relative h-[210px] overflow-hidden rounded-lg bg-white">
          <iframe
            src={resolvedUrl}
            className="h-full w-full border-0"
            title={`${appName || "App"} Preview`}
            onLoad={() => setIsLoading(false)}
            sandbox="allow-scripts allow-same-origin allow-forms"
          />
          {isLoading && (
            <div className="absolute inset-0 flex items-center justify-center bg-white">
              <span className="text-sm text-gray-400">预览加载中...</span>
            </div>
          )}
        </div>
      ) : (
        <div className="flex h-[210px] items-center justify-center rounded-lg bg-white">
          <span className="text-sm text-gray-400">预览加载中...</span>
        </div>
      )}
    </div>
  );
}