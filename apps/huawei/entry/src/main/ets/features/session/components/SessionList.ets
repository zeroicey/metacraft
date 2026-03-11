import { ChatSession } from '../domain/Session';
import { sessionService } from '../service/SessionService';
import { toApiUrl } from '../../../core/http/ApiConfig';
import { buildSessionGroups } from '../state/SessionGroups';

@ComponentV2
export struct SessionList {
  @Param @Require selectedSessionId: string;
  @Param onSessionClick: (sessionId: string) => void = () => {};
  @Param refreshSignal: number = 0;

  @Local todaySessions: ChatSession[] = [];
  @Local yesterdaySessions: ChatSession[] = [];
  @Local past30DaysSessions: ChatSession[] = [];
  @Local isLoading: boolean = false;
  @Local isEmpty: boolean = false;
  @Local logoLoadFailedSessionIds: string[] = [];

  @Monitor('refreshSignal')
  onRefreshSignalChange() {
    this.fetchSessions();
  }

  aboutToAppear() {
    this.fetchSessions();
  }

  async fetchSessions() {
    this.isLoading = true;
    const result = await sessionService.getUserSessions();
    this.isLoading = false;

    if (result.success && result.data) {
      this.groupSessions(result.data);
    } else {
      console.error('[SessionList] Failed to load sessions:', result.error);
    }
  }

  private groupSessions(sessions: ChatSession[]) {
    const groups = buildSessionGroups(sessions);
    this.isEmpty = groups.isEmpty;
    this.logoLoadFailedSessionIds = [];
    this.todaySessions = groups.todaySessions;
    this.yesterdaySessions = groups.yesterdaySessions;
    this.past30DaysSessions = groups.past30DaysSessions;
  }

  private hasLogo(session: ChatSession): boolean {
    return !!session.relatedAppId && !this.hasLogoLoadFailed(session.sessionId);
  }

  private hasLogoLoadFailed(sessionId: string): boolean {
    return this.logoLoadFailedSessionIds.includes(sessionId);
  }

  private markLogoLoadFailed(sessionId: string) {
    if (this.hasLogoLoadFailed(sessionId)) {
      return;
    }
    this.logoLoadFailedSessionIds = [...this.logoLoadFailedSessionIds, sessionId];
  }

  private getLogoUrl(session: ChatSession): string {
    if (!session.relatedAppId) {
      return '';
    }
    return toApiUrl(`/api/logo/app/${session.relatedAppId}?t=${session.updatedAt}`);
  }

  @Builder
  SessionGroup(title: string, sessions: ChatSession[]) {
    if (sessions.length > 0) {
      Column({ space: 2 }) {
        Text(title)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ bottom: 4 })
          .width('100%')
          .textAlign(TextAlign.Start)

        Column({ space: 2 }) {
          ForEach(sessions, (item: ChatSession) => {
            Row({ space: 8 }) {
              Text(item.title)
                .fontSize(14)
                .fontWeight(this.selectedSessionId === item.sessionId ? FontWeight.Bold : FontWeight.Medium)
                .fontColor(this.selectedSessionId === item.sessionId ? '#000000' : '#333333')
                .layoutWeight(1)
                .maxLines(1)
                .textOverflow({ overflow: TextOverflow.Ellipsis })

              if (this.hasLogo(item)) {
                Image(this.getLogoUrl(item))
                  .width(28)
                  .height(28)
                  .borderRadius(6)
                  .objectFit(ImageFit.Cover)
                  .onError(() => {
                    this.markLogoLoadFailed(item.sessionId);
                  });
              }
            }
            .width('100%')
            .padding({ top: 6, bottom: 6, left: 12, right: 12 })
            .backgroundColor(this.selectedSessionId === item.sessionId ? '#F2F2F2' : Color.Transparent)
            .borderRadius(10)
            .onClick(() => {
              this.onSessionClick(item.sessionId);
            })
          }, (item: ChatSession) => item.sessionId)
        }
        .width('100%')
      }
    }
  }

  build() {
    Column({ space: 16 }) {
      if (this.isLoading) {
        Row() {
          LoadingProgress().width(24).height(24)
        }.width('100%').justifyContent(FlexAlign.Center).padding(20)
      } else if (this.isEmpty) {
        Column({ space: 8 }) {
          Text('还没有会话记录')
            .fontSize(14)
            .fontColor('#999999')
          Text('快去创建一个新应用吧')
            .fontSize(12)
            .fontColor('#CCCCCC')
        }
        .width('100%')
        .padding({ top: 32, bottom: 32 })
        .alignItems(HorizontalAlign.Center)
      } else {
        this.SessionGroup('今天', this.todaySessions)
        this.SessionGroup('昨天', this.yesterdaySessions)
        this.SessionGroup('30天内', this.past30DaysSessions)
      }
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }
}