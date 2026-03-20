import { Streamdown } from "streamdown";
import { code } from "@streamdown/code";
import { mermaid } from "@streamdown/mermaid";
import { math } from "@streamdown/math";
import { cjk } from "@streamdown/cjk";

interface PlanCardProps {
  plan: string;
}

export function PlanCard({ plan }: PlanCardProps) {
  return (
    <div className="mt-2 rounded-xl border border-[#D9E5FF] bg-[#F6F9FF] p-3">
      <div className="mb-2 flex items-center justify-between">
        <span className="text-sm font-bold text-[#2F5DFF]">应用计划</span>
        <span className="text-xs text-[#7C8AA5]">PLAN</span>
      </div>
      {plan ? (
        <div className="max-h-[220px] overflow-y-auto">
          <Streamdown
            plugins={{
              code,
              mermaid,
              math,
              cjk,
            }}
          >
            {plan}
          </Streamdown>
        </div>
      ) : (
        <span className="text-xs text-[#8E8E93]">计划生成中...</span>
      )}
    </div>
  );
}