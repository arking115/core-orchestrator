import { Link, Navigate, useNavigate } from "react-router";
import type { Route } from "./+types/student";
import { useAuth } from "~/contexts/auth-context";
import { dashboardPathForRole } from "~/lib/jwt-payload";
import { StudentDashboard } from "~/components/student/student-dashboard";

export function meta({}: Route.MetaArgs) {
  return [{ title: "Student — Real-Time Linux Lab" }];
}

export default function StudentPage() {
  const navigate = useNavigate();
  const { hydrated, isAuthenticated, user, signOut } = useAuth();

  if (!hydrated) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-50 p-4 dark:bg-slate-950">
        <p className="text-sm text-slate-500">Loading…</p>
      </main>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/" replace />;
  }

  if (user.role !== "ROLE_STUDENT" && user.role !== "ROLE_TEACHER") {
    return <Navigate to={dashboardPathForRole(user.role)} replace />;
  }

  const isTeacherPreview = user.role === "ROLE_TEACHER";

  return (
    <>
      {isTeacherPreview ? (
        <div className="border-b border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-950 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-100">
          <div className="mx-auto flex max-w-[1200px] flex-wrap items-center justify-between gap-3">
            <p>
              You&apos;re viewing the student dashboard as a teacher (preview).{" "}
              <Link
                to="/teacher"
                className="font-medium text-blue-700 underline hover:text-blue-800 dark:text-blue-300"
              >
                Back to teacher panel
              </Link>
            </p>
          </div>
        </div>
      ) : null}
      <StudentDashboard
        username={user.username}
        roleLabel={isTeacherPreview ? "Teacher preview" : "Student"}
        onSignOut={() => {
          signOut();
          navigate("/", { replace: true });
        }}
      />
    </>
  );
}
