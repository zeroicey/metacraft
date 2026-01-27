import { ApiResponse } from '../utils/http/HttpManager';
export interface AiChatRequest {
  message: string;
  sessionId?: string;
}

export interface AiChatDTO {
  message: string;
  stream: boolean;
  sessionId?: string;
}

export interface AiChatReply {
  reply: string;
  model: string;
}

export type AiChatApiResponse = ApiResponse<AiChatReply>;

export interface AiChatValidationResult {
  valid: boolean;
  error?: string;
}

export class AiChatRequestModel implements AiChatRequest {
  message: string;
  sessionId?: string;
  
  constructor(message: string, sessionId?: string) {
    this.message = message;
    this.sessionId = sessionId;
  }
}

export class AiChatModel {
  static validateRequest(req: AiChatRequest): AiChatValidationResult {
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
