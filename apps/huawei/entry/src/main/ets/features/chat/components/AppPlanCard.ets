import { Markdown } from '@luvi/lv-markdown-in';

@Component
export struct AppPlanCard {
  @Prop plan: string = '';
  private planScroller: Scroller = new Scroller();
  private readonly MAX_PLAN_HEIGHT: number = 220;

  build() {
    Column() {
      Row() {
        Text('应用计划')
          .fontSize(13)
          .fontWeight(FontWeight.Bold)
          .fontColor('#2F5DFF')

        Blank()

        Text('PLAN')
          .fontSize(10)
          .fontColor('#7C8AA5')
      }
      .width('100%')
      .margin({ bottom: 8 })

      if (this.plan.length > 0) {
        Scroll(this.planScroller) {
          Column() {
            Markdown({
              text: this.plan,
              mode: 'text'
            })
          }
          .width('100%')
        }
        .width('100%')
        .constraintSize({ maxHeight: this.MAX_PLAN_HEIGHT })
        .scrollBar(BarState.On)
        .scrollable(ScrollDirection.Vertical)
      } else {
        Text('计划生成中...')
          .fontSize(12)
          .fontColor('#8E8E93')
      }
    }
    .width('100%')
    .padding(12)
    .backgroundColor('#F6F9FF')
    .borderRadius(12)
    .border({ width: 1, color: '#D9E5FF' })
    .margin({ top: 8 })
  }
}