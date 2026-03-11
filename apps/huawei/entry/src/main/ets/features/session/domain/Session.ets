export interface ChatSession {
  sessionId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  relatedAppId?: number;
}

export interface ChatMessage {
  id: number;
  sessionId: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  createdAt: string;
  type?: string;
  relatedAppId?: number;
  relatedVersionId?: number;
  relatedAppUuid?: string;
  relatedAppName?: string;
  relatedAppDescription?: string;
  relatedAppLogo?: string;
}