import { afterEach, describe, expect, it, vi } from "vitest";
import { request } from "./client";

describe("API request errors", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("reports the HTTP status when an error response has no JSON body", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    await expect(request("/api/v1/servers")).rejects.toMatchObject({
      status: 401,
      message: "Request failed (401)"
    });
  });

  it("reports malformed successful responses without exposing a JSON parser error", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      new Response("not-json", { status: 200 })
    ));

    await expect(request("/api/v1/servers")).rejects.toThrow(
      "Invalid JSON response from server"
    );
  });
});
