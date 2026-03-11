import { ChatSession } from '../domain/Session';

export interface SessionGroups {
  todaySessions: ChatSession[];
  yesterdaySessions: ChatSession[];
  past30DaysSessions: ChatSession[];
  isEmpty: boolean;
}

export function buildSessionGroups(sessions: ChatSession[]): SessionGroups {
  if (!sessions || sessions.length === 0) {
    return {
      todaySessions: [],
      yesterdaySessions: [],
      past30DaysSessions: [],
      isEmpty: true,
    };
  }

  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const yesterdayStart = todayStart - 24 * 60 * 60 * 1000;
  const thirtyDaysAgoStart = todayStart - 30 * 24 * 60 * 60 * 1000;

  const todaySessions: ChatSession[] = [];
  const yesterdaySessions: ChatSession[] = [];
  const past30DaysSessions: ChatSession[] = [];

  sessions.forEach((session: ChatSession) => {
    const updateTime = new Date(session.updatedAt).getTime();
    if (updateTime >= todayStart) {
      todaySessions.push(session);
    } else if (updateTime >= yesterdayStart) {
      yesterdaySessions.push(session);
    } else if (updateTime >= thirtyDaysAgoStart) {
      past30DaysSessions.push(session);
    }
  });

  return {
    todaySessions,
    yesterdaySessions,
    past30DaysSessions,
    isEmpty: false,
  };
}