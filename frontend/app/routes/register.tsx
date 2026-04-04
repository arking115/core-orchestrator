import { Navigate } from "react-router";
import type { Route } from "./+types/register";
import { AuthLayout } from "~/components/auth/auth-layout";
import { RegisterForm } from "~/components/register-form";
import { useAuth } from "~/contexts/auth-context";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Real-Time Linux Lab — Register" },
    {
      name: "description",
      content: "Create an account — Technical University of Cluj-Napoca",
    },
  ];
}

export default function RegisterRoute() {
  const { hydrated, isAuthenticated, dashboardPath } = useAuth();

  if (!hydrated) {
    return (
      <AuthLayout tagline="Create an account">
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
    <AuthLayout tagline="Create an account">
      <RegisterForm />
    </AuthLayout>
  );
}
