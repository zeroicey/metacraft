import { DataCenterScreen } from '../features/data-center/pages/DataCenterScreen';

@Entry
@ComponentV2
struct DataCenter {
  build() {
    Column() {
      DataCenterScreen()
    }
    .width('100%')
    .height('100%')
  }
}
