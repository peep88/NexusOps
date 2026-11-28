import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchServerMetrics } from "./metrics";

describe("fetchServerMetrics", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("requests a server's recent samples with the access token", async () => {
    const samples = [{
      metricType: "cpu",
      metricName: "cpu.usage_percent",
      value: 42.5,
      unit: "percent",
      labels: {},
      recordedAt: "2026-11-28T11:00:00Z"
    }];
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({
        data: samples,
        meta: { requestId: "request-1" },
        error: null
      }), { status: 200 })
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(fetchServerMetrics("server-1", "access-token")).resolves.toEqual(samples);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/servers/server-1/metrics",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
  });
});
