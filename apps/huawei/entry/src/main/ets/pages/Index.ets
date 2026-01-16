import { Sidebar } from '../components/Sidebar';
import { LandingStage } from '../components/LandingStage';
import { InceptionStage } from '../components/InceptionStage';
import { BlueprintStage } from '../components/BlueprintStage';
import { WorkspaceStage } from '../components/WorkspaceStage';
import { BottomSheetMenu } from '../components/BottomSheetMenu';

enum CreationStage {
  Landing,
  Inception,
  Blueprint,
  Workspace
}

@Entry
@ComponentV2
struct Index {
  topRectHeight: number = 0;
  bottomRectHeight: number = 0;
  @Local creationStage: CreationStage = CreationStage.Landing;
  @Local appName: string = '';
  @Local appDesc: string = '';
  @Local planText: string = '';
  @Local showSideBar: boolean = false;

  aboutToAppear(): void {
    this.topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
    this.bottomRectHeight = AppStorage.get<number>('bottomRectHeight') ?? 0;
  }

  @Builder
  buildTopBar() {
    Row() {
      Button() {
        SymbolGlyph($r('sys.symbol.open_sidebar'))
          .fontWeight(FontWeight.Normal)
          .fontSize(22)
          .fontColor([$r('sys.color.ohos_id_color_titlebar_icon')])
          .hitTestBehavior(HitTestMode.None)
      }
      .backgroundColor(Color.Transparent)
      .height(40)
      .width(40)
      .onClick(() => {
        this.showSideBar = true;
      })

      Blank().layoutWeight(1)

      Text('元创')
        .fontSize(18)
        .fontWeight(FontWeight.Medium)
        .fontStyle(FontStyle.Italic)

      Blank().layoutWeight(1)

      BottomSheetMenu({
        onMenuItemClick: (menuId: string) => {
          this.handleMenuClick(menuId);
        }
      })
    }
    .width('100%')
    .padding({
      top: this.getUIContext().px2vp(this.topRectHeight),
      left: 16,
      right: 16,
      bottom: 8
    })}

  @Builder
  buildContent() {
    Column() {
      this.buildTopBar();

      // 可滚动的内容区域
      Scroll() {
        Column() {
          if (this.creationStage === CreationStage.Landing) {
            LandingStage({
              onStart: () => {
                this.creationStage = CreationStage.Inception;
              },
              onSelectInspiration: (name: string, desc: string) => {
                this.appName = name;
                this.appDesc = desc;
                this.creationStage = CreationStage.Inception;
              }
            })
          } else if (this.creationStage === CreationStage.Inception) {
            InceptionStage({
              appName: this.appName,
              appDesc: this.appDesc,
              onAppNameChange: (value: string) => {
                this.appName = value;
              },
              onAppDescChange: (value: string) => {
                this.appDesc = value;
              },
              onNext: () => {
                this.planText = this.buildMockPlan();
                this.creationStage = CreationStage.Blueprint;
              }
            })
          } else if (this.creationStage === CreationStage.Blueprint) {
            BlueprintStage({
              appName: this.appName,
              planText: this.planText,
              onPlanTextChange: (value: string) => {
                this.planText = value;
              },
              onBack: () => {
                this.creationStage = CreationStage.Inception;
              },
              onBuild: () => {
                this.creationStage = CreationStage.Workspace;
              }
            })
          } else if (this.creationStage === CreationStage.Workspace) {
            WorkspaceStage({
              appName: this.appName,
              onRunPreview: () => {
                this.getUIContext().getRouter().pushUrl({ url: 'pages/Preview' });
              }
            })
          }
        }
        .width('100%')
        .padding({ left: 16, right: 16, bottom: 16 })
      }
      .layoutWeight(1)
      .edgeEffect(EdgeEffect.Spring)
    }
    .width('100%')
    .height('100%')
  }

  build() {
    Stack() {
      Sidebar({
        isShowSidebar: this.showSideBar,
        topRectHeight: this.getUIContext().px2vp(this.topRectHeight),
        onSideBarChange: (isShow: boolean) => {
          this.showSideBar = isShow;
        },
        contentBuilder: () => {
          this.buildContent();
        }
      })
    }
    .width('100%')
    .height('100%')
  }

  buildMockPlan(): string {
    const name = this.appName || '我的应用';
    const desc = this.appDesc || '这是一个由 MetaCraft 生成的应用。';
    return `1. 首页:展示 ${name} 的核心功能和快速入口。\n2. 功能模块:根据描述「${desc}」拆解为若干子功能。\n3. UI 风格:扁平化设计,主色调为蓝色,可在生成后继续微调。`;
  }

  // 菜单项点击处理
  handleMenuClick(menuId: string) {
    console.info('点击了菜单项:', menuId);
    switch (menuId) {
      case 'profile':
        // 跳转到个人中心
        break;
      case 'settings':
        // 跳转到设置页面
        break;
      case 'about':
        // 显示关于信息
        break;
    }
  }
}