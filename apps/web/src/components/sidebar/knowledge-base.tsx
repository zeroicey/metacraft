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
            className={`w-full flex items-center gap-2 px-2 py-2 rounded-lg transition-colors ${
              selectedId === kb.id
                ? "bg-[#FDF2F8] border border-[#EC4899]"
                : "hover:bg-gray-50"
            }`}
          >
            <div
              className={`w-7 h-7 rounded-md flex items-center justify-center ${
                selectedId === kb.id
                  ? "bg-gradient-to-br from-[#EC4899] to-[#8B5CF6]"
                  : "bg-gray-200"
              }`}
            >
              <BookIcon
                className={`w-3.5 h-3.5 ${
                  selectedId === kb.id ? "text-white" : "text-gray-500"
                }`}
              />
            </div>
            <div className="flex-1 text-left">
              <div
                className={`text-xs ${
                  selectedId === kb.id ? "font-medium text-gray-900" : "text-gray-700"
                }`}
              >
                {kb.name}
              </div>
              <div className="text-[10px] text-gray-400">{kb.articleCount} 篇文章</div>
            </div>
            <ChevronRightIcon className="w-3 h-3 text-gray-400" />
          </button>
        ))}
      </div>

      <div className="px-3 pb-3">
        <button className="w-full py-2 bg-[#EC4899] hover:bg-[#BE185D] text-white text-xs font-medium rounded-lg flex items-center justify-center gap-1 transition-colors">
          <PlusIcon className="w-3.5 h-3.5" />
          新建知识库
        </button>
      </div>
    </div>
  );
}