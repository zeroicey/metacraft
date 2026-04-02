/**
 * App version type (aligned with backend AppVersionVO)
 */
export interface AppVersion {
    id: number;
    versionNumber: number;
    storagePath: string;
    changeLog: string;
    createdAt: string;
}

/**
 * App type (aligned with backend AppVO)
 */
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

/**
 * App create request
 */
export interface CreateAppRequest {
    name: string;
    description?: string;
}