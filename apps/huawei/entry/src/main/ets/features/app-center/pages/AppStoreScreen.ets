import { BasePageHeader } from '../../../shared/ui/BasePageHeader';
import { PagePlaceholder } from '../../../shared/ui/PagePlaceholder';

@ComponentV2
export struct AppStoreScreen {
  topRectHeight: number = 0;

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
  }

  build() {
    Column() {
      BasePageHeader({
        title: '元应用商店',
        topRectHeight: this.getUIContext().px2vp(this.topRectHeight),
        onBack: () => {
          this.getUIContext().getRouter().back();
        }
      })

      PagePlaceholder({
        title: '元应用商店',
        description: '这里将展示可发现的元应用与推荐内容'
      })
    }
    .width('100%')
    .height('100%')
    .gesture(
      PanGesture({ direction: PanDirection.Right })
        .onActionEnd((event: GestureEvent) => {
          if (event.offsetX > 50) {
            this.getUIContext().getRouter().back();
          }
        })
    )
  }
}