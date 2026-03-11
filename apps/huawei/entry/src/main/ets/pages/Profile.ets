import { ProfileScreen } from '../features/user-center/pages/ProfileScreen';

@Entry
@ComponentV2
struct ProfilePage {
  build() {
    Column() {
      ProfileScreen()
    }
    .width('100%')
    .height('100%')
  }
}
