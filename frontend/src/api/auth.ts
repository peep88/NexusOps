export type UserRole = "ADMIN" | "OPERATOR" | "VIEWER" | "AGENT";

export type AuthTokens = {
  accessToken: string;
  refreshToken: string;
  userId: string;
  email: string;
  displayName: string;
  role: UserRole;
};

export function login(email: string, password: string): Promise<AuthTokens> {
  return request("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password })
  });
}

export function refresh(refreshToken: string): Promise<AuthTokens> {
  return request("/api/v1/auth/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken })
  });
}

const storageKey = "nexusops.auth";

export function saveTokens(tokens: AuthTokens): void {
  localStorage.setItem(storageKey, JSON.stringify(tokens));
}

export function loadTokens(): AuthTokens | null {
  const stored = localStorage.getItem(storageKey);
  if (!stored) {
    return null;
  }
  try {
    return JSON.parse(stored) as AuthTokens;
  } catch {
    localStorage.removeItem(storageKey);
    return null;
  }
}

export function clearTokens(): void {
  localStorage.removeItem(storageKey);
}

export function register(
  email: string,
  password: string,
  displayName: string
): Promise<AuthTokens> {
  return request("/api/v1/auth/register", {
    method: "POST",
    body: JSON.stringify({ email, password, displayName })
  });
}
import { request } from "./client";

