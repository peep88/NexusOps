import { FormEvent, useEffect, useState } from "react";
import { AuthTokens, clearTokens, loadTokens, login, refresh, register, saveTokens } from "./api/auth";
import { ApiRequestError } from "./api/client";
import { fetchServerMetrics, MetricSample } from "./api/metrics";
import { fetchServers, Server } from "./api/servers";
import { fetchSystemStatus, SystemStatus } from "./api/system";
import { AlertPanel } from "./AlertPanel";
import { MetricDashboard } from "./MetricDashboard";

export function App() {
  const [status, setStatus] = useState<SystemStatus | null>(null);
  const [servers, setServers] = useState<Server[]>([]);
  const [selectedServerId, setSelectedServerId] = useState<string | null>(null);
  const [metrics, setMetrics] = useState<MetricSample[]>([]);
  const [metricsLoading, setMetricsLoading] = useState(false);
  const [metricsError, setMetricsError] = useState<string | null>(null);
  const [auth, setAuth] = useState<AuthTokens | null>(() => loadTokens());
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchSystemStatus().then(setStatus).catch(() => setStatus(null));
  }, []);

  useEffect(() => {
    if (auth) {
      loadServers(auth);
    }
  }, [auth]);

  async function loadServers(tokens: AuthTokens) {
    try {
      setServers(await fetchServers(tokens.accessToken));
    } catch (reason: unknown) {
      if (reason instanceof ApiRequestError && reason.status === 401) {
        try {
          const renewed = await refresh(tokens.refreshToken);
          saveTokens(renewed);
          setAuth(renewed);
          setServers(await fetchServers(renewed.accessToken));
          return;
        } catch {
          clearTokens();
          setAuth(null);
        }
      }
      setError(reason instanceof Error ? reason.message : "Unable to load servers");
    }
  }

  async function toggleMetrics(serverId: string) {
    if (!auth || metricsLoading) {
      return;
    }
    if (selectedServerId === serverId) {
      setSelectedServerId(null);
      setMetrics([]);
      setMetricsError(null);
      return;
    }

    setSelectedServerId(serverId);
    setMetrics([]);
    setMetricsError(null);
    setMetricsLoading(true);
    try {
      setMetrics(await fetchServerMetrics(serverId, auth.accessToken));
    } catch (reason: unknown) {
      setMetricsError(reason instanceof Error ? reason.message : "Unable to load metrics");
    } finally {
      setMetricsLoading(false);
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    try {
      const tokens = mode === "login"
        ? await login(email, password)
        : await register(email, password, displayName);
      saveTokens(tokens);
      setAuth(tokens);
    } catch (reason: unknown) {
      setError(reason instanceof Error ? reason.message : "Authentication failed");
    }
  }

  return (
    <main>
      <p className="eyebrow">NEXUSOPS</p>
      <h1>Server monitoring, with context.</h1>
      <p className="subtitle">Authentication and server management</p>
      <section className="status-card">
        <h2>Platform status</h2>
        <p className={status ? "success" : "error"}>{status ? `${status.status} / ${status.database}` : "Backend unavailable"}</p>
      </section>
      {!auth ? (
        <section className="status-card">
          <h2>{mode === "login" ? "Sign in" : "Create account"}</h2>
          <form onSubmit={submit}>
            {mode === "register" && (
              <label>Display name<input value={displayName} onChange={(event) => setDisplayName(event.target.value)} required maxLength={120} /></label>
            )}
            <label>Email<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
            <label>Password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={12} /></label>
            <button type="submit">{mode === "login" ? "Sign in" : "Register"}</button>
          </form>
          <button className="link-button" onClick={() => setMode(mode === "login" ? "register" : "login")}>
            {mode === "login" ? "Need an account?" : "Already have an account?"}
          </button>
          {error && <p className="error">{error}</p>}
        </section>
      ) : (
        <section className="status-card">
          <div className="section-heading">
            <div><h2>Servers</h2><p>{auth.email} · {auth.role}</p></div>
            <button onClick={() => { clearTokens(); setAuth(null); setServers([]); }}>Sign out</button>
          </div>
          {error && <p className="error">{error}</p>}
          {servers.length === 0 && !error && <p>No servers registered yet.</p>}
          <ul className="server-list">
            {servers.map((server) => (
              <li key={server.id}>
                <div className="server-summary">
                  <div><strong>{server.name}</strong><span>{server.hostname} · {server.status}</span></div>
                  <button
                    type="button"
                    className="secondary-button"
                    aria-expanded={selectedServerId === server.id}
                    disabled={metricsLoading}
                    onClick={() => toggleMetrics(server.id)}
                  >
                    {selectedServerId === server.id ? "Hide details" : "View metrics & alerts"}
                  </button>
                </div>
                {selectedServerId === server.id && (
                  <div className="server-metrics" aria-live="polite">
                    {metricsLoading ? <p>Loading metric samples…</p> : null}
                    {metricsError ? <p className="error">{metricsError}</p> : null}
                    {!metricsLoading && !metricsError ? <MetricDashboard samples={metrics} /> : null}
                    <AlertPanel
                      serverId={server.id}
                      accessToken={auth.accessToken}
                      canManage={auth.role === "ADMIN" || auth.role === "OPERATOR"}
                    />
                  </div>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}
    </main>
  );
}
