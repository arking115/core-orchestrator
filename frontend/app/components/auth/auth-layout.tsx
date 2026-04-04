import type { ReactNode } from "react";
import { IconTerminal } from "./icons";

type AuthLayoutProps = {
  children: ReactNode;
  /** Extra line under the university subtitle (e.g. “Create an account”). */
  tagline?: string;
};

export function AuthLayout({ children, tagline }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-slate-50 to-slate-100 p-4 dark:from-slate-950 dark:to-slate-900">
      <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-950">
        <div className="space-y-1 border-b border-slate-100 px-6 pb-4 pt-6 text-center dark:border-slate-800">
          <div className="mb-4 flex justify-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-lg bg-blue-600">
              <IconTerminal className="h-8 w-8 text-white" />
            </div>
          </div>
          <h1 className="text-2xl font-semibold tracking-tight text-slate-900 dark:text-slate-50">
            Real-Time Linux Lab
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Technical University of Cluj-Napoca
          </p>
          {tagline ? (
            <p className="pt-2 text-sm font-medium text-slate-700 dark:text-slate-300">
              {tagline}
            </p>
          ) : null}
        </div>
        <div className="px-6 py-6">{children}</div>
      </div>
    </div>
  );
}
