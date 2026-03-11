@Component
export struct AppInfoCard {
  @Prop appName: string = 'Application Generated';
  @Prop appDescription: string = 'Click to preview the app';
  @Prop @Watch('onLogoUrlChange') logoUrl: string = '';
  @Prop previewReady: boolean = false;
  @State private logoLoadFailed: boolean = false;
  @State private resolvedLogoUrl: string = '';

  aboutToAppear() {
    this.syncResolvedLogoUrl();
  }

  onLogoUrlChange() {
    this.syncResolvedLogoUrl();
  }

  private syncResolvedLogoUrl() {
    this.logoLoadFailed = false;

    if (!this.logoUrl || this.logoUrl.length === 0) {
      this.resolvedLogoUrl = '';
      return;
    }

    this.resolvedLogoUrl = this.withCacheBust(this.logoUrl);
  }

  private withCacheBust(url: string): string {
    const separator = url.includes('?') ? '&' : '?';
    return `${url}${separator}t=${Date.now()}`;
  }

  build() {
    Column() {
      Row() {
        Stack() {
          if (this.logoUrl.length > 0 && this.resolvedLogoUrl.length > 0 && !this.logoLoadFailed) {
            Image(this.resolvedLogoUrl)
              .width(40)
              .height(40)
              .borderRadius(8)
              .objectFit(ImageFit.Cover)
              .onError(() => {
                this.logoLoadFailed = true;
              });
          } else {
            Rect().width(40).height(40).fill('#007AFF').radius(8);
            Text('App').fontSize(12).fontColor(Color.White);
          }
        }
        .margin({ right: 12 })

        Column() {
          Text(this.appName)
            .fontSize(14)
            .fontWeight(FontWeight.Bold)
            .fontColor('#333333')
            .margin({ bottom: 2 })
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })

          Text(this.appDescription)
            .fontSize(11)
            .fontColor('#999999')
            .maxLines(2)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)

        Column() {
          if (this.previewReady) {
            Text('已完成')
              .fontSize(11)
              .fontColor('#12A150')
              .fontWeight(FontWeight.Medium)
          } else {
            LoadingProgress()
              .width(20)
              .height(20)
              .color('#007AFF')

            Text('生成中')
              .fontSize(10)
              .fontColor('#7C8AA5')
              .margin({ top: 4 })
          }
        }
        .width(48)
      }
      .width('100%')
    }
    .width('100%')
    .padding(12)
    .backgroundColor('#F5F7FA')
    .borderRadius(12)
    .border({ width: 1, color: '#EEEEEE' })
    .margin({ top: 8 })
  }
}