import { request } from "./client";

export type AlertComparison = "GREATER_THAN" | "LESS_THAN";
export type AlertSeverity = "WARNING" | "CRITICAL";
export type AlertStatus = "OPEN" | "RESOLVED";

export type AlertRule = {
  id: string;
  serverId: string;
  name: string;
  metricName: string;
  comparison: AlertComparison;
  threshold: number;
  severity: AlertSeverity;
  enabled: boolean;
  createdAt: string;
};

export type AlertRecord = {
  id: string;
  serverId: string;
  ruleId: string;
  ruleName: string;
  metricName: string;
  threshold: number;
  observedValue: number;
  severity: AlertSeverity;
  status: AlertStatus;
  triggeredAt: string;
  resolvedAt: string | null;
};

export type AlertRuleInput = Pick<
  AlertRule,
  "name" | "metricName" | "comparison" | "threshold" | "severity"
>;

export async function fetchAlertRules(serverId: string, accessToken: string): Promise<AlertRule[]> {
  return request(`/api/v1/servers/${encodeURIComponent(serverId)}/alert-rules`, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
}

export async function createAlertRule(
  serverId: string,
  accessToken: string,
  rule: AlertRuleInput
): Promise<AlertRule> {
  return request(`/api/v1/servers/${encodeURIComponent(serverId)}/alert-rules`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify(rule)
  });
}

export async function updateAlertRule(
  serverId: string,
  accessToken: string,
  rule: AlertRule
): Promise<AlertRule> {
  const { name, metricName, comparison, threshold, severity, enabled } = rule;
  return request(
    `/api/v1/servers/${encodeURIComponent(serverId)}/alert-rules/${encodeURIComponent(rule.id)}`,
    {
      method: "PUT",
      headers: { Authorization: `Bearer ${accessToken}` },
      body: JSON.stringify({ name, metricName, comparison, threshold, severity, enabled })
    }
  );
}

export async function fetchServerAlerts(serverId: string, accessToken: string): Promise<AlertRecord[]> {
  return request(`/api/v1/servers/${encodeURIComponent(serverId)}/alerts`, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
}
