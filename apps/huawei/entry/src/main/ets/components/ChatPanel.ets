import { aiChatApi } from '../api/aiChat';
import { Markdown } from '@luvi/lv-markdown-in';
import { AiChatRequestModel } from '../model/aiChatModel';

@Observed
export class ChatMessage {
  id: number;
  text: string;
  isUser: boolean;

  constructor(id: number, text: string, isUser: boolean) {
    this.id = id;
    this.text = text;
    this.isUser = isUser;
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
          // Debug Text to verify data flow - KEEP THIS until resolved
          // Text(`[Debug] Len: ${this.message.text.length}`).fontSize(10).fontColor(Color.Red)
          
          if (this.message.text.length > 0) {
            Markdown({
              text: this.message.text,
              mode: 'text'
            })
          } else {
             LoadingProgress().width(24).height(24).color('#999999')
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
  @State messages: ChatMessage[] = [
    new ChatMessage(1, '你好！我是元创AI助手，很高兴为你服务。我可以帮你创建各种元应用，你有什么想法吗？', false),
    new ChatMessage(2, '我想做一个番茄钟应用', true),
    new ChatMessage(3, '很好的想法！番茄钟是一个很实用的生产力工具。让我来帮你规划一下：\n\n1. 核心功能：25分钟工作计时和5分钟休息提醒\n2. 可以添加任务列表，记录每个任务完成的番茄钟数量\n3. 统计功能，展示每天/每周的专注时长\n4. 自定义时长设置\n\n你觉得这个方案怎么样？需要调整什么吗？', false),
    new ChatMessage(4, '听起来不错，能加上白噪音功能吗？', true),
    new ChatMessage(5, '当然可以！我会在应用中集成白噪音功能，提供多种环境音供你选择，比如雨声、咖啡厅、森林等，帮助你更好地专注。现在开始为你生成这个应用吧！', false)
  ];
  @State inputText: string = '';
  @State lastMsgUpdateTrigger: number = 0; // Force UI update trigger
  private scroller: Scroller = new Scroller(); // Add scroller

  private async send(): Promise<void> {
    const content = this.inputText.trim();
    if (content.length === 0) {
      return;
    }
    this.messages.push(new ChatMessage(this.messages.length + 1, content, true));
    this.inputText = '';

    // Placeholder for AI response
    const botMsgId = this.messages.length + 1;
    this.messages.push(new ChatMessage(botMsgId, '', false));
    this.scrollToBottom();

    let currentText = ''; // Keep track of the full text locally
    let lastUpdateTime = 0;
    const UPDATE_INTERVAL = 100; // Throttle UI updates to every 100ms

    const req = new AiChatRequestModel(content);

    await aiChatApi.sendStreamMessage(
      req,
      (text: string) => {
        currentText += text;
        const now = Date.now();
        console.info(`[ChatPanel] Received text chunk: "${text}", current total length: ${currentText.length}`);
        
          // Throttle updates to avoid UI flickering
          if (now - lastUpdateTime >= UPDATE_INTERVAL) {
            const index = this.messages.findIndex(m => m.id === botMsgId);
            if (index !== -1) {
              console.info(`[ChatPanel] Updating UI, msgId: ${botMsgId}, text length: ${currentText.length}`);
              // Update text
              // Trigger state update by modifying the @Observed object property directly
              this.messages[index].text = currentText;
              lastUpdateTime = now;
              this.scrollToBottom();
            }
          }
        },
        () => {
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
      (err: string) => {
        const index = this.messages.findIndex(m => m.id === botMsgId);
        if (index !== -1) {
          const msg = this.messages[index];
          this.messages[index].text = msg.text ? `${msg.text}\n[Error: ${err}]` : `请求失败：${err}`;
        }
      }
    );
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
            .enabled(this.inputText.trim().length > 0)
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

