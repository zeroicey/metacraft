import { http, ApiResponse } from '../utils/http/HttpManager';
import { AiChatRequest, AiChatReply, AiChatModel, AiChatApiResponse } from '../model/aiChatModel';

export interface AiChatResult {
  success: boolean;
  data?: AiChatReply;
  error?: string;
}

class AiChatApi {
  async sendMessage(req: AiChatRequest): Promise<AiChatResult> {
    console.info('[AiChatApi] ======== Send Message (non-stream) ========');

    const GeneratedDestructObj_1 = AiChatModel.validateRequest(req);
    const valid = GeneratedDestructObj_1.valid;
    const error = GeneratedDestructObj_1.error;
    if (!valid) {
      console.error('[AiChatApi] Validation failed:', error);
      return { success: false, error };
    }

    const dto = AiChatModel.toDTO(req);
    console.info('[AiChatApi] URL: /api/ai/chat/message');
    console.info('[AiChatApi] Payload:', JSON.stringify(dto));

    try {
      const response = await http.post<ApiResponse<AiChatReply>>('/api/ai/chat/message', dto);
      console.info('[AiChatApi] Response message:', response.message);
      console.info('[AiChatApi] Response error:', response.error);
      console.info('[AiChatApi] Response data:', JSON.stringify(response.data));

      if (response.error) {
        const errMsg = typeof response.error === 'string' ? response.error : response.message || 'Server error';
        console.error('[AiChatApi] Business error:', errMsg);
        return { success: false, error: errMsg };
      }

      const data = AiChatModel.fromResponse(response as AiChatApiResponse);
      if (!data) {
        const msg = response.message || 'Empty response data';
        console.error('[AiChatApi] Empty data');
        return { success: false, error: msg };
      }

      console.info('[AiChatApi] ======== Chat Success ========');
      return { success: true, data };
    } catch (err) {
      const errorObj = err as Error;
      console.error('[AiChatApi] Network/HTTP error:', errorObj.message);
      return { success: false, error: errorObj.message || 'Network request failed' };
    }
  }
}

export const aiChatApi = new AiChatApi();

