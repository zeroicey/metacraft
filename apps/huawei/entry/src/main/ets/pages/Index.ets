import { WorkspaceScreen } from '../features/workspace/pages/WorkspaceScreen';

@Entry
@ComponentV2
struct Index {
  build() {
    Column() {
      WorkspaceScreen()
    }
    .width('100%')
    .height('100%')
  }
}
