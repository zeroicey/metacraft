import { useState } from "react";
import { BookIcon, PlusIcon, ChevronRightIcon } from "lucide-react";

interface KnowledgeBase {
  id: string;
  name: string;
  articleCount: number;
}

const mockKnowledgeBases: KnowledgeBase[] = [
  { id: "1", name: "我的知识库", articleCount: 12 },
  { id: "2", name: "技术文档", articleCount: 8 },
  { id: "3", name: "产品FAQ", articleCount: 24 },
];

export function KnowledgeBaseSidebar() {
  const [selectedId, setSelectedId] = useState<string>("1");

  return (
    <div className="border-t border-[#FCE7F3]">
      <div className="px-3 py-2">
        <div className="text-xs font-medium text-gray-500 uppercase tracking-wider">
          知识库
        </div>
      </div>

      <div className="px-3 pb-2 space-y-1">
        {mockKnowledgeBases.map((kb) => (
          <button
            key={kb.id}
            onClick={() => setSelectedId(kb.id)}
            className={`w-full flex items-center gap-2 px-3 py-2.5 rounded-xl transition-all ${
              selectedId === kb.id
                ? "bg-gradient-to-r from-[#FDF2F8] to-[#FCE7F3] border border-[#EC4899] shadow-sm"
                : "hover:bg-[#F9FAFB] border border-transparent"
            }`}
          >
            <div
              className={`w-8 h-8 rounded-lg flex items-center justify-center ${
                selectedId === kb.id
                  ? "bg-gradient-to-br from-[#EC4899] to-[#8B5CF6] shadow-md"
                  : "bg-gray-200"
              }`}
            >
              <BookIcon
                className={`w-4 h-4 ${
                  selectedId === kb.id ? "text-white" : "text-gray-500"
                }`}
              />
            </div>
            <div className="flex-1 text-left">
              <div
                className={`text-sm ${
                  selectedId === kb.id ? "font-medium text-gray-900" : "text-gray-700"
                }`}
              >
                {kb.name}
              </div>
              <div className="text-[10px] text-gray-400">{kb.articleCount} 篇文章</div>
            </div>
            <ChevronRightIcon className="w-4 h-4 text-gray-400" />
          </button>
        ))}
      </div>
    </div>
  );
}