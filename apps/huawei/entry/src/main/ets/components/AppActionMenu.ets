/**
 * 应用操作菜单组件
 * 提供应用的更多操作菜单（编辑、切换版本、删除等）
 */
import { App, AppVersion } from '../model/appModel';
import { appApi } from '../api/appApi';

export interface AppActionMenuOptions {
  app: App;
  onRefresh: () => void;
}

@ComponentV2
export struct AppActionMenu {
  @Require @Param options: AppActionMenuOptions;
  @Local showSheet: boolean = false;
  @Local showEditDialog: boolean = false;
  @Local showVersionDialog: boolean = false;
  @Local showDeleteConfirm: boolean = false;
  @Local editName: string = '';
  @Local editDescription: string = '';
  @Local selectedVersionId: number | null = null;

  build() {
    Button() {
      Text('⋮')
        .fontWeight(FontWeight.Medium)
        .fontSize(22)
        .fontColor($r('sys.color.ohos_id_color_titlebar_icon'))
    }
    .backgroundColor(Color.Transparent)
    .height(36)
    .width(36)
    .onClick(() => {
      this.showSheet = true;
    })
    .bindSheet(this.showSheet, this.buildSheetContent(), {
      detents: [SheetSize.MEDIUM],
      preferType: SheetType.BOTTOM,
      showClose: true,
      title: { title: '应用操作' },
      dragBar: true,
      onDisappear: () => {
        this.showSheet = false;
      }
    })
    .bindContentCover(this.showEditDialog, this.buildEditDialog(), {
      modalTransition: ModalTransition.NONE
    })
    .bindContentCover(this.showVersionDialog, this.buildVersionDialog(), {
      modalTransition: ModalTransition.NONE
    })
    .bindContentCover(this.showDeleteConfirm, this.buildDeleteConfirm(), {
      modalTransition: ModalTransition.NONE
    })
  }

  @Builder
  buildSheetContent() {
    Column() {
      List({ space: 0 }) {
        // 编辑名称和描述
        ListItem() {
      Row() {
        Text('✏️')
          .fontSize(22)
          .margin({ right: 12 })
        Text('编辑应用')
          .fontSize(16)
          .fontColor($r('sys.color.ohos_id_color_text_primary'))
        Blank()
      }
      .width('100%')
      .height(52)
      .padding({ left: 16, right: 16 })
      .onClick(() => {
        this.showSheet = false;
        this.editName = this.options.app.name;
        this.editDescription = this.options.app.description;
        this.showEditDialog = true;
      })
    }

        // 切换版本
        ListItem() {
      Row() {
        Text('🔄')
          .fontSize(22)
          .margin({ right: 12 })
        Text('切换版本')
          .fontSize(16)
          .fontColor($r('sys.color.ohos_id_color_text_primary'))
        Blank()
        Text(`v${this.options.app.versions?.[0]?.versionNumber || 1}`)
          .fontSize(14)
          .fontColor('#999999')
      }
      .width('100%')
      .height(52)
      .padding({ left: 16, right: 16 })
      .onClick(() => {
        this.showSheet = false;
        if (this.options.app.versions && this.options.app.versions.length > 0) {
          this.selectedVersionId = this.options.app.currentVersionId;
          this.showVersionDialog = true;
        }
      })
    }

        // 删除应用
        ListItem() {
      Row() {
        Text('🗑️')
          .fontSize(22)
          .margin({ right: 12 })
        Text('删除应用')
          .fontSize(16)
          .fontColor($r('sys.color.ohos_id_color_text_primary'))
        Blank()
      }
      .width('100%')
      .height(52)
      .padding({ left: 16, right: 16 })
      .onClick(() => {
        this.showSheet = false;
        this.showDeleteConfirm = true;
      })
    }
      }
      .width('100%')
      .divider({ strokeWidth: 1, color: $r('sys.color.ohos_id_color_list_separator') })
      .borderRadius(12)
    }
    .width('100%')
    .padding(8)
  }

  @Builder
  buildEditDialog() {
    Column() {
      Column() {
        Text('编辑应用')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 20 })

        TextInput({ placeholder: '应用名称', text: this.editName })
          .fontSize(16)
          .height(48)
          .backgroundColor('#F5F7FA')
          .borderRadius(8)
          .onChange((value: string) => {
            this.editName = value;
          })

        TextArea({ placeholder: '应用描述', text: this.editDescription })
          .fontSize(14)
          .height(80)
          .backgroundColor('#F5F7FA')
          .borderRadius(8)
          .margin({ top: 12 })
          .onChange((value: string) => {
            this.editDescription = value;
          })

        Row() {
      Button('取消')
        .fontSize(16)
        .height(44)
        .backgroundColor('#F5F7FA')
        .layoutWeight(1)
        .onClick(() => {
          this.showEditDialog = false;
        })

      Button('保存')
        .fontSize(16)
        .height(44)
        .backgroundColor('#007AFF')
        .layoutWeight(1)
        .margin({ left: 12 })
        .onClick(() => {
          this.handleSaveEdit();
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
      this.showEditDialog = false;
    })
  }

  @Builder
  buildVersionDialog() {
    Column() {
      Column() {
        Text('选择版本')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 16 })

        List({ space: 8 }) {
      ForEach(this.options.app.versions || [], (version: AppVersion) => {
        ListItem() {
          Row() {
            Column() {
              Text(`版本 ${version.versionNumber}`)
                .fontSize(16)
                .fontWeight(FontWeight.Medium)
                .fontColor($r('sys.color.ohos_id_color_text_primary'))
              if (version.changeLog) {
                Text(version.changeLog)
                  .fontSize(12)
                  .fontColor('#999999')
                  .margin({ top: 4 })
                  .maxLines(1)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
              }
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)

            if (version.id === this.options.app.currentVersionId) {
              Text('当前')
                .fontSize(12)
                .fontColor('#007AFF')
                .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                .backgroundColor('#E5F1FF')
                .borderRadius(4)
            }
          }
          .width('100%')
          .height(60)
          .padding({ left: 12, right: 12 })
          .backgroundColor(this.selectedVersionId === version.id ? '#E5F1FF' : '#F5F7FA')
          .borderRadius(8)
          .onClick(() => {
            this.selectedVersionId = version.id;
            this.handleVersionChange(version.id);
          })
        }
      })
    }
    .width('100%')
    .height(300)
    .margin({ bottom: 16 })

    Button('关闭')
      .fontSize(16)
      .height(44)
      .width('100%')
      .backgroundColor('#F5F7FA')
      .onClick(() => {
        this.showVersionDialog = false;
      })
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
  }

  @Builder
  buildDeleteConfirm() {
    Column() {
      Column() {
        Text('删除应用')
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .margin({ bottom: 12 })

        Text(`确定要删除"${this.options.app.name}"吗？`)
          .fontSize(14)
          .fontColor('#666666')
          .margin({ bottom: 8 })

        Text('此操作将删除应用及其所有版本，无法恢复。')
          .fontSize(12)
          .fontColor('#FF3B30')
          .margin({ bottom: 20 })

        Row() {
      Button('取消')
        .fontSize(16)
        .height(44)
        .backgroundColor('#F5F7FA')
        .layoutWeight(1)
        .onClick(() => {
          this.showDeleteConfirm = false;
        })

      Button('删除')
        .fontSize(16)
        .height(44)
        .backgroundColor('#FF3B30')
        .layoutWeight(1)
        .margin({ left: 12 })
        .onClick(() => {
          this.handleDelete();
        })
    }
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
  }

  private async handleSaveEdit(): Promise<void> {
    if (!this.editName || this.editName.trim().length === 0) {
      this.showEditDialog = false;
      return;
    }

    const result = await appApi.updateApp(this.options.app.id, {
      name: this.editName,
      description: this.editDescription
    });

    this.showEditDialog = false;

    if (result.success) {
      this.options.onRefresh();
    } else {
      console.error('Failed to update app:', result.error);
    }
  }

  private async handleVersionChange(versionId: number): Promise<void> {
    // 这里需要调用后端API切换版本
    // 由于后端可能没有专门的切换版本API，我们可能需要添加
    // 暂时先关闭对话框
    this.showVersionDialog = false;

    // TODO: 调用切换版本API
    // const result = await appApi.switchVersion(this.options.app.id, versionId);
    // if (result.success) {
    //   this.options.onRefresh();
    // }
  }

  private async handleDelete(): Promise<void> {
    const result = await appApi.deleteApp(this.options.app.id);

    this.showDeleteConfirm = false;

    if (result.success) {
      this.options.onRefresh();
    } else {
      console.error('Failed to delete app:', result.error);
    }
  }
}
