/**
 * User login request data
 */
export interface LoginRequest {
    email: string;
    password: string;
}

/**
 * User registration request data
 */
export interface RegisterRequest {
    email: string;
    password: string;
    name: string;
    bio: string; // Required field, but can be empty string
    avatarBase64?: string;
}

/**
 * Authentication token response
 */
export interface AuthTokenResponse {

    token: string;
    type: string;
    expiresIn: number;
}

/**
 * Current user info type (aligned with backend UserVO)
 */
export interface User {
    id: number;
    email: string;
    name: string;
    avatarBase64?: string;
    bio: string;
    createdAt: string;
    updatedAt: string;
}
