import { SettingsScreen } from '../features/user-center/pages/SettingsScreen';

@Entry
@ComponentV2
struct SettingsPage {
  build() {
    Column() {
      SettingsScreen()
    }
    .width('100%')
    .height('100%')
  }
}
