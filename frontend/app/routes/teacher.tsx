import { Navigate, useNavigate } from "react-router";
import type { Route } from "./+types/teacher";
import { TeacherDashboard } from "~/components/teacher/teacher-dashboard";
import { useAuth } from "~/contexts/auth-context";
import { dashboardPathForRole } from "~/lib/jwt-payload";

export function meta({}: Route.MetaArgs) {
  return [{ title: "Teacher — Real-Time Linux Lab" }];
}

export default function TeacherRoute() {
  const navigate = useNavigate();
  const { hydrated, isAuthenticated, user, signOut } = useAuth();

  if (!hydrated) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 p-4 dark:from-slate-950 dark:to-slate-900">
        <p className="text-sm text-slate-500 dark:text-slate-400">Loading…</p>
      </main>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  if (user?.role !== "ROLE_TEACHER") {
    return <Navigate to={dashboardPathForRole("ROLE_STUDENT")} replace />;
  }

  return (
    <TeacherDashboard
      username={user.username}
      onSignOut={() => {
        signOut();
        navigate("/", { replace: true });
      }}
    />
  );
}
