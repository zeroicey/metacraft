export interface AppVersion {
  id: number;
  versionNumber: number;
  storagePath: string;
  changeLog: string;
  createdAt: string;
}

export interface App {
  id: number;
  uuid: string;
  name: string;
  description: string;
  logo?: string;
  isPublic: boolean;
  currentVersionId: number | null;
  versions: AppVersion[];
  createdAt: string;
  updatedAt: string;
}