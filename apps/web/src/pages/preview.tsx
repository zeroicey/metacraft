import { useSearchParams, useNavigate } from "react-router";
import { ArrowLeftIcon } from "lucide-react";
import { useState } from "react";
import { API_BASE_URL } from "@/lib/config";

export default function PreviewPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const url = searchParams.get("url") || "";
  const appName = searchParams.get("appName") || "App Preview";
  const logoUrl = searchParams.get("logoUrl") || "";

  const [isLoading, setIsLoading] = useState(true);
  const [logoLoadFailed, setLogoLoadFailed] = useState(false);

  const resolvedUrl = url.startsWith("/")
    ? `${API_BASE_URL}${url}`
    : url;

  const handleBack = () => {
    navigate(-1);
  };

  return (
    <div className="flex flex-col h-screen bg-white">
      {/* Floating Back Button */}
      <button
        onClick={handleBack}
        className="absolute left-4 top-4 z-50 flex items-center justify-center w-10 h-10 rounded-full bg-white/90 shadow-md hover:bg-gray-100"
      >
        <ArrowLeftIcon className="w-5 h-5" />
      </button>

      {/* Iframe Preview */}
      {resolvedUrl ? (
        <div className="flex-1 relative">
          <iframe
            src={resolvedUrl}
            className="w-full h-full border-0"
            title={`${appName} Preview`}
            onLoad={() => setIsLoading(false)}
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
          />
          {isLoading && (
            <div className="absolute inset-0 flex items-center justify-center bg-white">
              <span className="text-sm text-gray-400">预览加载中...</span>
            </div>
          )}
        </div>
      ) : (
        <div className="flex-1 flex items-center justify-center">
          <span className="text-sm text-gray-400">No URL provided</span>
        </div>
      )}
    </div>
  );
}