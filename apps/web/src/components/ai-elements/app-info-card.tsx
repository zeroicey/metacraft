interface AppInfoCardProps {
  appName?: string;
  appDescription?: string;
  logoUrl?: string;
}

export function AppInfoCard({
  appName,
  appDescription,
  logoUrl,
}: AppInfoCardProps) {
  const hasContent =
    (appName && appName.length > 0) ||
    (appDescription && appDescription.length > 0) ||
    (logoUrl && logoUrl.length > 0);

  if (!hasContent) {
    return null;
  }

  return (
    <div className="mt-2 max-w-lg rounded-2xl border border-[#E8F0FE] bg-white p-4 shadow-sm">
      <div className="flex items-start gap-3">
        {logoUrl ? (
          <img
            src={logoUrl}
            alt={appName || "App Logo"}
            className="h-12 w-12 rounded-xl object-cover shadow-sm"
          />
        ) : (
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-[#007AFF] to-[#0056CC] shadow-md">
            <span className="text-xl font-bold text-white">
              {appName?.charAt(0) || "A"}
            </span>
          </div>
        )}
        <div className="flex-1">
          <h3 className="font-semibold text-gray-800">{appName || "未命名应用"}</h3>
          {appDescription && (
            <p className="mt-1 text-sm text-gray-500">{appDescription}</p>
          )}
        </div>
      </div>
    </div>
  );
}