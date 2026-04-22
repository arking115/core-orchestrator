/**
 * Base URL for the Spring API. Empty string = same origin (use Vite dev proxy for `/api`).
 * Override with `VITE_API_BASE_URL` if the UI is served from another host.
 */
const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "";

export type AuthenticationResponse = { token: string };

export type RegisterRole = "ROLE_STUDENT" | "ROLE_TEACHER";

export type RegisterPayload = {
  username: string;
  password: string;
  role: RegisterRole;
  studentId: string | null;
};

async function readErrorMessage(res: Response): Promise<string> {
  const text = (await res.text()).trim();
  return text || res.statusText || "Request failed";
}

export async function loginApi(
  username: string,
  password: string,
): Promise<AuthenticationResponse> {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    throw new Error(await readErrorMessage(res));
  }
  return res.json() as Promise<AuthenticationResponse>;
}

export async function registerApi(
  body: RegisterPayload,
): Promise<AuthenticationResponse> {
  const res = await fetch(`${API_BASE}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    throw new Error(await readErrorMessage(res));
  }
  return res.json() as Promise<AuthenticationResponse>;
}
