import { ChatMessage as SessionChatMessage } from '../../session/domain/Session';
import { ChatMessage } from '../domain/ChatMessage';
import { splitGenMessageContent } from './GenMessageParser';
import { toApiUrl } from '../../../core/http/ApiConfig';

function resolveLogoUrl(relatedAppLogo?: string): string | undefined {
  if (!relatedAppLogo || relatedAppLogo.length === 0) {
    return undefined;
  }
  const dotIndex = relatedAppLogo.lastIndexOf('.');
  const logoUuid = dotIndex > 0 ? relatedAppLogo.substring(0, dotIndex) : relatedAppLogo;
  return toApiUrl('/api/logo/' + logoUuid);
}

export function mapSessionMessageToChatMessage(msg: SessionChatMessage): ChatMessage {
  const isUser = msg.role === 'user';
  let previewUrl: string | undefined = undefined;
  let appName: string | undefined = undefined;
  let appDescription: string | undefined = undefined;
  let logoUrl: string | undefined = undefined;
  let plan: string | undefined = undefined;
  let chatBeforeGen: string | undefined = undefined;
  let text: string = msg.content;
  let isGenMessage: boolean = false;

  if (msg.type === 'app' && msg.relatedAppUuid) {
    isGenMessage = true;
    previewUrl = toApiUrl('/api/preview/' + msg.relatedAppUuid);
    appName = msg.relatedAppName;
    appDescription = msg.relatedAppDescription;
    const genContent = splitGenMessageContent(msg.content);
    chatBeforeGen = genContent.chatBeforeGen;
    plan = genContent.plan;
    text = '';
    logoUrl = resolveLogoUrl(msg.relatedAppLogo);
  }

  return new ChatMessage(msg.id, text, isUser, previewUrl, appName, appDescription, logoUrl, plan, chatBeforeGen, isGenMessage);
}