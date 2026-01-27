import { http, ApiResponse } from '../utils/http/HttpManager';
import { rcp } from '@kit.RemoteCommunicationKit';
import { BusinessError } from '@kit.BasicServicesKit';
import util from '@ohos.util';
import { tokenService } from '../model/auth';
import { AiChatRequest, AiChatReply, AiChatModel, AiChatApiResponse } from '../model/aiChatModel';

// 定义 SSE 回调接口
interface SSEListener {
  onMessage: (data: string) => void;
  onError: (error: string) => void;
  onDone?: () => void;
}

// 自定义 WriteStream 用于接收流数据
class SSEWriteStream implements rcp.WriteStream {
  private decoder: util.TextDecoder;
  private buffer: string = '';
  private listener: SSEListener;

  constructor(listener: SSEListener) {
    this.listener = listener;
    // 创建文本解码器，处理 UTF-8 字符流
    this.decoder = util.TextDecoder.create('utf-8', { ignoreBOM: true });
  }

  // 当收到网络数据时，此方法会被 rcp 自动调用
  async write(buffer: ArrayBuffer): Promise<number | void> {
    // 1. 解码二进制数据为字符串 (stream: true 保持解码上下文，处理多字节字符跨包问题)
    const chunk = this.decoder.decodeToString(new Uint8Array(buffer), { stream: true });
    this.buffer += chunk;
    console.info(`[SSEWriteStream] Received chunk, buffer size: ${this.buffer.length}`);

    // 2. 处理 SSE 消息 (以 \n\n 分隔)
    // 兼容 CRLF (\r\n) 的情况
    this.buffer = this.buffer.replace(/\r\n/g, '\n');
    let eventEndIndex: number;
    while ((eventEndIndex = this.buffer.indexOf('\n\n')) !== -1) {
      // 提取一条完整的消息
      const messageChunk = this.buffer.substring(0, eventEndIndex);
      // 更新缓冲区，移除已处理的部分
      this.buffer = this.buffer.substring(eventEndIndex + 2);

      // 3. 解析 data: 字段
      this.parseSSEMessage(messageChunk);
    }
    
    return buffer.byteLength;
  }

  private parseSSEMessage(chunk: string) {
    const lines = chunk.split('\n');
    let eventName = '';
    let dataBuffer: string[] = [];

    for (const line of lines) {
      // Trim start to handle potential indentation in the stream (robustness)
      const trimmedLine = line.trimStart();
      
      if (trimmedLine.startsWith('event:')) {
        eventName = trimmedLine.substring(6).trim();
      } else if (trimmedLine.startsWith('data:')) {
        // Extract content after "data:"
        let content = trimmedLine.substring(5);
        
        // Spec: remove optional single leading space
        // Fix for "missing space" issue: 
        // If content is exactly one space (" "), it means the payload is a space token (data: ), 
        // so we should PRESERVE it instead of stripping it to empty string.
        // For longer content (e.g. " data:  Title"), we still strip the first space per spec.
        if (content.startsWith(' ') && content.length > 1) {
          content = content.substring(1);
        }
        
        // IMPORTANT: Do NOT trim the end of content to preserve trailing spaces/newlines
        dataBuffer.push(content);
      }
    }

    const eventData = dataBuffer.join('\n');

    console.info(`[SSEWriteStream] Parsed event - Name: '${eventName}', Data length: ${eventData.length}`);

    if ((!eventName || eventName === 'message') && eventData.length > 0) {
      this.listener.onMessage(eventData);
    } else if (eventName === 'done') {
      console.info('[SSEWriteStream] Received DONE event');
      if (this.listener.onDone) this.listener.onDone();
    }
  }
}

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

  async sendStreamMessage(
    req: AiChatRequest,
    onMessage: (text: string) => void,
    onDone: () => void,
    onError: (err: string) => void
  ): Promise<void> {
    console.info('[AiChatApi] ======== Send Message (stream - rcp) ========');

    const validation = AiChatModel.validateRequest(req);
    if (!validation.valid) {
      onError(validation.error || 'Invalid request');
      return;
    }

    const token = await tokenService.getToken();
    const dto = AiChatModel.toDTO(req);
    // Explicitly ensure sessionId is passed if available
    if (req.sessionId) {
      dto.sessionId = req.sessionId;
    }
    
    // Using the specific agent chat endpoint for streaming
    const url = 'http://192.168.1.118:8080/api/ai/agent/chat';
    console.info('[AiChatApi] Stream URL:', url);

    // Define Header Interceptor to inject headers for session.post
    class HeaderInterceptor implements rcp.Interceptor {
      async intercept(context: rcp.RequestContext, next: rcp.RequestHandler): Promise<rcp.Response> {
        // Create a new headers object or modify existing one safely without spread syntax
        if (!context.request.headers) {
          context.request.headers = {};
        }
        context.request.headers['Content-Type'] = 'application/json';
        context.request.headers['Accept'] = 'text/event-stream';
        context.request.headers['Authorization'] = `Bearer ${token}`;
        
        return next.handle(context);
      }
    }

    const session = rcp.createSession({
      interceptors: [new HeaderInterceptor()]
    });
    
    // Create custom write stream listener
    const sseListener: SSEListener = {
      onMessage: onMessage,
      onError: onError,
      onDone: () => {
        onDone();
        session.close();
      }
    };

    const sseStream = new SSEWriteStream(sseListener);

    try {
      console.info('[AiChatApi] Starting stream request...');
      // session.post(url, content, destination)
      await session.post(url, JSON.stringify(dto), { 
        kind: 'stream', 
        stream: sseStream 
      });
      console.info('[AiChatApi] Stream request finished (session closed)');
    } catch (err) {
      const errorObj = err as BusinessError;
      console.error('[AiChatApi] Stream request exception:', JSON.stringify(errorObj), errorObj.message);
      onError(errorObj.message || 'Network request failed');
    } finally {
      // Ensure session is closed if not already
      session.close();
    }
  }
}

export const aiChatApi = new AiChatApi();

