import { ChatMessage } from '../domain/ChatMessage';
import { MessageMarkdownCard } from './MessageMarkdownCard';
import { GenMessageCard } from './GenMessageCard';

@Component
export struct MessageItem {
  @ObjectLink message: ChatMessage;

  build() {
    Column() {
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
      } else {
        Row() {
          Column() {
            if (this.message.isGenMessage || this.message.chatBeforeGen || this.message.plan || this.message.previewUrl) {
              GenMessageCard({
                message: this.message
              })
            } else if (this.message.text && this.message.text.length > 0) {
              MessageMarkdownCard({
                text: this.message.text
              })
            } else {
              LoadingProgress().width(24).height(24).color('#999999')
            }
          }
          .alignItems(HorizontalAlign.Start)
        }
        .width('100%')
        .justifyContent(FlexAlign.Start)
      }
    }
    .width('100%')
    .margin({ bottom: 16 })
  }
}