export class ApiRequestError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { "Content-Type": "application/json", ...init.headers }
  });
  const body: { data: T; error: { code: string; message: string } | null } = await response.json();
  if (!response.ok || body.error) {
    throw new ApiRequestError(response.status, body.error?.message ?? `Request failed (${response.status})`);
  }
  return body.data;
}
