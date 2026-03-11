export function readSafeAreaHeights(): [number, number] {
  const topRectHeight = AppStorage.get<number>('topRectHeight') ?? 0;
  const bottomRectHeight = AppStorage.get<number>('bottomRectHeight') ?? 0;
  return [topRectHeight, bottomRectHeight];
}

export function resolveInitialSessionId(sessionIds: string[]): string {
  if (sessionIds.length === 0) {
    return '';
  }
  return sessionIds[0];
}