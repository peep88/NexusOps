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
  const responseText = await response.text();
  if (!responseText.trim()) {
    throw new ApiRequestError(
      response.status,
      response.ok ? "Empty response from server" : `Request failed (${response.status})`
    );
  }

  let body: { data: T; error: { code: string; message: string } | null };
  try {
    body = JSON.parse(responseText) as typeof body;
  } catch (reason: unknown) {
    if (!(reason instanceof SyntaxError)) {
      throw reason;
    }
    throw new ApiRequestError(
      response.status,
      response.ok ? "Invalid JSON response from server" : `Request failed (${response.status})`
    );
  }

  if (!response.ok || body.error) {
    throw new ApiRequestError(response.status, body.error?.message ?? `Request failed (${response.status})`);
  }
  return body.data;
}
