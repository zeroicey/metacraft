import { aiChatApi } from '../api/aiChat';
import { chatSessionApi } from '../api/chatSession';
import { Markdown } from '@luvi/lv-markdown-in';
import { AiChatRequestModel } from '../model/aiChatModel';
import { ChatMessage as ApiChatMessage } from '../model/chatSessionModel';
import { AppPreviewCard } from './AppPreviewCard';

@Observed
export class ChatMessage {
  id: number;
  text: string;
  isUser: boolean;
  previewUrl?: string;

  constructor(id: number, text: string, isUser: boolean, previewUrl?: string) {
    this.id = id;
    this.text = text;
    this.isUser = isUser;
    this.previewUrl = previewUrl;
  }
}

@Component
struct MessageItem {
  @ObjectLink message: ChatMessage;

  build() {
    if (this.message.isUser) {
      Row() {
        Column() {
          Text(this.message.text)
            .fontSize(15)
            .fontColor('#FFFFFF')
            .padding({ top: 10, bottom: 10, left: 14, right: 14 })
            .backgroundColor('#007AFF')
            .borderRadius(16)
            .maxLines(999)
        }
        .alignItems(HorizontalAlign.End)
      }
      .width('100%')
      .justifyContent(FlexAlign.End)
      .margin({ bottom: 16 })
    } else {
      Row() {
        Column() {
          // Safe check for message text
          if (this.message.text && this.message.text.length > 0) {
            Markdown({
              text: this.message.text,
              mode: 'text'
            })
          } else {
            LoadingProgress().width(24).height(24).color('#999999')
          }

          if (this.message.previewUrl) {
            AppPreviewCard({ previewUrl: this.message.previewUrl })
          }
        }
        .alignItems(HorizontalAlign.Start)
      }
      .width('100%')
      .justifyContent(FlexAlign.Start)
      .margin({ bottom: 16 })
    }
  }
}

@Component
export struct ChatPanel {
  @Prop topPadding?: number;
  @Prop bottomPadding?: number;
  @Prop @Watch('onSessionIdChange') sessionId: string = '';
  @State messages: ChatMessage[] = [];
  @State inputText: string = '';
  @State lastMsgUpdateTrigger: number = 0; // Force UI update trigger
  @State isSending: boolean = false;
  private scroller: Scroller = new Scroller(); // Add scroller

  aboutToAppear() {
    this.loadHistory();
  }

  onSessionIdChange() {
    this.isSending = false;
    this.loadHistory();
  }

  async loadHistory() {
    if (!this.sessionId) {
      this.messages = [];
      return;
    }

    const result = await chatSessionApi.getSessionMessages(this.sessionId);
    if (result.success && result.data) {
      this.messages = result.data.map((msg: ApiChatMessage) => {
        // 后端 role: "user" | "assistant"
        const isUser = msg.role === 'user';
        return new ChatMessage(msg.id, msg.content, isUser);
      });
      // Scroll to bottom after loading
      setTimeout(() => {
        this.scrollToBottom();
      }, 100);
    } else {
      console.error('[ChatPanel] Failed to load messages:', result.error);
      // Optional: show error toast
    }
  }

  private async send(): Promise<void> {
    const content = this.inputText.trim();
    if (content.length === 0) {
      return;
    }

    if (this.isSending) {
      return;
    }
    this.isSending = true;
    
    // Optimistic update
    this.messages.push(new ChatMessage(Date.now(), content, true));
    this.inputText = '';

    // Placeholder for AI response
    const botMsgId = Date.now() + 1;
    this.messages.push(new ChatMessage(botMsgId, '', false));
    this.scrollToBottom();

    let currentText = ''; // Keep track of the full text locally
    let lastUpdateTime = 0;
    const UPDATE_INTERVAL = 100; // Throttle UI updates to every 100ms

    // Pass sessionId if available
    const req = new AiChatRequestModel(content, this.sessionId);

    // Helper to handle text chunks (chat or plan)
    const handleTextChunk = (text: string) => {
      currentText += text;
      const now = Date.now();
      // console.info(`[ChatPanel] Received text chunk: "${text}", current total length: ${currentText.length}`);

      // Throttle updates to avoid UI flickering
      if (now - lastUpdateTime >= UPDATE_INTERVAL) {
        const index = this.messages.findIndex(m => m.id === botMsgId);
        if (index !== -1) {
          // console.info(`[ChatPanel] Updating UI, msgId: ${botMsgId}, text length: ${currentText.length}`);
          this.messages[index].text = currentText;
          lastUpdateTime = now;
          this.scrollToBottom();
        }
      }
    };

    try {
      await aiChatApi.sendStreamMessage(
        req,
        // onMessage
        (text: string) => {
          handleTextChunk(text);
        },
        // onDone
        () => {
          this.isSending = false;
          // Flush any remaining text update
          const index = this.messages.findIndex(m => m.id === botMsgId);
          if (index !== -1) {
            const msg = this.messages[index];
            // Only update if there's a difference to avoid unnecessary render
            if (msg.text !== currentText) {
              console.info(`[ChatPanel] Final flush UI, msgId: ${botMsgId}, text length: ${currentText.length}`);
              this.messages[index].text = currentText;
            }
            this.scrollToBottom();
          }
          console.info('Stream completed');
        },
        // onError
        (err: string) => {
          this.isSending = false;
          const index = this.messages.findIndex(m => m.id === botMsgId);
          if (index !== -1) {
            const msg = this.messages[index];
            this.messages[index].text = msg.text ? `${msg.text}\n[Error: ${err}]` : `请求失败：${err}`;
          }
        },
        // onPlan
        (text: string) => {
          handleTextChunk(text);
        },
        // onIntent
        (intent: string) => {
           console.info(`[ChatPanel] Intent: ${intent}`);
        },
        // onAppGenerated
        (url: string) => {
           console.info(`[ChatPanel] App Generated: ${url}`);
           const index = this.messages.findIndex(m => m.id === botMsgId);
           if (index !== -1) {
             this.messages[index].previewUrl = url;
             this.scrollToBottom();
           }
        }
      );
    } catch (error) {
      this.isSending = false;
      console.error('[ChatPanel] Send exception:', error);
      const index = this.messages.findIndex(m => m.id === botMsgId);
      if (index !== -1) {
        this.messages[index].text += `\n[Exception: ${JSON.stringify(error)}]`;
      }
    }
  }

  scrollToBottom() {
    this.scroller.scrollEdge(Edge.Bottom);
  }

  build() {
    Column() {
      Scroll(this.scroller) {
        Column() {
          Text('内容由AI生成')
            .fontSize(12)
            .fontColor('#999999')
            .width('100%')
            .textAlign(TextAlign.Center)
            .margin({ bottom: 16 })

          ForEach(this.messages, (m: ChatMessage) => {
            MessageItem({ message: m })
          }, (m: ChatMessage) => m.id.toString())
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16, bottom: 16 })
      }
      .layoutWeight(1)
      .edgeEffect(EdgeEffect.Spring)
      .align(Alignment.Top)

      Column() {
        Row() {
          TextInput({ placeholder: '输入你的想法...', text: this.inputText })
            .layoutWeight(1)
            .height(40)
            .backgroundColor('#F5F5F5')
            .borderRadius(10)
            .padding({ left: 16, right: 16 })
            .onChange((v: string) => this.inputText = v)

          Button('发送')
            .height(40)
            .backgroundColor($r('sys.color.ohos_id_color_emphasize'))
            .borderRadius(10)
            .margin({ left: 8 })
            .enabled(!this.isSending && this.inputText.trim().length > 0)
            .onClick(() => { this.send(); })
        }
        .width('100%')
      }
      .width('100%')
      .padding({ left: 16, right: 16, bottom: (this.bottomPadding ?? 8) + 5, top: this.topPadding ?? 8 })
      .backgroundColor(Color.White)
    }
    .width('100%')
    .height('100%')
  }
}

