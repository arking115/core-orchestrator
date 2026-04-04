import { Navigate, useNavigate } from "react-router";
import type { Route } from "./+types/student";
import { useAuth } from "~/contexts/auth-context";
import { dashboardPathForRole } from "~/lib/jwt-payload";

export function meta({}: Route.MetaArgs) {
  return [{ title: "Student — Real-Time Linux Lab" }];
}

export default function StudentDashboard() {
  const navigate = useNavigate();
  const { hydrated, isAuthenticated, user, signOut } = useAuth();

  if (!hydrated) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-50 p-4 dark:bg-slate-950">
        <p className="text-sm text-slate-500">Loading…</p>
      </main>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  if (user?.role !== "ROLE_STUDENT") {
    return (
      <Navigate to={dashboardPathForRole("ROLE_TEACHER")} replace />
    );
  }

  return (
    <main className="min-h-screen bg-slate-50 p-6 dark:bg-slate-950">
      <div className="mx-auto flex max-w-3xl items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-slate-900 dark:text-slate-100">
          Hello world
        </h1>
        <button
          type="button"
          onClick={() => {
            signOut();
            navigate("/", { replace: true });
          }}
          className="rounded-md border border-slate-200 bg-white px-3 py-1.5 text-sm font-medium text-slate-700 shadow-sm hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
        >
          Sign out
        </button>
      </div>
    </main>
  );
}
