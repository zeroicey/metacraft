import { ExploreScreen } from '../features/app-center/pages/ExploreScreen';

@Entry
@Component
struct ExplorePage {
  build() {
    Column() {
      ExploreScreen()
    }
    .width('100%')
    .height('100%')
  }
}
