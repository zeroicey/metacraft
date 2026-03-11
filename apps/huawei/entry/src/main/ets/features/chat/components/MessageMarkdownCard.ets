import { Markdown } from '@luvi/lv-markdown-in';

@Component
export struct MessageMarkdownCard {
  @Prop text: string = '';

  build() {
    if (this.text.length > 0) {
      Markdown({
        text: this.text,
        mode: 'text'
      })
    }
  }
}