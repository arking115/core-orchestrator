import { Navigate } from "react-router";
import type { Route } from "./+types/home";
import { AuthLayout } from "~/components/auth/auth-layout";
import { LoginForm } from "~/components/login-form";
import { useAuth } from "~/contexts/auth-context";

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
  const { hydrated, isAuthenticated, dashboardPath } = useAuth();

  if (!hydrated) {
    return (
      <AuthLayout>
        <p className="text-center text-sm text-slate-500 dark:text-slate-400">
          Loading…
        </p>
      </AuthLayout>
    );
  }

  if (isAuthenticated && dashboardPath) {
    return <Navigate to={dashboardPath} replace />;
  }

  return (
    <AuthLayout>
      <LoginForm />
    </AuthLayout>
  );
}
