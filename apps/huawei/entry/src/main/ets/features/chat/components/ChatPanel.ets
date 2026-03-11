import { chatStreamService } from '../service/ChatStreamService';
import { sessionService } from '../../session/service/SessionService';
import { AiChatRequestModel } from '../request/AiChatRequestMapper';
import { ChatMessage as ApiChatMessage } from '../../session/domain/Session';
import { ChatMessage } from '../domain/ChatMessage';
import { mapSessionMessageToChatMessage } from '../adapter/ChatMessageMapper';
import { ChatStreamDraft } from '../state/ChatStreamDraft';
import {
  createUserChatMessage,
  createAssistantPlaceholderMessage,
  applyDraftSnapshot,
  finalizeDraftMessage,
  applyAppInfo,
  applyLogoUrl,
  applyGeneratedApp,
  applyStreamError,
  applyStreamException,
} from '../state/ChatMessageState';
import { MessageItem } from './MessageItem';

@Component
export struct ChatPanel {
  @Prop topPadding?: number;
  @Prop bottomPadding?: number;
  @Prop @Watch('onSessionIdChange') sessionId: string = '';
  @State messages: ChatMessage[] = [];
  @State inputText: string = '';
  @State isSending: boolean = false;
  private scroller: Scroller = new Scroller();

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

    const result = await sessionService.getSessionMessages(this.sessionId);
    if (result.success && result.data) {
      this.messages = result.data.map((msg: ApiChatMessage) => mapSessionMessageToChatMessage(msg));
      setTimeout(() => {
        this.scrollToBottom();
      }, 100);
    } else {
      console.error('[ChatPanel] Failed to load messages:', result.error);
    }
  }

  private async send(): Promise<void> {
    const content = this.inputText.trim();
    if (content.length === 0 || this.isSending) {
      return;
    }
    this.isSending = true;

    this.messages.push(createUserChatMessage(Date.now(), content));
    this.inputText = '';

    const botMsgId = Date.now() + 1;
    this.messages.push(createAssistantPlaceholderMessage(botMsgId));
    this.scrollToBottom();

    const draft = new ChatStreamDraft();
    let lastUpdateTime = 0;
    const UPDATE_INTERVAL = 100;
    const req = new AiChatRequestModel(content, this.sessionId);

    const syncMessageState = (force: boolean) => {
      const now = Date.now();
      if (force || now - lastUpdateTime >= UPDATE_INTERVAL) {
        if (applyDraftSnapshot(this.messages, botMsgId, draft)) {
          lastUpdateTime = now;
          this.scrollToBottom();
        }
      }
    };

    try {
      await chatStreamService.sendStreamMessage(
        req,
        (text: string) => {
          draft.appendTextChunk(text);
          syncMessageState(false);
        },
        () => {
          this.isSending = false;
          syncMessageState(true);
          if (finalizeDraftMessage(this.messages, botMsgId, draft)) {
            this.scrollToBottom();
          }
        },
        (err: string) => {
          this.isSending = false;
          if (applyStreamError(this.messages, botMsgId, err)) {
            this.scrollToBottom();
          }
        },
        (text: string) => {
          draft.appendPlanChunk(text);
          syncMessageState(false);
        },
        (intent: string) => {
          draft.setIntent(intent);
        },
        (appName?: string, appDescription?: string) => {
          if (applyAppInfo(this.messages, botMsgId, appName, appDescription)) {
            this.scrollToBottom();
          }
        },
        (logoUrl: string) => {
          if (applyLogoUrl(this.messages, botMsgId, logoUrl)) {
            this.scrollToBottom();
          }
        },
        (previewUrl: string, appName?: string, appDescription?: string, logoUrl?: string) => {
          if (applyGeneratedApp(this.messages, botMsgId, previewUrl, appName, appDescription, logoUrl)) {
            this.scrollToBottom();
          }
        }
      );
    } catch (error) {
      this.isSending = false;
      console.error('[ChatPanel] Send exception:', error);
      if (applyStreamException(this.messages, botMsgId, error as Object)) {
        this.scrollToBottom();
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