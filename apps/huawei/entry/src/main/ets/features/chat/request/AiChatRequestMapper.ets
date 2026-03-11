import { ApiResponse } from '../../../core/http/HttpManager';
import { AiChatRequest, AiChatDTO, AiChatReply, AiChatValidationResult } from '../types';

export type AiChatApiResponse = ApiResponse<AiChatReply>;

export class AiChatRequestModel implements AiChatRequest {
  message: string;
  sessionId?: string;

  constructor(message: string, sessionId?: string) {
    this.message = message;
    this.sessionId = sessionId;
  }
}

export class AiChatRequestMapper {
  static validate(req: AiChatRequest): AiChatValidationResult {
    if (!req || typeof req.message !== 'string') {
      return { valid: false, error: 'Invalid request: message is required' };
    }
    const msg = req.message.trim();
    if (msg.length === 0) {
      return { valid: false, error: 'Message cannot be empty' };
    }
    if (msg.length > 2000) {
      return { valid: false, error: 'Message length must be <= 2000 characters' };
    }
    return { valid: true };
  }

  static toDTO(req: AiChatRequest): AiChatDTO {
    return {
      message: req.message.trim(),
      stream: false,
      sessionId: req.sessionId
    };
  }

  static fromResponse(res: AiChatApiResponse): AiChatReply | null {
    return res?.data ?? null;
  }
}