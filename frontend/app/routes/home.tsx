import type { Route } from "./+types/home";
import { AuthLayout } from "~/components/auth/auth-layout";
import { LoginForm } from "~/components/login-form";

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
  return (
    <AuthLayout>
      <LoginForm />
    </AuthLayout>
  );
}
