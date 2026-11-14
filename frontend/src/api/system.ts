export type SystemStatus = {
  service: string;
  status: string;
  database: string;
  version: string;
};

type ApiResponse<T> = {
  data: T;
  meta: { requestId: string };
  error: null;
};

export async function fetchSystemStatus(): Promise<SystemStatus> {
  const response = await fetch("/api/v1/system/status");
  if (!response.ok) {
    throw new Error(`Backend status request failed (${response.status})`);
  }
  const body: ApiResponse<SystemStatus> = await response.json();
  return body.data;
}

