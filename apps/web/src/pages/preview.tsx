import { useSearchParams, useNavigate } from "react-router";
import { ArrowLeftIcon } from "lucide-react";
import { useEffect, useState } from "react";

export default function PreviewPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const url = searchParams.get("url") || "";
  const appName = searchParams.get("appName") || "App Preview";
  const logoUrl = searchParams.get("logoUrl") || "";

  const [isLoading, setIsLoading] = useState(true);
  const [logoLoadFailed, setLogoLoadFailed] = useState(false);

  const resolvedUrl = url.startsWith("/")
    ? `http://100.101.157.4:8080${url}`
    : url;

  const handleBack = () => {
    navigate(-1);
  };

  return (
    <div className="flex flex-col h-screen bg-white">
      {/* Header */}
      <div className="flex items-center h-12 px-4 border-b border-gray-100">
        <button
          onClick={handleBack}
          className="flex items-center justify-center w-8 h-8 rounded-full hover:bg-gray-100"
        >
          <ArrowLeftIcon className="w-5 h-5" />
        </button>
        <span className="ml-2 text-base font-medium">{appName}</span>
        {logoUrl && !logoLoadFailed && (
          <img
            src={logoUrl}
            alt="Logo"
            className="w-6 h-6 ml-2 rounded-md object-cover"
            onError={() => setLogoLoadFailed(true)}
          />
        )}
      </div>

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