import { useSearchParams, useNavigate } from "react-router";
import { ArrowLeftIcon } from "lucide-react";
import { useState } from "react";
import { API_BASE_URL } from "@/lib/config";

export default function PreviewPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const url = searchParams.get("url") || "";
  const appName = searchParams.get("appName") || "App Preview";
  // const logoUrl = searchParams.get("logoUrl") || "";

  const [isLoading, setIsLoading] = useState(true);
  // const [logoLoadFailed, setLogoLoadFailed] = useState(false);

  // 拖动相关状态
  const [position, setPosition] = useState({ x: 16, y: 16 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const [dragStartPos, setDragStartPos] = useState({ x: 0, y: 0 }); // 记录拖动开始时的指针位置

  const resolvedUrl = url.startsWith("/")
    ? `${API_BASE_URL}${url}`
    : url;

  const handleBack = () => {
    navigate(-1);
  };

  const handlePointerDown = (e: React.PointerEvent<HTMLButtonElement>) => {
    const button = e.currentTarget.getBoundingClientRect();
    setDragOffset({
      x: e.clientX - button.left,
      y: e.clientY - button.top,
    });
    setDragStartPos({ x: e.clientX, y: e.clientY });
    setIsDragging(true);
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLButtonElement>) => {
    if (!isDragging) return;

    const buttonWidth = 40; // w-10 = 2.5rem = 40px
    const buttonHeight = 40; // h-10 = 2.5rem = 40px

    const newX = e.clientX - dragOffset.x;
    const newY = e.clientY - dragOffset.y;

    // 边界限制
    const maxX = window.innerWidth - buttonWidth;
    const maxY = window.innerHeight - buttonHeight;

    setPosition({
      x: Math.max(0, Math.min(newX, maxX)),
      y: Math.max(0, Math.min(newY, maxY)),
    });
  };

  const handlePointerUp = (e: React.PointerEvent<HTMLButtonElement>) => {
    const wasDragging = isDragging;

    // 计算拖动距离
    const dx = Math.abs(e.clientX - dragStartPos.x);
    const dy = Math.abs(e.clientY - dragStartPos.y);
    const isClick = dx < 5 && dy < 5; // 移动小于 5px 视为点击

    setIsDragging(false);
    (e.target as HTMLElement).releasePointerCapture(e.pointerId);

    // 如果不是拖动，则触发点击
    if (!wasDragging || isClick) {
      handleBack();
    }
  };

  return (
    <div className="flex flex-col h-screen bg-white">
      {/* Floating Back Button */}
      <button
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onPointerLeave={handlePointerUp}
        style={{
          position: 'fixed',
          left: position.x,
          top: position.y,
          touchAction: 'none',
          transition: isDragging ? 'none' : 'all 0.2s ease',
          opacity: isDragging ? 0.8 : 1,
          zIndex: 50,
        }}
        className={`flex items-center justify-center w-10 h-10 rounded-full bg-white/90 shadow-md hover:bg-gray-100 ${
          isDragging ? 'shadow-lg' : ''
        }`}
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
