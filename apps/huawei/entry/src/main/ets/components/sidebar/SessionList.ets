import { ChatSession } from '../../model/chatSessionModel';
import { chatSessionApi } from '../../api/chatSession';

@ComponentV2
export struct SessionList {
  @Param @Require selectedSessionId: string;
  @Param onSessionClick: (sessionId: string) => void = () => {};

  @Local todaySessions: ChatSession[] = [];
  @Local yesterdaySessions: ChatSession[] = [];
  @Local past30DaysSessions: ChatSession[] = [];
  @Local isLoading: boolean = false;
  @Local isEmpty: boolean = false;

  aboutToAppear() {
    this.fetchSessions();
  }

  async fetchSessions() {
    this.isLoading = true;
    const result = await chatSessionApi.getUserSessions();
    this.isLoading = false;

    if (result.success && result.data) {
      this.groupSessions(result.data);
    } else {
      console.error('[SessionList] Failed to load sessions:', result.error);
    }
  }

  private groupSessions(sessions: ChatSession[]) {
    if (!sessions || sessions.length === 0) {
      this.isEmpty = true;
      return;
    }
    this.isEmpty = false;

    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const yesterdayStart = todayStart - 24 * 60 * 60 * 1000;
    const thirtyDaysAgoStart = todayStart - 30 * 24 * 60 * 60 * 1000;

    const today: ChatSession[] = [];
    const yesterday: ChatSession[] = [];
    const past30Days: ChatSession[] = [];

    sessions.forEach(session => {
      // 假设 updatedAt 是 ISO 8601 字符串
      const updateTime = new Date(session.updatedAt).getTime();
      if (updateTime >= todayStart) {
        today.push(session);
      } else if (updateTime >= yesterdayStart) {
        yesterday.push(session);
      } else if (updateTime >= thirtyDaysAgoStart) {
        past30Days.push(session);
      }
    });

    this.todaySessions = today;
    this.yesterdaySessions = yesterday;
    this.past30DaysSessions = past30Days;
  }

  @Builder
  SessionGroup(title: string, sessions: ChatSession[]) {
    if (sessions.length > 0) {
      Column({ space: 4 }) {
        Text(title)
          .fontSize(12)
          .fontColor('#999999')
          .margin({ bottom: 4 })
          .width('100%')
          .textAlign(TextAlign.Start)

        Column({ space: 4 }) {
          ForEach(sessions, (item: ChatSession) => {
            Row({ space: 12 }) {
              Text(item.title)
                .fontSize(14)
                .fontWeight(this.selectedSessionId === item.sessionId ? FontWeight.Bold : FontWeight.Medium)
                .fontColor(this.selectedSessionId === item.sessionId ? '#000000' : '#333333')
                .layoutWeight(1)
                .maxLines(1)
                .textOverflow({ overflow: TextOverflow.Ellipsis })
            }
            .width('100%')
            .padding({ top: 8, bottom: 8, left: 12, right: 12 })
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
        // Loading State
        Row() {
          LoadingProgress().width(24).height(24)
        }.width('100%').justifyContent(FlexAlign.Center).padding(20)
      } else if (this.isEmpty) {
        // Empty State
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
        // Session Lists
        this.SessionGroup('今天', this.todaySessions)
        this.SessionGroup('昨天', this.yesterdaySessions)
        this.SessionGroup('30天内', this.past30DaysSessions)
      }
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
  }
}
