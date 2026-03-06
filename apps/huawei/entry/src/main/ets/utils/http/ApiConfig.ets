export const API_BASE_URL: string = 'http://10.225.11.177:8080';

export function toApiUrl(path: string): string {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }

  if (path.startsWith('/')) {
    return API_BASE_URL + path;
  }

  return API_BASE_URL + '/' + path;
}
