import { SSEClient } from '../../../core/http/SSEClient';
import { toApiUrl } from '../../../core/http/ApiConfig';
import { AiChatRequest } from '../types';
import { AiChatRequestMapper } from '../request/AiChatRequestMapper';

class ChatStreamService {
  async sendStreamMessage(
    req: AiChatRequest,
    onMessage: (text: string) => void,
    onDone: () => void,
    onError: (err: string) => void,
    onPlan?: (text: string) => void,
    onIntent?: (intent: string) => void,
    onAppInfo?: (appName?: string, appDescription?: string) => void,
    onLogoGenerated?: (logoUrl: string) => void,
    onAppGenerated?: (url: string, appName?: string, appDescription?: string, logoUrl?: string) => void
  ): Promise<void> {
    console.info('[ChatStreamService] ======== Send Message (stream - rcp) ========');

    const validation = AiChatRequestMapper.validate(req);
    if (!validation.valid) {
      onError(validation.error || 'Invalid request');
      return;
    }

    const dto = AiChatRequestMapper.toDTO(req);
    if (req.sessionId) {
      dto.sessionId = req.sessionId;
    }

    const url = toApiUrl('/api/ai/agent/unified');
    console.info('[ChatStreamService] Stream URL:', url);

    const client = new SSEClient({
      onMessage: onMessage,
      onError: onError,
      onDone: onDone,
      onPlan: onPlan,
      onIntent: onIntent,
      onAppInfo: onAppInfo,
      onLogoGenerated: onLogoGenerated,
      onAppGenerated: onAppGenerated
    });

    await client.connect(url, dto);
  }
}

export const chatStreamService = new ChatStreamService();