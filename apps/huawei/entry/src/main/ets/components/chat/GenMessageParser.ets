import { GenMessageContent } from './ChatMessageModels';

export function splitGenMessageContent(content: string): GenMessageContent {
  const normalized = content.replace(/\r\n/g, '\n').trim();
  if (normalized.length === 0) {
    return new GenMessageContent('', '');
  }

  if (normalized.startsWith('- ')) {
    return new GenMessageContent('', normalized);
  }

  const separatorIndex = normalized.indexOf('\n\n');
  if (separatorIndex === -1) {
    return new GenMessageContent(normalized, '');
  }

  return new GenMessageContent(
    normalized.substring(0, separatorIndex).trim(),
    normalized.substring(separatorIndex + 2).trim()
  );
}