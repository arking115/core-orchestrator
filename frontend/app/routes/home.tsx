import { useEffect, useState } from "react";
import type { Route } from "./+types/home";
import { AuthLayout } from "~/components/auth/auth-layout";
import { LoginForm } from "~/components/login-form";
import { clearAuthToken, getAuthToken } from "~/lib/auth-token";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Real-Time Linux Lab — Sign in" },
    {
      name: "description",
      content: "Technical University of Cluj-Napoca — lab orchestrator",
    },
  ];
}

export default function Home() {
  const [token, setToken] = useState<string | null>(null);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    setToken(getAuthToken());
    setHydrated(true);
  }, []);

  function handleSignOut() {
    clearAuthToken();
    setToken(null);
  }

  return (
    <AuthLayout>
      {!hydrated ? (
        <p className="text-center text-sm text-slate-500 dark:text-slate-400">
          Loading…
        </p>
      ) : token ? (
        <div className="space-y-4 text-center">
          <p className="text-slate-700 dark:text-slate-300">You are signed in.</p>
          <button
            type="button"
            onClick={handleSignOut}
            className="rounded-md border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-800 shadow-sm transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:hover:bg-slate-800"
          >
            Sign out
          </button>
        </div>
      ) : (
        <LoginForm onSignedIn={(t) => setToken(t)} />
      )}
    </AuthLayout>
  );
}
