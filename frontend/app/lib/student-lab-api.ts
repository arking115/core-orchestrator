import { getAuthToken } from "~/lib/auth-token";

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";

/** Mirrors backend {@code ActiveLabSessionResponse}. */
export type ActiveLabSessionResponse = {
  studentId: string;
  assignedCore: number;
  assignedPort: number;
  /** ISO-like string from server, e.g. {@code yyyy-MM-dd'T'HH:mm:ss} */
  startTime: string;
};

export class StudentLabApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "StudentLabApiError";
  }
}

export function isStudentLabApiError(e: unknown): e is StudentLabApiError {
  return e instanceof StudentLabApiError;
}

async function readErrorBody(res: Response): Promise<string> {
  const text = (await res.text()).trim();
  return text || res.statusText || "Request failed";
}

async function studentFetch(path: string, init?: RequestInit): Promise<Response> {
  const token = getAuthToken();
  if (!token) {
    throw new StudentLabApiError(401, "Not authenticated");
  }
  const headers = new Headers(init?.headers);
  headers.set("Authorization", `Bearer ${token}`);
  if (init?.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
  });
  if (!res.ok) {
    throw new StudentLabApiError(res.status, await readErrorBody(res));
  }
  return res;
}

/**
 * GET /api/student/session
 * - 200 JSON when session exists
 * - 204 No Content when no active session
 */
export async function getStudentSession(): Promise<ActiveLabSessionResponse | null> {
  const res = await studentFetch("/api/student/session", { method: "GET" });
  if (res.status === 204) return null;
  return res.json() as Promise<ActiveLabSessionResponse>;
}

/**
 * POST /api/student/start
 * 200 JSON with {@link ActiveLabSessionResponse}.
 */
export async function startStudentSession(): Promise<ActiveLabSessionResponse> {
  const res = await studentFetch("/api/student/start", { method: "POST" });
  return res.json() as Promise<ActiveLabSessionResponse>;
}

/**
 * POST /api/student/stop
 * Void 200.
 */
export async function stopStudentSession(): Promise<void> {
  await studentFetch("/api/student/stop", { method: "POST" });
}

