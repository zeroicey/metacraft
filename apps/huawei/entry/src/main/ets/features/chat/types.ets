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

export interface AiChatValidationResult {
  valid: boolean;
  error?: string;
}

export interface AiChatResult {
  success: boolean;
  data?: AiChatReply;
  error?: string;
}