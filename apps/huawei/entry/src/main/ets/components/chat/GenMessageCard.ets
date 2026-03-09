import { ChatMessage } from './ChatMessageModels';
import { MessageMarkdownCard } from './MessageMarkdownCard';
import { AppPlanCard } from './AppPlanCard';
import { AppInfoCard } from '../app/AppInfoCard';
import { AppWebPreviewCard } from '../app/AppWebPreviewCard';

@Component
export struct GenMessageCard {
  @ObjectLink message: ChatMessage;

  build() {
    Column() {
      if (this.message.chatBeforeGen && this.message.chatBeforeGen.length > 0) {
        MessageMarkdownCard({
          text: this.message.chatBeforeGen
        })
      }

      if (this.message.plan && this.message.plan.length > 0) {
        AppPlanCard({
          plan: this.message.plan
        })
      }

      if ((this.message.appName && this.message.appName.length > 0)
        || (this.message.appDescription && this.message.appDescription.length > 0)
        || (this.message.logoUrl && this.message.logoUrl.length > 0)
        || (this.message.previewUrl && this.message.previewUrl.length > 0)) {
        AppInfoCard({
          appName: this.message.appName,
          appDescription: this.message.appDescription,
          logoUrl: this.message.logoUrl,
          previewReady: !!this.message.previewUrl && this.message.previewUrl.length > 0
        })
      }

      if (this.message.previewUrl) {
        AppWebPreviewCard({
          previewUrl: this.message.previewUrl,
          appName: this.message.appName,
          logoUrl: this.message.logoUrl
        })
      }
    }
    .width('100%')
  }
}