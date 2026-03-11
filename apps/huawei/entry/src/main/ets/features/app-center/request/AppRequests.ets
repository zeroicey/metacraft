export interface CreateAppRequest {
  name: string;
  description: string;
}

export interface UpdateAppRequest {
  name?: string;
  description?: string;
  isPublic?: boolean;
}