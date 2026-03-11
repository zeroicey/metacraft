import { PreviewScreen } from '../features/app-center/pages/PreviewScreen';

@Entry
@Component
struct PreviewPage {
  build() {
    Column() {
      PreviewScreen()
    }
    .width('100%')
    .height('100%')
  }
}
