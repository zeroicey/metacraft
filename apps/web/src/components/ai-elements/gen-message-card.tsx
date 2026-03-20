import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";
import { PlanCard } from "./plan-card";
import { AppInfoCard } from "./app-info-card";
import { AppPreviewCard } from "./app-preview-card";

interface GenMessageCardProps {
  chatBeforeGen?: string;
  plan?: string;
  appName?: string;
  appDescription?: string;
  logoUrl?: string;
  previewUrl?: string;
  isStreaming?: boolean;
}

export function GenMessageCard({
  chatBeforeGen,
  plan,
  appName,
  appDescription,
  logoUrl,
  previewUrl,
  isStreaming = false,
}: GenMessageCardProps) {
  return (
    <div className="flex flex-col gap-2">
      {/* 聊天内容 */}
      {chatBeforeGen && chatBeforeGen.length > 0 && (
        <div className="text-sm text-gray-700">
          <Streamdown
            plugins={{
              code,
              mermaid,
              math,
              cjk,
            }}
            isAnimating={isStreaming}
          >
            {chatBeforeGen}
          </Streamdown>
        </div>
      )}

      {/* 计划 */}
      {plan && plan.length > 0 && <PlanCard plan={plan} />}

      {/* 应用信息 */}
      {(appName || appDescription || logoUrl) && (
        <AppInfoCard
          appName={appName}
          appDescription={appDescription}
          logoUrl={logoUrl}
        />
      )}

      {/* 预览 */}
      {previewUrl && (
        <AppPreviewCard
          previewUrl={previewUrl}
          appName={appName}
          logoUrl={logoUrl}
        />
      )}
    </div>
  );
}