import { MetricSample } from "./api/metrics";

const TREND_METRICS = [
  { name: "cpu.usage_percent", label: "CPU usage" },
  { name: "memory.usage_percent", label: "Memory usage" },
  { name: "disk.usage_percent", label: "Disk usage" }
];

type Props = {
  samples: MetricSample[];
};

function MetricTrend({ samples, metricName, label }: {
  samples: MetricSample[];
  metricName: string;
  label: string;
}) {
  const points = samples
    .filter((sample) => sample.metricName === metricName)
    .slice(-60);
  const coordinates = points.map((sample, index) => {
    const x = points.length === 1 ? 50 : index * 100 / (points.length - 1);
    const value = Math.max(0, Math.min(100, sample.value));
    return `${x},${100 - value}`;
  }).join(" ");
  const latest = points[points.length - 1];

  return (
    <article className="metric-card">
      <div className="metric-card-heading">
        <h3>{label}</h3>
        <strong>{latest ? `${latest.value.toFixed(1)}%` : "—"}</strong>
      </div>
      {points.length > 0 ? (
        <>
          <svg
            className="metric-chart"
            viewBox="0 0 100 100"
            role="img"
            aria-label={`${label} trend`}
            preserveAspectRatio="none"
          >
            <polyline points={coordinates} />
          </svg>
          <p>{points.length} samples · latest {new Date(latest.recordedAt).toLocaleTimeString()}</p>
        </>
      ) : (
        <p>No {label.toLowerCase()} samples yet.</p>
      )}
    </article>
  );
}

export function MetricDashboard({ samples }: Props) {
  const recentSamples = samples.slice(-16).reverse();

  return (
    <section className="metric-dashboard" aria-label="Recent metrics">
      <h3>Recent metric trends</h3>
      <div className="metric-charts">
        {TREND_METRICS.map((metric) => (
          <MetricTrend
            key={metric.name}
            samples={samples}
            metricName={metric.name}
            label={metric.label}
          />
        ))}
      </div>
      <h3>Latest samples</h3>
      {recentSamples.length > 0 ? (
        <div className="metric-table-wrap">
          <table className="metric-table">
            <thead>
              <tr><th>Metric</th><th>Value</th><th>Recorded</th></tr>
            </thead>
            <tbody>
              {recentSamples.map((sample, index) => (
                <tr key={`${sample.metricName}-${sample.recordedAt}-${index}`}>
                  <td>{sample.metricName}</td>
                  <td>{sample.value.toLocaleString()} {sample.unit ?? ""}</td>
                  <td>{new Date(sample.recordedAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p>No metric samples have been received for this server.</p>
      )}
    </section>
  );
}
