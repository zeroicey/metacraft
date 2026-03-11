import { AppStoreScreen } from '../features/app-center/pages/AppStoreScreen';

@Entry
@ComponentV2
struct AppStore {
  build() {
    Column() {
      AppStoreScreen()
    }
    .width('100%')
    .height('100%')
  }
}
