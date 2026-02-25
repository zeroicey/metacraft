import { rcp } from '@kit.RemoteCommunicationKit';
import { BusinessError } from '@kit.BasicServicesKit';
import util from '@ohos.util';
import { AuthInterceptor } from './Interceptors';

/**
 * SSE 回调接口
 */
export interface SSEListener {
  /**
   * 收到消息时触发
   * @param data 消息内容
   */
  onMessage: (data: string) => void;
  
  /**
   * 发生错误时触发
   * @param error 错误信息
   */
  onError: (error: string) => void;
  
  /**
   * 流结束时触发
   */
  onDone?: () => void;

  /**
   * 收到意图识别结果时触发
   * @param intent 意图类型 (chat | gen)
   */
  onIntent?: (intent: string) => void;

  /**
   * 收到计划内容时触发 (用于 gen 模式)
   * @param data 计划内容片段
   */
  onPlan?: (data: string) => void;

  /**
   * 应用生成完毕时触发
   * @param previewUrl 预览链接
   */
  onAppGenerated?: (previewUrl: string) => void;
}

/**
 * 自定义 WriteStream 用于接收 SSE 流数据
 */
class SSEWriteStream implements rcp.WriteStream {
  private decoder: util.TextDecoder;
  private buffer: string = '';
  private listener: SSEListener;

  constructor(listener: SSEListener) {
    this.listener = listener;
    // 创建文本解码器，处理 UTF-8 字符流，ignoreBOM 处理字节顺序标记
    this.decoder = util.TextDecoder.create('utf-8', { ignoreBOM: true });
  }

  // 当收到网络数据时，此方法会被 rcp 自动调用
  async write(buffer: ArrayBuffer): Promise<number | void> {
    // 1. 解码二进制数据为字符串 (stream: true 保持解码上下文，处理多字节字符跨包问题)
    const chunk = this.decoder.decodeToString(new Uint8Array(buffer), { stream: true });
    this.buffer += chunk;

    // 2. 处理 SSE 消息 (以 \n\n 分隔)
    // 兼容 CRLF (\r\n) 的情况
    this.buffer = this.buffer.replace(/\r\n/g, '\n');
    let eventEndIndex: number;
    
    // 循环处理缓冲区中的所有完整消息块
    while ((eventEndIndex = this.buffer.indexOf('\n\n')) !== -1) {
      // 提取一条完整的消息
      const messageChunk = this.buffer.substring(0, eventEndIndex);
      // 更新缓冲区，移除已处理的部分 (跳过 \n\n)
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
        
        // Standard SSE spec: remove single optional leading space
        if (content.startsWith(' ')) {
          content = content.substring(1);
        }

        // Backend sends plain text, no JSON parsing needed
        dataBuffer.push(content);
      }
    }

    const eventData = dataBuffer.join('\n');

    console.info(`[SSEWriteStream] Parsed event - Name: '${eventName}', Data length: ${eventData.length}, Data: '${eventData.replace(/\n/g, '\\n')}'`);

    // 根据事件类型分发
    if (!eventName || eventName === 'message') {
      if (eventData.length > 0) {
        this.listener.onMessage(eventData);
      }
    } else if (eventName === 'plan') {
      if (this.listener.onPlan && eventData.length > 0) {
        this.listener.onPlan(eventData);
      }
    } else if (eventName === 'intent') {
      if (this.listener.onIntent && eventData.length > 0) {
        this.listener.onIntent(eventData);
      }
    } else if (eventName === 'app_generated') {
      if (this.listener.onAppGenerated && eventData.length > 0) {
        this.listener.onAppGenerated(eventData);
      }
    } else if (eventName === 'done') {
      // 收到结束事件
      if (this.listener.onDone) this.listener.onDone();
    }
  }
}

/**
 * SSE 客户端工具类
 * 封装了 rcp 会话管理和流式请求处理
 */
export class SSEClient {
  private session: rcp.Session | null = null;
  private listener: SSEListener;

  constructor(listener: SSEListener) {
    this.listener = listener;
  }

  /**
   * 发起 SSE 连接
   * @param url 请求地址
   * @param body 请求体 (POST)
   */
  public async connect(url: string, body?: object): Promise<void> {
    // 1. 创建 Session
    // 这里创建一个新的 Session 以便独立管理生命周期 (如取消请求)
    // 同时复用 AuthInterceptor 进行统一的 Token 注入
    this.session = rcp.createSession({
      interceptors: [new AuthInterceptor()],
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      },
      requestConfiguration: {
        transfer: {
          timeout: {
            connectMs: 60000, // 60s
            transferMs: 3600000 // 1 hour for long generation tasks
          }
        }
      }
    });

    // 2. 创建自定义流写入器
    const sseStream = new SSEWriteStream({
      onMessage: this.listener.onMessage,
      onError: this.listener.onError,
      onDone: () => {
        // 流结束时，自动关闭会话
        this.close();
        if (this.listener.onDone) {
          this.listener.onDone();
        }
      },
      onPlan: this.listener.onPlan,
      onIntent: this.listener.onIntent,
      onAppGenerated: this.listener.onAppGenerated
    });

    try {
      // 3. 发起流式请求
      const content: rcp.RequestContent = body ? body : {};
      
      // 使用 session.post 并指定 kind: 'stream'
      await this.session.post(url, content, { 
        kind: 'stream', 
        stream: sseStream 
      });
      
    } catch (err) {
      const errorObj = err as BusinessError;
      
      // 1007900992 是 RCP 中 "Request is canceled" 的错误码
      // 当我们在 onDone 中调用 session.close() 时，正在进行的请求会被取消并抛出此错误
      // 这是预期行为，应忽略
      if (errorObj.code === 1007900992) {
        console.info('[SSEClient] Request canceled by client (Normal termination)');
        return;
      }
      
      console.error('[SSEClient] Stream request exception:', JSON.stringify(errorObj), errorObj.message);
      this.listener.onError(errorObj.message || 'Network request failed');
    } finally {
      // 确保会话关闭
      this.close();
    }
  }

  /**
   * 手动关闭连接
   */
  public close(): void {
    if (this.session) {
      try {
        this.session.close();
      } catch (e) {
        // 忽略关闭时的错误
      }
      this.session = null;
    }
  }
}
