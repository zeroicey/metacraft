import { SSEClient } from '../utils/http/SSEClient';
import { AiChatRequest, AiChatReply, AiChatModel } from '../model/aiChatModel';

export interface AiChatResult {
  success: boolean;
  data?: AiChatReply;
  error?: string;
}

class AiChatApi {
  async sendStreamMessage(
    req: AiChatRequest,
    onMessage: (text: string) => void,
    onDone: () => void,
    onError: (err: string) => void,
    onPlan?: (text: string) => void,
    onIntent?: (intent: string) => void,
    onAppGenerated?: (url: string) => void
  ): Promise<void> {
    console.info('[AiChatApi] ======== Send Message (stream - rcp) ========');

    const validation = AiChatModel.validateRequest(req);
    if (!validation.valid) {
      onError(validation.error || 'Invalid request');
      return;
    }

    const dto = AiChatModel.toDTO(req);
    // Explicitly ensure sessionId is passed if available
    if (req.sessionId) {
      dto.sessionId = req.sessionId;
    }
    
    // Using the specific agent chat endpoint for streaming
    const url = 'http://192.168.1.118:8080/api/ai/agent/unified';
    console.info('[AiChatApi] Stream URL:', url);

    // Instantiate SSEClient with callbacks
    const client = new SSEClient({
      onMessage: onMessage,
      onError: onError,
      onDone: onDone,
      onPlan: onPlan,
      onIntent: onIntent,
      onAppGenerated: onAppGenerated
    });

    // Start connection
    await client.connect(url, dto);
  }
}

export const aiChatApi = new AiChatApi();
