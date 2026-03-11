import { ChatMessage } from '../domain/ChatMessage';
import { ChatStreamDraft } from './ChatStreamDraft';

function findMessageIndex(messages: ChatMessage[], messageId: number): number {
  return messages.findIndex(message => message.id === messageId);
}

export function createUserChatMessage(messageId: number, text: string): ChatMessage {
  return new ChatMessage(messageId, text, true);
}

export function createAssistantPlaceholderMessage(messageId: number): ChatMessage {
  return new ChatMessage(messageId, '', false);
}

export function applyDraftSnapshot(messages: ChatMessage[], messageId: number, draft: ChatStreamDraft): boolean {
  const index = findMessageIndex(messages, messageId);
  if (index === -1) {
    return false;
  }
  draft.applyToMessage(messages[index]);
  return true;
}

export function finalizeDraftMessage(messages: ChatMessage[], messageId: number, draft: ChatStreamDraft): boolean {
  const index = findMessageIndex(messages, messageId);
  if (index === -1) {
    return false;
  }

  const message = messages[index];
  draft.applyToMessage(message);
  if (message.text !== draft.getText()) {
    message.text = draft.getText();
  }
  if (message.chatBeforeGen !== draft.getChatBeforeGen()) {
    message.chatBeforeGen = draft.getChatBeforeGen();
  }
  if (message.plan !== draft.getPlan()) {
    message.plan = draft.getPlan();
  }
  return true;
}

export function applyAppInfo(messages: ChatMessage[], messageId: number, appName?: string, appDescription?: string): boolean {
  const index = findMessageIndex(messages, messageId);
  if (index === -1) {
    return false;
  }
  messages[index].appName = appName;
  messages[index].appDescription = appDescription;
  messages[index].isGenMessage = true;
  return true;
}

export function applyLogoUrl(messages: ChatMessage[], messageId: number, logoUrl: string): boolean {
  const index = findMessageIndex(messages, messageId);
  if (index === -1) {
    return false;
  }
  messages[index].logoUrl = logoUrl;
  messages[index].isGenMessage = true;
  return true;
}

export function applyGeneratedApp(messages: ChatMessage[], messageId: number, previewUrl: string, appName?: string, appDescription?: string, logoUrl?: string): boolean {
  const index = findMessageIndex(messages, messageId);
  if (index === -1) {
    return false;
  }
  messages[index].previewUrl = previewUrl;
  messages[index].appName = appName;
  messages[index].appDescription = appDescription;
  messages[index].logoUrl = logoUrl;
  messages[index].isGenMessage = true;
  return true;
}

export function applyStreamError(messages: ChatMessage[], messageId: number, err: string): boolean {
  const index = findMessageIndex(messages, messageId);
  if (index === -1) {
    return false;
  }
  const message = messages[index];
  message.text = message.text ? `${message.text}\n[Error: ${err}]` : `请求失败：${err}`;
  return true;
}

export function applyStreamException(messages: ChatMessage[], messageId: number, error: Object): boolean {
  const index = findMessageIndex(messages, messageId);
  if (index === -1) {
    return false;
  }
  messages[index].text += `\n[Exception: ${JSON.stringify(error)}]`;
  return true;
}