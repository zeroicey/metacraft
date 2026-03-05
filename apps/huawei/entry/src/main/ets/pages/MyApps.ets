import { router } from '@kit.ArkUI';
import { App } from '../model/appModel';
import { appApi } from '../api/appApi';
import { AppItem } from '../components/AppItem';

@Entry
@ComponentV2
struct MyApps {
  topRectHeight: number = 0;
  @Local apps: App[] = [];
  @Local isLoading: boolean = true;
  @Local showNewAppDialog: boolean = false;
  @Local newAppName: string = '';
  @Local newAppDescription: string = '';

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
    this.loadApps();
  }

  build() {
    Column() {
      // 顶部导航栏
      Row() {
        Button() {
          SymbolGlyph($r('sys.symbol.chevron_left'))
            .fontWeight(FontWeight.Normal)
            .fontSize(22)
            .fontColor([$r('sys.color.ohos_id_color_titlebar_icon')])
        }
        .backgroundColor(Color.Transparent)
        .height(40)
        .width(40)
        .onClick(() => {
          this.getUIContext().getRouter().back();
        })

        Text('我的元应用')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ left: 8 })

        Blank().layoutWeight(1)

        // 新建应用按钮
        Button() {
          SymbolGlyph($r('sys.symbol.plus'))
            .fontWeight(FontWeight.Medium)
            .fontSize(20)
            .fontColor([Color.White])
        }
        .width(36)
        .height(36)
        .backgroundColor('#007AFF')
        .borderRadius(18)
        .onClick(() => {
          this.showNewAppDialog = true;
          this.newAppName = '';
          this.newAppDescription = '';
        })
      }
      .width('100%')
      .padding({
        top: this.getUIContext().px2vp(this.topRectHeight),
        left: 16,
        right: 16,
        bottom: 8
      })

      // 应用列表内容
      if (this.isLoading) {
        // 加载状态
        Column() {
          LoadingProgress()
            .width(40)
            .height(40)
            .color('#007AFF')
          Text('加载中...')
            .fontSize(14)
            .fontColor('#999999')
            .margin({ top: 12 })
        }
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else if (this.apps.length === 0) {
        // 空状态
        Column({ space: 16 }) {
          Text('📱')
            .fontSize(48)

          Text('还没有应用')
            .fontSize(18)
            .fontWeight(FontWeight.Medium)
            .fontColor($r('sys.color.ohos_id_color_text_primary'))

          Text('通过对话创建你的第一个元应用吧')
            .fontSize(14)
            .fontColor('#999999')

          Button('去创建')
            .fontSize(16)
            .height(44)
            .backgroundColor('#007AFF')
            .borderRadius(22)
            .padding({ left: 24, right: 24 })
            .margin({ top: 8 })
            .onClick(() => {
              this.getUIContext().getRouter().back();
            })
        }
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
        .padding({ left: 32, right: 32 })
      } else {
        // 应用列表
        List({ space: 0 }) {
          ForEach(this.apps, (app: App, index: number) => {
            ListItem() {
              AppItem({
                app: app,
                onRefresh: () => {
                  this.loadApps();
                }
              })
            }
          })
        }
        .layoutWeight(1)
        .width('100%')
        .padding({ left: 16, right: 16 })
        .divider({ strokeWidth: 1, color: '#EEEEEE', startMargin: 76, endMargin: 16 })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F7FA')
    .gesture(
      PanGesture({ direction: PanDirection.Right })
        .onActionEnd((event: GestureEvent) => {
          if (event.offsetX > 50) {
            this.getUIContext().getRouter().back();
          }
        })
    )
    .bindContentCover(this.showNewAppDialog, this.buildNewAppDialog(), {
      modalTransition: ModalTransition.NONE
    })
  }

  @Builder
  buildNewAppDialog() {
    Column() {
      Column() {
        Text('新建应用')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 20 })

        TextInput({ placeholder: '应用名称', text: this.newAppName })
          .fontSize(16)
          .height(48)
          .backgroundColor('#F5F7FA')
          .borderRadius(8)
          .onChange((value: string) => {
            this.newAppName = value;
          })

        TextArea({ placeholder: '应用描述（可选）', text: this.newAppDescription })
          .fontSize(14)
          .height(80)
          .backgroundColor('#F5F7FA')
          .borderRadius(8)
          .margin({ top: 12 })
          .onChange((value: string) => {
            this.newAppDescription = value;
          })

        Row() {
          Button('取消')
            .fontSize(16)
            .height(44)
            .backgroundColor('#F5F7FA')
            .layoutWeight(1)
            .onClick(() => {
              this.showNewAppDialog = false;
            })

          Button('创建')
            .fontSize(16)
            .height(44)
            .backgroundColor('#007AFF')
            .layoutWeight(1)
            .margin({ left: 12 })
            .onClick(() => {
              this.handleCreateApp();
            })
        }
        .margin({ top: 20 })
      }
      .width('100%')
      .padding(20)
      .backgroundColor(Color.White)
      .borderRadius(16)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('rgba(0, 0, 0, 0.4)')
    .justifyContent(FlexAlign.Center)
    .padding({ left: 24, right: 24 })
    .onClick(() => {
      this.showNewAppDialog = false;
    })
  }

  private async loadApps(): Promise<void> {
    this.isLoading = true;
    const result = await appApi.getUserApps();

    if (result.success && result.data) {
      this.apps = result.data;
    } else {
      console.error('Failed to load apps:', result.error);
      // 可以在这里添加错误提示
    }

    this.isLoading = false;
  }

  private async handleCreateApp(): Promise<void> {
    if (!this.newAppName || this.newAppName.trim().length === 0) {
      return;
    }

    const result = await appApi.createApp({
      name: this.newAppName.trim(),
      description: this.newAppDescription.trim()
    });

    this.showNewAppDialog = false;

    if (result.success) {
      // 刷新列表
      this.loadApps();
    } else {
      console.error('Failed to create app:', result.error);
      // 可以在这里添加错误提示
    }
  }
}
