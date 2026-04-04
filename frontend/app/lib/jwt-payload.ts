import { jwtDecode } from "jwt-decode";

/**
 * Matches claims from {@code com.lab.orchestrator.security.JwtService}:
 * subject = username, custom claim {@code role} = Spring authority (e.g. ROLE_STUDENT).
 * Signature is not verified here — routing/UX only; APIs must still enforce JWT on the server.
 */

export type OrchestratorJwtRole = "ROLE_STUDENT" | "ROLE_TEACHER";

export type ParsedOrchestratorJwt = {
  sub: string;
  role: OrchestratorJwtRole;
  exp: number;
};

const ROLES: OrchestratorJwtRole[] = ["ROLE_STUDENT", "ROLE_TEACHER"];

function isOrchestratorRole(value: unknown): value is OrchestratorJwtRole {
  return typeof value === "string" && ROLES.includes(value as OrchestratorJwtRole);
}

/** Raw payload shape from our backend; other standard JWT fields are ignored. */
type OrchestratorJwtPayload = {
  sub?: string;
  role?: unknown;
  exp?: number;
};

export function isJwtExpired(expSeconds: number): boolean {
  return expSeconds * 1000 <= Date.now();
}

export function parseOrchestratorJwt(
  token: string,
): ParsedOrchestratorJwt | null {
  try {
    const record = jwtDecode<OrchestratorJwtPayload>(token);
    const sub = record.sub;
    const role = record.role;
    const exp = record.exp;
    if (typeof sub !== "string" || !sub) return null;
    if (!isOrchestratorRole(role)) return null;
    if (typeof exp !== "number") return null;
    return { sub, role, exp };
  } catch {
    return null;
  }
}

export function dashboardPathForRole(role: OrchestratorJwtRole): string {
  return role === "ROLE_TEACHER" ? "/teacher" : "/student";
}
