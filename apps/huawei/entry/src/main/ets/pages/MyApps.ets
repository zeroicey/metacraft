import { MyAppsScreen } from '../features/app-center/pages/MyAppsScreen';

@Entry
@ComponentV2
struct MyApps {
  build() {
    Column() {
      MyAppsScreen()
    }
    .width('100%')
    .height('100%')
  }
}
