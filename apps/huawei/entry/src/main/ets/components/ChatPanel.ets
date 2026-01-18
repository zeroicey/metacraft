import { aiChatApi } from '../api/aiChat';
import { Markdown } from '@luvi/lv-markdown-in';

interface ChatMessage {
  id: number;
  text: string;
  isUser: boolean;
}

@Component
export struct ChatPanel {
  @Prop topPadding?: number;
  @Prop bottomPadding?: number;
  @State messages: ChatMessage[] = [
    { id: 1, text: '你好！我是元创AI助手，很高兴为你服务。我可以帮你创建各种元应用，你有什么想法吗？', isUser: false },
    { id: 2, text: '我想做一个番茄钟应用', isUser: true },
    { id: 3, text: '很好的想法！番茄钟是一个很实用的生产力工具。让我来帮你规划一下：\n\n1. 核心功能：25分钟工作计时和5分钟休息提醒\n2. 可以添加任务列表，记录每个任务完成的番茄钟数量\n3. 统计功能，展示每天/每周的专注时长\n4. 自定义时长设置\n\n你觉得这个方案怎么样？需要调整什么吗？', isUser: false },
    { id: 4, text: '听起来不错，能加上白噪音功能吗？', isUser: true },
    { id: 5, text: '当然可以！我会在应用中集成白噪音功能，提供多种环境音供你选择，比如雨声、咖啡厅、森林等，帮助你更好地专注。现在开始为你生成这个应用吧！', isUser: false }
  ];
  @State inputText: string = '';

  private async send(): Promise<void> {
    const content = this.inputText.trim();
    if (content.length === 0) {
      return;
    }
    this.messages.push({ id: this.messages.length + 1, text: content, isUser: true });
    this.inputText = '';
    const result = await aiChatApi.sendMessage({ message: content });
    if (result.success && result.data) {
      this.messages.push({ id: this.messages.length + 1, text: result.data.reply, isUser: false });
    } else {
      this.messages.push({ id: this.messages.length + 1, text: `请求失败：${result.error ?? '未知错误'}`, isUser: false });
    }
  }

  @Builder
  buildMessageItem(message: ChatMessage) {
    if (message.isUser) {
      Row() {
        Column() {
          Text(message.text)
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
        Markdown({
          text: message.text,
          mode: 'text'
        })
      }
      .width('100%')
      .justifyContent(FlexAlign.Start)
      .margin({ bottom: 16 })
    }
  }

  build() {
    Column() {
      Scroll() {
        Column() {
          ForEach(this.messages, (m: ChatMessage) => {
            this.buildMessageItem(m);
          })
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

        Text('内容由AI生成')
          .fontSize(12)
          .fontColor('#999999')
          .width('100%')
          .textAlign(TextAlign.Center)
          .margin({ top: 8 })
      }
      .width('100%')
      .padding({ left: 16, right: 16, bottom: this.bottomPadding ?? 8, top: this.topPadding ?? 8 })
      .backgroundColor(Color.White)
    }
    .width('100%')
    .height('100%')
  }
}

