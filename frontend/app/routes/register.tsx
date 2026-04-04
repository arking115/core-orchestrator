import type { Route } from "./+types/register";
import { AuthLayout } from "~/components/auth/auth-layout";
import { RegisterForm } from "~/components/register-form";

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
  return (
    <AuthLayout tagline="Create an account">
      <RegisterForm />
    </AuthLayout>
  );
}
