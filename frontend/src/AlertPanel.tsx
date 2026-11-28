import { FormEvent, useEffect, useState } from "react";
import {
  AlertRecord,
  AlertRule,
  AlertRuleInput,
  createAlertRule,
  fetchAlertRules,
  fetchServerAlerts,
  updateAlertRule
} from "./api/alerts";

const METRIC_OPTIONS = [
  ["cpu.usage_percent", "CPU usage (%)"],
  ["memory.usage_percent", "Memory usage (%)"],
  ["disk.usage_percent", "Disk usage (%)"],
  ["memory.used_bytes", "Memory used (bytes)"],
  ["disk.used_bytes", "Disk used (bytes)"],
  ["network.received_bytes", "Network received (bytes)"],
  ["network.sent_bytes", "Network sent (bytes)"],
  ["process.count", "Process count"]
];

type Props = {
  serverId: string;
  accessToken: string;
  canManage: boolean;
};

export function AlertPanel({ serverId, accessToken, canManage }: Props) {
  const [rules, setRules] = useState<AlertRule[]>([]);
  const [alerts, setAlerts] = useState<AlertRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState("High CPU utilization");
  const [metricName, setMetricName] = useState("cpu.usage_percent");
  const [comparison, setComparison] = useState<AlertRuleInput["comparison"]>("GREATER_THAN");
  const [threshold, setThreshold] = useState("80");
  const [severity, setSeverity] = useState<AlertRuleInput["severity"]>("WARNING");

  async function reload() {
    const [nextRules, nextAlerts] = await Promise.all([
      fetchAlertRules(serverId, accessToken),
      fetchServerAlerts(serverId, accessToken)
    ]);
    setRules(nextRules);
    setAlerts(nextAlerts);
  }

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    Promise.all([
      fetchAlertRules(serverId, accessToken),
      fetchServerAlerts(serverId, accessToken)
    ]).then(([nextRules, nextAlerts]) => {
      if (active) {
        setRules(nextRules);
        setAlerts(nextAlerts);
      }
    }).catch((reason: unknown) => {
      if (active) {
        setError(reason instanceof Error ? reason.message : "Unable to load alerts");
      }
    }).finally(() => {
      if (active) {
        setLoading(false);
      }
    });
    return () => { active = false; };
  }, [serverId, accessToken]);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      await createAlertRule(serverId, accessToken, {
        name: name.trim(),
        metricName,
        comparison,
        threshold: Number(threshold),
        severity
      });
      await reload();
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : "Unable to create alert rule");
    } finally {
      setSaving(false);
    }
  }

  async function toggleRule(rule: AlertRule) {
    setError(null);
    setSaving(true);
    try {
      await updateAlertRule(serverId, accessToken, { ...rule, enabled: !rule.enabled });
      await reload();
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : "Unable to update alert rule");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="alert-panel" aria-label="Alert rules and alerts">
      <h3>Alerting</h3>
      {error && <p className="error" role="alert">{error}</p>}
      {loading ? <p>Loading alert rules…</p> : (
        <>
          <div className="alert-section">
            <h4>Rules</h4>
            {rules.length === 0 && <p>No alert rules configured.</p>}
            <ul className="alert-rule-list">
              {rules.map((rule) => (
                <li key={rule.id}>
                  <div>
                    <strong>{rule.name}</strong>
                    <span>{rule.metricName} {rule.comparison === "GREATER_THAN" ? ">" : "<"} {rule.threshold} · {rule.severity}</span>
                  </div>
                  {canManage && (
                    <button
                      type="button"
                      className="secondary-button"
                      disabled={saving}
                      onClick={() => toggleRule(rule)}
                    >
                      {rule.enabled ? "Disable" : "Enable"}
                    </button>
                  )}
                  {!rule.enabled && <span className="muted-status">Disabled</span>}
                </li>
              ))}
            </ul>
            {canManage && (
              <form className="alert-rule-form" onSubmit={submit}>
                <label>
                  Rule name
                  <input value={name} onChange={(event) => setName(event.target.value)} required maxLength={120} />
                </label>
                <label>
                  Metric
                  <select value={metricName} onChange={(event) => setMetricName(event.target.value)}>
                    {METRIC_OPTIONS.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select>
                </label>
                <label>
                  Condition
                  <select value={comparison} onChange={(event) => setComparison(event.target.value as AlertRuleInput["comparison"])}>
                    <option value="GREATER_THAN">Greater than</option>
                    <option value="LESS_THAN">Less than</option>
                  </select>
                </label>
                <label>
                  Threshold
                  <input
                    type="number"
                    step="any"
                    value={threshold}
                    onChange={(event) => setThreshold(event.target.value)}
                    required
                  />
                </label>
                <label>
                  Severity
                  <select value={severity} onChange={(event) => setSeverity(event.target.value as AlertRuleInput["severity"])}>
                    <option value="WARNING">Warning</option>
                    <option value="CRITICAL">Critical</option>
                  </select>
                </label>
                <button type="submit" disabled={saving}>{saving ? "Saving…" : "Add rule"}</button>
              </form>
            )}
          </div>
          <div className="alert-section">
            <h4>Recent alerts</h4>
            {alerts.length === 0 ? <p>No alerts triggered.</p> : (
              <ul className="alert-history">
                {alerts.map((alert) => (
                  <li key={alert.id} className={alert.status === "OPEN" ? "alert-open" : ""}>
                    <div>
                      <strong>{alert.ruleName}</strong>
                      <span>{alert.metricName}: {alert.observedValue} (threshold {alert.threshold}) · {alert.severity}</span>
                    </div>
                    <div className="alert-state">
                      <span>{alert.status}</span>
                      <time dateTime={alert.triggeredAt}>{new Date(alert.triggeredAt).toLocaleString()}</time>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </section>
  );
}
