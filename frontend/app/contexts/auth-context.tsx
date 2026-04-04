import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  clearAuthToken,
  getAuthToken,
  setAuthToken,
} from "~/lib/auth-token";
import {
  dashboardPathForRole,
  isJwtExpired,
  parseOrchestratorJwt,
  type OrchestratorJwtRole,
  type ParsedOrchestratorJwt,
} from "~/lib/jwt-payload";

export type AuthUser = {
  username: string;
  role: OrchestratorJwtRole;
};

type AuthContextValue = {
  /** Finished reading localStorage after mount (avoid flash / wrong redirects). */
  hydrated: boolean;
  token: string | null;
  user: AuthUser | null;
  claims: ParsedOrchestratorJwt | null;
  isAuthenticated: boolean;
  signIn: (token: string) => OrchestratorJwtRole | null;
  signOut: () => void;
  dashboardPath: string | null;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function applyToken(token: string | null): {
  user: AuthUser | null;
  claims: ParsedOrchestratorJwt | null;
} {
  if (!token) {
    return { user: null, claims: null };
  }
  const claims = parseOrchestratorJwt(token);
  if (!claims || isJwtExpired(claims.exp)) {
    return { user: null, claims: null };
  }
  return {
    user: { username: claims.sub, role: claims.role },
    claims,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [hydrated, setHydrated] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [claims, setClaims] = useState<ParsedOrchestratorJwt | null>(null);

  const signOut = useCallback(() => {
    clearAuthToken();
    setToken(null);
    setUser(null);
    setClaims(null);
  }, []);

  const signIn = useCallback((newToken: string): OrchestratorJwtRole | null => {
    const { user: nextUser, claims: nextClaims } = applyToken(newToken);
    if (!nextUser || !nextClaims) {
      clearAuthToken();
      setToken(null);
      setUser(null);
      setClaims(null);
      return null;
    }
    setAuthToken(newToken);
    setToken(newToken);
    setUser(nextUser);
    setClaims(nextClaims);
    return nextUser.role;
  }, []);

  useEffect(() => {
    const stored = getAuthToken();
    if (stored) {
      const { user: nextUser, claims: nextClaims } = applyToken(stored);
      if (nextUser && nextClaims) {
        setToken(stored);
        setUser(nextUser);
        setClaims(nextClaims);
      } else {
        clearAuthToken();
      }
    }
    setHydrated(true);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      hydrated,
      token,
      user,
      claims,
      isAuthenticated: !!user,
      signIn,
      signOut,
      dashboardPath: user ? dashboardPathForRole(user.role) : null,
    }),
    [hydrated, token, user, claims, signIn, signOut],
  );

  return (
    <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
