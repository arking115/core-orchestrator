import { getAuthToken } from "~/lib/auth-token";

/**
 * Base URL for the Spring API (same convention as {@link ~/lib/auth-api}).
 */
const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";

/** Mirrors backend {@code LabInitializationRequest}. */
export type LabInitializationRequest = {
  totalStudents: number;
  coreNumbers: number[];
  imageName: string;
};

export type ServerCapacityResponse = {
  cores: number;
  reliable: boolean;
  message?: string | null;
};

export type StopSessionsResult = {
  totalSessions: number;
  successfullyStoppedCount: number;
  failedStudentIds: string[];
  allSuccessful: boolean;
};

/** Mirrors backend {@code TeacherStudentResponse} (GET /api/teacher/students). */
export type TeacherStudentResponse = {
  studentId: string;
  displayName: string;
};

/** Mirrors backend {@code ActiveLabSessionResponse} (GET /api/teacher/sessions). */
export type ActiveLabSessionResponse = {
  studentId: string;
  assignedCore: number;
  assignedPort: number;
  /** ISO-like string from server, e.g. {@code yyyy-MM-dd'T'HH:mm:ss} */
  startTime: string;
};

/** Mirrors backend {@code LabSession} JSON (start endpoint). */
export type LabSessionResponse = {
  studentId: string;
  assignedPort: number;
  assignedCore: number;
  startTime?: string | number[] | null;
};

export class TeacherLabApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "TeacherLabApiError";
  }
}

function isTeacherLabApiError(e: unknown): e is TeacherLabApiError {
  return e instanceof TeacherLabApiError;
}

export { isTeacherLabApiError };

async function readErrorBody(res: Response): Promise<string> {
  const text = (await res.text()).trim();
  return text || res.statusText || "Request failed";
}

async function teacherFetch(path: string, init?: RequestInit): Promise<Response> {
  const token = getAuthToken();
  if (!token) {
    throw new TeacherLabApiError(401, "Not authenticated");
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
    throw new TeacherLabApiError(res.status, await readErrorBody(res));
  }
  return res;
}

/**
 * GET /api/teacher/server-capacity
 * On success: 200 with {@link ServerCapacityResponse}.
 * Remote failures may surface as 503 with body "Remote server unavailable" ({@code RemoteServiceException}).
 */
export async function getServerCapacity(): Promise<ServerCapacityResponse> {
  const res = await teacherFetch("/api/teacher/server-capacity", { method: "GET" });
  return res.json() as Promise<ServerCapacityResponse>;
}

/**
 * GET /api/teacher/sessions
 * 200 with a JSON array (possibly empty). Same rows as considered by stop-all, read-only.
 */
export async function listActiveSessions(): Promise<ActiveLabSessionResponse[]> {
  const res = await teacherFetch("/api/teacher/sessions", { method: "GET" });
  return res.json() as Promise<ActiveLabSessionResponse[]>;
}

/**
 * GET /api/teacher/students
 * Registered students (roster). Join with {@link listActiveSessions} for running vs stopped.
 */
export async function listTeacherStudents(): Promise<TeacherStudentResponse[]> {
  const res = await teacherFetch("/api/teacher/students", { method: "GET" });
  return res.json() as Promise<TeacherStudentResponse[]>;
}

/**
 * POST /api/teacher/initialize
 * Stops all active sessions then reconfigures cores (void 200).
 * Errors: 400 invalid JSON body; 500 validation / service failures (body often the exception message as plain text).
 */
export async function initializeLab(body: LabInitializationRequest): Promise<void> {
  await teacherFetch("/api/teacher/initialize", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/**
 * POST /api/teacher/stop-all
 * 200 with {@link StopSessionsResult} (partial failures still 200; check {@code allSuccessful}).
 */
export async function stopAllSessions(): Promise<StopSessionsResult> {
  const res = await teacherFetch("/api/teacher/stop-all", { method: "POST" });
  return res.json() as Promise<StopSessionsResult>;
}

/**
 * POST /api/teacher/start/{studentId}
 * 200 with {@link LabSessionResponse}.
 */
export async function startStudentSession(studentId: string): Promise<LabSessionResponse> {
  const encoded = encodeURIComponent(studentId);
  const res = await teacherFetch(`/api/teacher/start/${encoded}`, { method: "POST" });
  return res.json() as Promise<LabSessionResponse>;
}

/**
 * POST /api/teacher/stop/{studentId}
 * Void 200.
 */
export async function stopStudentSession(studentId: string): Promise<void> {
  const encoded = encodeURIComponent(studentId);
  await teacherFetch(`/api/teacher/stop/${encoded}`, { method: "POST" });
}

/** Same rules as backend {@code LabSessionService.validateStudentId} (for client-side hints). */
export function isValidStudentId(studentId: string): boolean {
  if (!studentId || studentId.length > 64) return false;
  return /^[a-zA-Z0-9][a-zA-Z0-9_-]*$/.test(studentId);
}
