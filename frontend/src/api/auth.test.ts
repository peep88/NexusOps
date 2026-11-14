import { afterEach, describe, expect, it } from "vitest";
import {
  clearTokens,
  loadTokens,
  saveTokens,
  AuthTokens
} from "./auth";

const tokens: AuthTokens = {
  accessToken: "access",
  refreshToken: "refresh",
  userId: "user-1",
  email: "user@example.com",
  displayName: "User",
  role: "VIEWER"
};

describe("auth token persistence", () => {
  afterEach(() => {
    clearTokens();
  });

  it("persists and restores tokens", () => {
    saveTokens(tokens);
    expect(loadTokens()).toEqual(tokens);
  });

  it("clears tokens", () => {
    saveTokens(tokens);
    clearTokens();
    expect(loadTokens()).toBeNull();
  });
});
