import { request } from "./client";

export type MetricSample = {
  metricType: string;
  metricName: string;
  value: number;
  unit: string | null;
  labels: Record<string, unknown>;
  recordedAt: string;
};

export async function fetchServerMetrics(
  serverId: string,
  accessToken: string
): Promise<MetricSample[]> {
  return request(`/api/v1/servers/${encodeURIComponent(serverId)}/metrics`, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
}
