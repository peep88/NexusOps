import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchSystemStatus } from "./system";

describe("fetchSystemStatus", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("returns the data envelope from the backend contract", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      new Response(JSON.stringify({
        data: {
          service: "nexusops-backend",
          status: "UP",
          database: "UP",
          version: "0.1.0"
        },
        meta: { requestId: "request-1" },
        error: null
      }), { status: 200 })
    ));

    await expect(fetchSystemStatus()).resolves.toEqual({
      service: "nexusops-backend",
      status: "UP",
      database: "UP",
      version: "0.1.0"
    });
  });

  it("raises an error for an unsuccessful response", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      new Response(null, { status: 503 })
    ));

    await expect(fetchSystemStatus()).rejects.toThrow(
      "Backend status request failed (503)"
    );
  });
});
