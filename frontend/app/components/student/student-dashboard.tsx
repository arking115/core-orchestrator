import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router";
import {
  IconActivity,
  IconPlay,
  IconSquare,
  IconTerminal,
  IconLogOut,
  IconShield,
} from "~/components/auth/icons";
import {
  getStudentSession,
  isStudentLabApiError,
  startStudentSession,
  stopStudentSession,
  type ActiveLabSessionResponse,
} from "~/lib/student-lab-api";

const SSH_HOST = (import.meta.env.VITE_SSH_HOST as string | undefined) ?? "localhost";
const SSH_USER = (import.meta.env.VITE_SSH_USER as string | undefined) ?? "student";

const cardClass =
  "rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-950";

const outlineButtonClass =
  "inline-flex items-center justify-center gap-2 rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800 dark:focus-visible:ring-offset-slate-950";

const primaryButtonClass =
  "inline-flex items-center justify-center gap-2 rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 dark:focus-visible:ring-offset-slate-950";

type StudentLabStatus = "stopped" | "starting" | "running";

type StudentDashboardProps = {
  username: string;
  roleLabel?: string;
  onSignOut: () => void;
  onSwitchToAdmin?: () => void;
};

type Feedback =
  | { kind: "success"; message: string }
  | { kind: "error"; message: string }
  | { kind: "warning"; message: string };

export function StudentDashboard({
  username,
  roleLabel,
  onSignOut,
  onSwitchToAdmin,
}: StudentDashboardProps) {
  const navigate = useNavigate();
  const [status, setStatus] = useState<StudentLabStatus>("stopped");
  const [sshCommand, setSshCommand] = useState("");
  const [copied, setCopied] = useState(false);
  const [session, setSession] = useState<ActiveLabSessionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<"start" | "stop" | "refresh" | null>(null);
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const timersRef = useRef<number[]>([]);

  function clearTimers() {
    for (const id of timersRef.current) window.clearTimeout(id);
    timersRef.current = [];
  }

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setFeedback(null);
    getStudentSession()
      .then((sess) => {
        if (cancelled) return;
        setSession(sess);
        if (sess) {
          setStatus("running");
          setSshCommand(`ssh ${SSH_USER}@${SSH_HOST} -p ${sess.assignedPort}`);
        } else {
          setStatus("stopped");
          setSshCommand("");
        }
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setFeedback({ kind: "error", message: formatApiError(e) });
        setSession(null);
        setStatus("stopped");
        setSshCommand("");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [username]);

  const statusBadge = useMemo(() => {
    if (status === "running") {
      return {
        label: "Running",
        className:
          "border-green-200 bg-green-100 text-green-800 dark:border-green-900/50 dark:bg-green-950/60 dark:text-green-200",
      };
    }
    if (status === "starting") {
      return {
        label: "Starting…",
        className:
          "border-yellow-200 bg-yellow-100 text-yellow-800 dark:border-yellow-900/50 dark:bg-yellow-950/60 dark:text-yellow-100",
      };
    }
    return {
      label: "Stopped",
      className:
        "border-slate-200 bg-slate-100 text-slate-600 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300",
    };
  }, [status]);

  const canStart = status === "stopped";
  const canStop = status === "running";

  const startDisabled = loading || busy !== null || !canStart;
  const stopDisabled = loading || busy !== null || !canStop;

  async function handleCopy() {
    if (!sshCommand) return;
    try {
      await navigator.clipboard.writeText(sshCommand);
      setCopied(true);
      const t = window.setTimeout(() => setCopied(false), 2000);
      timersRef.current.push(t);
    } catch {
      // If clipboard isn't available (permissions / non-secure context), still show selection UX.
      setCopied(false);
    }
  }

  async function handleStartLab() {
    if (startDisabled) return;
    clearTimers();
    setCopied(false);
    setFeedback(null);
    setStatus("starting");
    setBusy("start");
    try {
      const sess = await startStudentSession();
      setSession(sess);
      setSshCommand(`ssh ${SSH_USER}@${SSH_HOST} -p ${sess.assignedPort}`);
      setStatus("running");
      setFeedback({ kind: "success", message: "Lab instance started successfully." });
    } catch (e) {
      setSession(null);
      setStatus("stopped");
      setSshCommand("");
      setFeedback({ kind: "error", message: formatApiError(e) });
    } finally {
      setBusy(null);
    }
  }

  async function handleStopLab() {
    if (stopDisabled) return;
    clearTimers();
    setCopied(false);
    setFeedback(null);
    setBusy("stop");
    try {
      await stopStudentSession();
      setSession(null);
      setStatus("stopped");
      setSshCommand("");
      setFeedback({ kind: "success", message: "Lab instance stopped." });
    } catch (e) {
      setFeedback({ kind: "error", message: formatApiError(e) });
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-950 dark:to-slate-900">
      <header className="border-b border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950">
        <div className="mx-auto flex max-w-[1200px] flex-wrap items-center justify-between gap-4 px-4 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-600">
              <IconTerminal className="h-6 w-6 text-white" />
            </div>
            <div>
              <h1 className="font-semibold text-slate-900 dark:text-slate-50">
                Real-Time Linux Lab — Student
              </h1>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Welcome, {username}{" "}
                {roleLabel ? (
                  <span className="text-slate-500 dark:text-slate-500">({roleLabel})</span>
                ) : null}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            {onSwitchToAdmin ? (
              <button type="button" className={outlineButtonClass} onClick={onSwitchToAdmin}>
                <IconShield className="h-4 w-4" aria-hidden />
                Admin view
              </button>
            ) : null}
            <button type="button" className={outlineButtonClass} onClick={onSignOut}>
              <IconLogOut className="h-4 w-4" aria-hidden />
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl space-y-6 px-4 py-8">
        {feedback ? (
          <p
            role={feedback.kind === "error" ? "alert" : "status"}
            className={`rounded-lg border px-4 py-3 text-sm ${
              feedback.kind === "success"
                ? "border-green-200 bg-green-50 text-green-900 dark:border-green-900/50 dark:bg-green-950/40 dark:text-green-100"
                : feedback.kind === "warning"
                  ? "border-amber-200 bg-amber-50 text-amber-950 dark:border-amber-900/50 dark:bg-amber-950/40 dark:text-amber-100"
                  : "border-red-200 bg-red-50 text-red-900 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-100"
            }`}
          >
            {feedback.message}
          </p>
        ) : null}

        <section className={cardClass} aria-labelledby="student-lab-heading">
          <div className="border-b border-slate-100 px-6 pb-4 pt-6 dark:border-slate-800">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <h2 id="student-lab-heading" className="text-lg font-semibold text-slate-900 dark:text-slate-50">
                  My lab instance
                </h2>
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  Manage your personal real-time Ubuntu environment
                </p>
                {loading ? (
                  <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">Loading session…</p>
                ) : session ? (
                  <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                    Assigned core{" "}
                    <span className="font-medium text-slate-800 dark:text-slate-200">
                      {session.assignedCore}
                    </span>
                    , port{" "}
                    <span className="font-medium text-slate-800 dark:text-slate-200">
                      {session.assignedPort}
                    </span>
                    .
                  </p>
                ) : (
                  <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                    No active lab session yet.
                  </p>
                )}
              </div>
              <span
                className={`inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium ${statusBadge.className}`}
              >
                {statusBadge.label}
              </span>
            </div>
          </div>

          <div className="space-y-6 px-6 py-6">
            {status === "stopped" ? (
              <div className="py-8 text-center">
                <div className="mx-auto mb-4 inline-flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 dark:bg-slate-900/50">
                  <PowerOffIcon className="h-8 w-8 text-slate-600 dark:text-slate-300" />
                </div>
                <h3 className="mb-2 text-lg font-semibold text-slate-900 dark:text-slate-50">
                  Lab instance stopped
                </h3>
                <p className="mx-auto mb-6 max-w-md text-slate-600 dark:text-slate-400">
                  Click the button below to start your personal real-time Linux environment. It will take
                  approximately 5–10 seconds to initialize.
                </p>
                <button
                  type="button"
                  className={`${primaryButtonClass} px-8 py-3 text-base`}
                  onClick={() => void handleStartLab()}
                  disabled={startDisabled}
                >
                  <PowerIcon className="h-4 w-4" aria-hidden />
                  {busy === "start" ? "Starting…" : "Start lab"}
                </button>
              </div>
            ) : null}

            {status === "starting" ? (
              <div className="py-8 text-center">
                <div className="mx-auto mb-4 inline-flex h-16 w-16 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-950/40">
                  <span className="inline-flex h-8 w-8 animate-spin items-center justify-center">
                    <IconActivity className="h-8 w-8 text-blue-600 dark:text-blue-300" />
                  </span>
                </div>
                <h3 className="mb-2 text-lg font-semibold text-slate-900 dark:text-slate-50">
                  Starting lab instance…
                </h3>
                <p className="text-slate-600 dark:text-slate-400">
                  Allocating CPU core and initializing container. Please wait.
                </p>
              </div>
            ) : null}

            {status === "running" ? (
              <div className="space-y-6">
                <div className="rounded-lg border border-green-200 bg-green-50 p-6 dark:border-green-900/50 dark:bg-green-950/40">
                  <div className="flex items-start gap-3">
                    <div className="flex h-10 w-10 flex-none items-center justify-center rounded-full bg-green-100 dark:bg-green-900/30">
                      <CheckIcon className="h-5 w-5 text-green-700 dark:text-green-200" aria-hidden />
                    </div>
                    <div className="min-w-0 flex-1">
                      <h3 className="mb-1 font-semibold text-green-900 dark:text-green-100">
                        Lab instance running
                      </h3>
                      <p className="text-sm text-green-700 dark:text-green-200">
                        Your real-time Linux environment is ready. Use the command below to connect.
                      </p>
                    </div>
                  </div>
                </div>

                <div>
                  <h4 className="mb-3 font-medium text-slate-900 dark:text-slate-100">
                    Connection command
                  </h4>
                  <div className="flex gap-2">
                    <div className="flex-1 rounded-lg bg-slate-900 p-4 font-mono text-sm text-green-400">
                      {sshCommand}
                    </div>
                    <button
                      type="button"
                      className={`${outlineButtonClass} h-[52px] w-[52px] p-0`}
                      onClick={handleCopy}
                      aria-label="Copy SSH command"
                      disabled={!sshCommand || loading || busy !== null}
                    >
                      {copied ? (
                        <CheckIcon className="h-4 w-4 text-green-700 dark:text-green-300" aria-hidden />
                      ) : (
                        <CopyIcon className="h-4 w-4" aria-hidden />
                      )}
                    </button>
                  </div>
                  <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                    Copy this command and paste it into your local terminal to access your lab instance.
                  </p>
                </div>

                <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-900/50 dark:bg-blue-950/40">
                  <h4 className="mb-2 font-medium text-blue-900 dark:text-blue-100">Instructions</h4>
                  <ol className="list-inside list-decimal space-y-1 text-sm text-blue-800 dark:text-blue-200">
                    <li>Copy the SSH command above</li>
                    <li>Open your local terminal application</li>
                    <li>Paste the command and press Enter</li>
                    <li>When finished, click “Stop lab” below to shut down your instance</li>
                  </ol>
                </div>

                <div className="flex justify-end">
                  <button
                    type="button"
                    className={outlineButtonClass}
                    onClick={() => void handleStopLab()}
                    disabled={stopDisabled}
                  >
                    <PowerOffIcon className="h-4 w-4" aria-hidden />
                    {busy === "stop" ? "Stopping…" : "Stop lab"}
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </section>

        <section className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className={cardClass}>
            <div className="px-6 pb-3 pt-6">
              <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Kernel type</h3>
            </div>
            <div className="px-6 pb-6">
              <p className="text-2xl font-semibold text-slate-900 dark:text-slate-50">PREEMPT_RT</p>
              <p className="mt-1 text-xs text-slate-600 dark:text-slate-400">Real-time Linux kernel</p>
            </div>
          </div>

          <div className={cardClass}>
            <div className="px-6 pb-3 pt-6">
              <h3 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                Operating system
              </h3>
            </div>
            <div className="px-6 pb-6">
              <p className="text-2xl font-semibold text-slate-900 dark:text-slate-50">Ubuntu 22.04</p>
              <p className="mt-1 text-xs text-slate-600 dark:text-slate-400">
                Ubuntu Pro with RT kernel
              </p>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}

function CopyIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <rect width="14" height="14" x="8" y="8" rx="2" ry="2" />
      <path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2" />
    </svg>
  );
}

function CheckIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M20 6 9 17l-5-5" />
    </svg>
  );
}

function PowerIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M12 2v10" />
      <path d="M18.4 6.6a9 9 0 1 1-12.8 0" />
    </svg>
  );
}

function PowerOffIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
    >
      <path d="M12 2v6" />
      <path d="M6.4 4.6a9 9 0 1 0 11.2 0" />
      <line x1="2" y1="2" x2="22" y2="22" />
    </svg>
  );
}

function formatApiError(e: unknown): string {
  if (isStudentLabApiError(e)) {
    if (e.status === 401) return "Your session expired or is missing. Please sign in again.";
    if (e.status === 403) return "You are not allowed to perform this action.";
    if (e.status === 204) return "No active session.";
    if (e.status >= 500) return e.message || "Server error.";
    return e.message || "Request failed.";
  }
  if (e instanceof Error) return e.message;
  return "Something went wrong.";
}

