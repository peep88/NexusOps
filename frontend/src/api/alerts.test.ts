import { afterEach, describe, expect, it, vi } from "vitest";
import { createAlertRule, updateAlertRule } from "./alerts";

describe("createAlertRule", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("sends a validated rule payload with the user's access token", async () => {
    const rule = {
      id: "rule-1",
      serverId: "server-1",
      name: "High CPU",
      metricName: "cpu.usage_percent",
      comparison: "GREATER_THAN",
      threshold: 80,
      severity: "WARNING",
      enabled: true,
      createdAt: "2026-11-28T11:00:00Z"
    };
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({
        data: rule,
        meta: { requestId: "request-1" },
        error: null
      }), { status: 200 })
    );
    vi.stubGlobal("fetch", fetchMock);

    await expect(createAlertRule("server-1", "access-token", {
      name: "High CPU",
      metricName: "cpu.usage_percent",
      comparison: "GREATER_THAN",
      threshold: 80,
      severity: "WARNING"
    })).resolves.toEqual(rule);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/servers/server-1/alert-rules",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ Authorization: "Bearer access-token" })
      })
    );
  });

  it("sends only mutable fields when updating a rule", async () => {
    const rule = {
      id: "rule-1",
      serverId: "server-1",
      name: "High CPU",
      metricName: "cpu.usage_percent",
      comparison: "GREATER_THAN" as const,
      threshold: 80,
      severity: "WARNING" as const,
      enabled: false,
      createdAt: "2026-11-28T11:00:00Z"
    };
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({
        data: rule,
        meta: { requestId: "request-1" },
        error: null
      }), { status: 200 })
    );
    vi.stubGlobal("fetch", fetchMock);

    await updateAlertRule("server-1", "access-token", rule);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/servers/server-1/alert-rules/rule-1",
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({
          name: rule.name,
          metricName: rule.metricName,
          comparison: rule.comparison,
          threshold: rule.threshold,
          severity: rule.severity,
          enabled: rule.enabled
        })
      })
    );
  });
});
