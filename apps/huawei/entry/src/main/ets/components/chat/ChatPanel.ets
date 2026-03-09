import { aiChatApi } from '../../api/aiChat';
import { chatSessionApi } from '../../api/chatSession';
import { AiChatRequestModel } from '../../model/aiChatModel';
import { ChatMessage as ApiChatMessage } from '../../model/chatSessionModel';
import { ChatMessage } from './ChatMessageModels';
import { splitGenMessageContent } from './GenMessageParser';
import { ChatStreamDraft } from './ChatStreamDraft';
import { MessageItem } from './MessageItem';
import { toApiUrl } from '../../utils/http/ApiConfig';

@Component
export struct ChatPanel {
  @Prop topPadding?: number;
  @Prop bottomPadding?: number;
  @Prop @Watch('onSessionIdChange') sessionId: string = '';
  @State messages: ChatMessage[] = [];
  @State inputText: string = '';
  @State lastMsgUpdateTrigger: number = 0;
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

    const result = await chatSessionApi.getSessionMessages(this.sessionId);
    if (result.success && result.data) {
      this.messages = result.data.map((msg: ApiChatMessage) => {
        const isUser = msg.role === 'user';
        let previewUrl: string | undefined = undefined;
        let appName: string | undefined = undefined;
        let appDescription: string | undefined = undefined;
        let logoUrl: string | undefined = undefined;
        let plan: string | undefined = undefined;
        let chatBeforeGen: string | undefined = undefined;
        let text: string = msg.content;
        let isGenMessage: boolean = false;
        if (msg.type === 'app' && msg.relatedAppUuid) {
          isGenMessage = true;
          previewUrl = toApiUrl('/api/preview/' + msg.relatedAppUuid);
          appName = msg.relatedAppName;
          appDescription = msg.relatedAppDescription;
          const genContent = splitGenMessageContent(msg.content);
          chatBeforeGen = genContent.chatBeforeGen;
          plan = genContent.plan;
          text = '';
          if (msg.relatedAppLogo && msg.relatedAppLogo.length > 0) {
            const logoName = msg.relatedAppLogo;
            const dotIndex = logoName.lastIndexOf('.');
            const logoUuid = dotIndex > 0 ? logoName.substring(0, dotIndex) : logoName;
            logoUrl = toApiUrl('/api/logo/' + logoUuid);
          }
        }
        return new ChatMessage(msg.id, text, isUser, previewUrl, appName, appDescription, logoUrl, plan, chatBeforeGen, isGenMessage);
      });
      setTimeout(() => {
        this.scrollToBottom();
      }, 100);
    } else {
      console.error('[ChatPanel] Failed to load messages:', result.error);
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

    this.messages.push(new ChatMessage(Date.now(), content, true));
    this.inputText = '';

    const botMsgId = Date.now() + 1;
    this.messages.push(new ChatMessage(botMsgId, '', false));
    this.scrollToBottom();

    const draft = new ChatStreamDraft();
    let lastUpdateTime = 0;
    const UPDATE_INTERVAL = 100;
    const req = new AiChatRequestModel(content, this.sessionId);

    const handleTextChunk = (text: string) => {
      draft.appendTextChunk(text);
      syncMessageState(false);
    };

    const handlePlanChunk = (text: string) => {
      draft.appendPlanChunk(text);
      syncMessageState(false);
    };

    const syncMessageState = (force: boolean) => {
      const now = Date.now();
      if (force || now - lastUpdateTime >= UPDATE_INTERVAL) {
        const index = this.messages.findIndex(m => m.id === botMsgId);
        if (index !== -1) {
          draft.applyToMessage(this.messages[index]);
          lastUpdateTime = now;
          this.scrollToBottom();
        }
      }
    };

    try {
      await aiChatApi.sendStreamMessage(
        req,
        (text: string) => {
          handleTextChunk(text);
        },
        () => {
          this.isSending = false;
          const index = this.messages.findIndex(m => m.id === botMsgId);
          if (index !== -1) {
            const msg = this.messages[index];
            syncMessageState(true);
            if (msg.text !== draft.getText()) {
              console.info(`[ChatPanel] Final flush UI, msgId: ${botMsgId}, text length: ${draft.getText().length}`);
              this.messages[index].text = draft.getText();
            }
            if (msg.chatBeforeGen !== draft.getChatBeforeGen()) {
              this.messages[index].chatBeforeGen = draft.getChatBeforeGen();
            }
            if (msg.plan !== draft.getPlan()) {
              this.messages[index].plan = draft.getPlan();
            }
            this.scrollToBottom();
          }
          console.info('Stream completed');
        },
        (err: string) => {
          this.isSending = false;
          const index = this.messages.findIndex(m => m.id === botMsgId);
          if (index !== -1) {
            const msg = this.messages[index];
            this.messages[index].text = msg.text ? `${msg.text}\n[Error: ${err}]` : `请求失败：${err}`;
          }
        },
        (text: string) => {
          handlePlanChunk(text);
        },
        (intent: string) => {
          draft.setIntent(intent);
          console.info(`[ChatPanel] Intent: ${intent}`);
        },
        (appName?: string, appDescription?: string) => {
          const index = this.messages.findIndex(m => m.id === botMsgId);
          if (index !== -1) {
            this.messages[index].appName = appName;
            this.messages[index].appDescription = appDescription;
            this.messages[index].isGenMessage = true;
            this.scrollToBottom();
          }
        },
        (logoUrl: string) => {
          const index = this.messages.findIndex(m => m.id === botMsgId);
          if (index !== -1) {
            this.messages[index].logoUrl = logoUrl;
            this.messages[index].isGenMessage = true;
            this.scrollToBottom();
          }
        },
        (previewUrl: string, appName?: string, appDescription?: string, logoUrl?: string) => {
          console.info(`[ChatPanel] App Generated: ${previewUrl}, name: ${appName}`);
          const index = this.messages.findIndex(m => m.id === botMsgId);
          if (index !== -1) {
            this.messages[index].previewUrl = previewUrl;
            this.messages[index].appName = appName;
            this.messages[index].appDescription = appDescription;
            this.messages[index].logoUrl = logoUrl;
            this.messages[index].isGenMessage = true;
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