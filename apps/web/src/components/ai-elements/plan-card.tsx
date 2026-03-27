import { Streamdown } from "streamdown";
import "streamdown/styles.css";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";
import { useRef, useEffect } from "react";

interface PlanCardProps {
  plan: string;
  isAnimating?: boolean;
}

export function PlanCard({ plan, isAnimating }: PlanCardProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  // 流式输出时自动滚动到底部
  useEffect(() => {
    if (isAnimating && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [plan, isAnimating]);

  return (
    <div className="mt-2 max-w-lg rounded-2xl border border-[#E8F0FE] bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <span className="text-sm font-bold text-[#007AFF]">应用计划</span>
        <span className="text-xs text-[#007AFF]/60 bg-[#E8F0FE] px-2 py-0.5 rounded-full">PLAN</span>
      </div>
      {plan ? (
        <div ref={scrollRef} className="max-h-[220px] overflow-y-auto">
          <Streamdown
            plugins={{
              code,
              mermaid,
              math,
              cjk,
            }}
            animated
            isAnimating={isAnimating}
          >
            {plan}
          </Streamdown>
        </div>
      ) : (
        <span className="text-xs text-gray-400">计划生成中...</span>
      )}
    </div>
  );
}