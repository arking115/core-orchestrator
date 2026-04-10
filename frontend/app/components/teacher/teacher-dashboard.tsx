import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { authInputPlainClass, authLabelClass } from "~/components/auth/field-styles";
import {
  IconActivity,
  IconSettings,
  IconSquare,
  IconTerminal,
  IconUser,
  IconLogOut,
} from "~/components/auth/icons";
import {
  getServerCapacity,
  initializeLab,
  isTeacherLabApiError,
  stopAllSessions,
} from "~/lib/teacher-lab-api";

const cardClass =
  "rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-950";

const outlineButtonClass =
  "inline-flex items-center justify-center gap-2 rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800 dark:focus-visible:ring-offset-slate-950";

const primaryButtonClass =
  "inline-flex items-center justify-center gap-2 rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 dark:focus-visible:ring-offset-slate-950";

const smOutlineButtonClass =
  "inline-flex items-center justify-center gap-1 rounded-md border border-slate-200 bg-white px-2 py-1.5 text-xs font-medium text-slate-700 shadow-sm hover:bg-slate-50 disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800";

const FALLBACK_CORE_DISPLAY = 16;

type Feedback =
  | { kind: "success"; message: string }
  | { kind: "error"; message: string }
  | { kind: "warning"; message: string };

type TeacherDashboardProps = {
  username: string;
  onSignOut: () => void;
};

export function TeacherDashboard({ username, onSignOut }: TeacherDashboardProps) {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<"setup" | "active">("setup");
  const [labInitialized, setLabInitialized] = useState(false);

  const [maxStudents, setMaxStudents] = useState("20");
  const [selectedCores, setSelectedCores] = useState<number[]>([]);
  const [dockerImage, setDockerImage] = useState("ubuntu-rt-base");
  const [isInitializing, setIsInitializing] = useState(false);

  const [isDragging, setIsDragging] = useState(false);
  const [dragMode, setDragMode] = useState<"select" | "deselect">("select");

  const [feedback, setFeedback] = useState<Feedback | null>(null);

  const [capacityLoading, setCapacityLoading] = useState(true);
  const [coreLimit, setCoreLimit] = useState(FALLBACK_CORE_DISPLAY);
  const [capacityReliable, setCapacityReliable] = useState(true);
  const [capacityMessage, setCapacityMessage] = useState<string | null>(null);

  const [isStoppingAll, setIsStoppingAll] = useState(false);

  const coreButtonCount = Math.min(FALLBACK_CORE_DISPLAY, Math.max(1, coreLimit));

  const dismissFeedback = useCallback(() => setFeedback(null), []);

  useEffect(() => {
    if (!feedback || feedback.kind !== "success") return;
    const t = window.setTimeout(dismissFeedback, 5000);
    return () => window.clearTimeout(t);
  }, [feedback, dismissFeedback]);

  useEffect(() => {
    let cancelled = false;
    setCapacityLoading(true);
    getServerCapacity()
      .then((cap) => {
        if (cancelled) return;
        const n = Number(cap.cores);
        setCoreLimit(Number.isFinite(n) && n > 0 ? n : FALLBACK_CORE_DISPLAY);
        setCapacityReliable(cap.reliable);
        setCapacityMessage(cap.message ?? null);
        if (!cap.reliable && cap.message) {
          setFeedback({ kind: "warning", message: cap.message });
        }
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        setCoreLimit(FALLBACK_CORE_DISPLAY);
        const msg = isTeacherLabApiError(e)
          ? e.message
          : e instanceof Error
            ? e.message
            : "Could not load server capacity.";
        setFeedback({
          kind: "error",
          message: `${msg} Using ${FALLBACK_CORE_DISPLAY} cores for this screen.`,
        });
      })
      .finally(() => {
        if (!cancelled) setCapacityLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  function toggleCore(coreNumber: number) {
    setSelectedCores((prev) =>
      prev.includes(coreNumber)
        ? prev.filter((c) => c !== coreNumber)
        : [...prev, coreNumber].sort((a, b) => a - b),
    );
  }

  function handleCoreMouseDown(coreNumber: number) {
    setIsDragging(true);
    const mode = selectedCores.includes(coreNumber) ? "deselect" : "select";
    setDragMode(mode);
    toggleCore(coreNumber);
  }

  function handleCoreMouseEnter(coreNumber: number) {
    if (!isDragging) return;
    const isSelected = selectedCores.includes(coreNumber);
    if (dragMode === "select" && !isSelected) {
      setSelectedCores((prev) => [...prev, coreNumber].sort((a, b) => a - b));
    } else if (dragMode === "deselect" && isSelected) {
      setSelectedCores((prev) => prev.filter((c) => c !== coreNumber));
    }
  }

  function handleMouseUp() {
    setIsDragging(false);
  }

  useEffect(() => {
    function handleGlobalMouseUp() {
      setIsDragging(false);
    }
    document.addEventListener("mouseup", handleGlobalMouseUp);
    return () => document.removeEventListener("mouseup", handleGlobalMouseUp);
  }, []);

  async function handleInitializeLab() {
    const total = parseInt(maxStudents, 10);
    if (!Number.isFinite(total) || total <= 0) {
      setFeedback({
        kind: "error",
        message: "Maximum students must be a positive number.",
      });
      return;
    }
    setIsInitializing(true);
    setFeedback(null);
    try {
      await initializeLab({
        totalStudents: total,
        coreNumbers: selectedCores,
        imageName: dockerImage.trim() || "ubuntu-rt-base",
      });
      setLabInitialized(true);
      setActiveTab("active");
      setFeedback({
        kind: "success",
        message: `Lab initialized for ${total} students.`,
      });
    } catch (e) {
      setFeedback({ kind: "error", message: formatApiError(e) });
    } finally {
      setIsInitializing(false);
    }
  }

  async function handleStopAll() {
    setIsStoppingAll(true);
    setFeedback(null);
    try {
      const result = await stopAllSessions();
      const failures = result.failedStudentIds ?? [];
      const allOk = result.allSuccessful ?? failures.length === 0;
      const failed = failures.length
        ? ` Some containers could not be stopped: ${failures.join(", ")}.`
        : "";
      setFeedback({
        kind: allOk ? "success" : "warning",
        message: `Stopped ${result.successfullyStoppedCount} of ${result.totalSessions} session(s).${failed}`,
      });
    } catch (e) {
      setFeedback({ kind: "error", message: formatApiError(e) });
    } finally {
      setIsStoppingAll(false);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-950 dark:to-slate-900">
      <header className="border-b border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950">
        <div className="mx-auto flex max-w-[1600px] flex-wrap items-center justify-between gap-4 px-4 py-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-600">
              <IconTerminal className="h-6 w-6 text-white" />
            </div>
            <div>
              <h1 className="font-semibold text-slate-900 dark:text-slate-50">
                Real-Time Linux Lab — Teacher Panel
              </h1>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Welcome, {username}{" "}
                <span className="text-slate-500 dark:text-slate-500">(Administrator)</span>
              </p>
            </div>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              className={outlineButtonClass}
              onClick={() => navigate("/student")}
            >
              <IconUser className="h-4 w-4" aria-hidden />
              Student view
            </button>
            <button type="button" className={outlineButtonClass} onClick={onSignOut}>
              <IconLogOut className="h-4 w-4" aria-hidden />
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[1600px] space-y-6 px-4 py-8">
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

        <div
          className="grid w-full max-w-md grid-cols-2 gap-1 rounded-lg border border-slate-200 bg-slate-100 p-1 dark:border-slate-700 dark:bg-slate-900"
          role="tablist"
          aria-label="Teacher dashboard sections"
        >
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === "setup"}
            className={`inline-flex items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition ${
              activeTab === "setup"
                ? "bg-white text-slate-900 shadow-sm dark:bg-slate-950 dark:text-slate-50"
                : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100"
            }`}
            onClick={() => setActiveTab("setup")}
          >
            <IconSettings className="h-4 w-4" aria-hidden />
            Lab setup
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === "active"}
            disabled={!labInitialized}
            className={`inline-flex items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-50 ${
              activeTab === "active"
                ? "bg-white text-slate-900 shadow-sm dark:bg-slate-950 dark:text-slate-50"
                : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-100"
            }`}
            onClick={() => labInitialized && setActiveTab("active")}
          >
            <IconActivity className="h-4 w-4" aria-hidden />
            Active session
          </button>
        </div>

        {activeTab === "setup" ? (
          <section className={`${cardClass} max-w-3xl`} aria-labelledby="init-heading">
            <div className="border-b border-slate-100 px-6 pb-4 pt-6 dark:border-slate-800">
              <h2 id="init-heading" className="text-lg font-semibold text-slate-900 dark:text-slate-50">
                Initialize lab session
              </h2>
              <p className="text-sm text-slate-600 dark:text-slate-400">
                Configure and start a new lab session for your class
              </p>
              {capacityLoading ? (
                <p className="mt-2 text-sm text-slate-500">Loading server capacity…</p>
              ) : (
                <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">
                  Reported machine capacity:{" "}
                  <span className="font-medium text-slate-800 dark:text-slate-200">
                    {coreLimit} core{coreLimit === 1 ? "" : "s"}
                  </span>
                  {capacityReliable ? "" : " (estimate)"}
                </p>
              )}
            </div>
            <div className="space-y-6 px-6 py-6">
              <div className="space-y-2">
                <label htmlFor="max-students" className={authLabelClass}>
                  Maximum students
                </label>
                <input
                  id="max-students"
                  type="number"
                  min={1}
                  max={50}
                  placeholder="20"
                  className={authInputPlainClass}
                  value={maxStudents}
                  onChange={(e) => setMaxStudents(e.target.value)}
                />
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  Number of student slots to prepare for this session
                </p>
              </div>

              <div className="space-y-3">
                <span className={authLabelClass}>Allowed CPU cores</span>
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  Select which physical CPU cores students can use (core 0 is reserved for system).
                  Showing cores 1–{coreButtonCount}
                  {selectedCores.some((c) => c > coreButtonCount) ? (
                    <span className="text-amber-700 dark:text-amber-300">
                      {" "}
                      — clear selections above {coreButtonCount} before initializing.
                    </span>
                  ) : null}
                </p>
                <div className="grid grid-cols-4 gap-3 sm:grid-cols-6 md:grid-cols-8">
                  {Array.from({ length: coreButtonCount }, (_, i) => i + 1).map((coreNum) => (
                    <button
                      key={coreNum}
                      type="button"
                      onMouseDown={() => handleCoreMouseDown(coreNum)}
                      onMouseEnter={() => handleCoreMouseEnter(coreNum)}
                      onMouseUp={handleMouseUp}
                      className={`aspect-square rounded-lg border-2 text-sm font-semibold transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 dark:focus-visible:ring-offset-slate-950 ${
                        selectedCores.includes(coreNum)
                          ? "border-green-600 bg-green-500 text-white shadow-md hover:bg-green-600 dark:border-green-500"
                          : "border-slate-300 bg-slate-100 text-slate-600 hover:bg-slate-200 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700"
                      }`}
                    >
                      <span className="flex h-full flex-col items-center justify-center">
                        <span className="text-xs">Core</span>
                        <span>{coreNum}</span>
                      </span>
                    </button>
                  ))}
                </div>
                <div className="flex flex-wrap items-center gap-4 text-sm">
                  <div className="flex items-center gap-2">
                    <span
                      className="h-4 w-4 rounded border-2 border-green-600 bg-green-500"
                      aria-hidden
                    />
                    <span className="text-slate-600 dark:text-slate-400">Selected</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span
                      className="h-4 w-4 rounded border-2 border-slate-300 bg-slate-100 dark:border-slate-600 dark:bg-slate-800"
                      aria-hidden
                    />
                    <span className="text-slate-600 dark:text-slate-400">Available</span>
                  </div>
                </div>
              </div>

              <div className="space-y-2">
                <label htmlFor="docker-image" className={authLabelClass}>
                  Docker image
                </label>
                <input
                  id="docker-image"
                  type="text"
                  placeholder="ubuntu-rt-base"
                  className={authInputPlainClass}
                  value={dockerImage}
                  onChange={(e) => setDockerImage(e.target.value)}
                />
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  Name of the Docker image with real-time kernel to use
                </p>
              </div>

              <div className="h-px bg-slate-200 dark:bg-slate-800" />

              <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-900/50 dark:bg-blue-950/40">
                <h3 className="mb-2 font-medium text-blue-900 dark:text-blue-100">
                  Configuration summary
                </h3>
                <ul className="space-y-1 text-sm text-blue-800 dark:text-blue-200">
                  <li>• Students: {maxStudents}</li>
                  <li>
                    • Available cores:{" "}
                    {selectedCores.length > 0 ? selectedCores.join(", ") : "None selected"}
                  </li>
                  <li>• Image: {dockerImage}</li>
                </ul>
              </div>

              <button
                type="button"
                className={`${primaryButtonClass} w-full px-4 py-3 text-base`}
                disabled={
                  isInitializing ||
                  selectedCores.length === 0 ||
                  selectedCores.some((c) => c > coreButtonCount)
                }
                onClick={handleInitializeLab}
              >
                {isInitializing ? "Initializing lab…" : "Initialize lab session"}
              </button>
            </div>
          </section>
        ) : (
          <section className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <div className={cardClass}>
              <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-100 px-6 pb-4 pt-6 dark:border-slate-800">
                <div>
                  <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-50">
                    Student sessions
                  </h2>
                  <p className="text-sm text-slate-600 dark:text-slate-400">
                    Per-student session list and actions will connect here once the backend exposes a
                    sessions listing API. Stop all still stops every active session on the server.
                  </p>
                </div>
                <button
                  type="button"
                  className={smOutlineButtonClass}
                  disabled={isStoppingAll}
                  onClick={handleStopAll}
                >
                  <IconSquare className="h-3 w-3" aria-hidden />
                  {isStoppingAll ? "Stopping…" : "Stop all"}
                </button>
              </div>
              <div className="space-y-4 px-6 py-6">
                <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50/50 px-4 py-10 text-center dark:border-slate-600 dark:bg-slate-900/30">
                  <p className="text-sm text-slate-600 dark:text-slate-400">
                    Session list not wired yet — waiting on a backend endpoint to list active lab
                    sessions. The{" "}
                    <code className="rounded bg-slate-200 px-1 py-0.5 text-slate-800 dark:bg-slate-800 dark:text-slate-200">
                      POST /api/teacher/start
                    </code>{" "}
                    and{" "}
                    <code className="rounded bg-slate-200 px-1 py-0.5 text-slate-800 dark:bg-slate-800 dark:text-slate-200">
                      POST /api/teacher/stop
                    </code>{" "}
                    routes exist; the UI will call them once listing is available.
                  </p>
                </div>
              </div>
            </div>

            <div className={cardClass}>
              <div className="border-b border-slate-100 px-6 pb-4 pt-6 dark:border-slate-800">
                <h2 className="text-lg font-semibold text-slate-900 dark:text-slate-50">
                  Real-time server monitoring
                </h2>
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  Live CPU core utilization (SigNoz or equivalent)
                </p>
              </div>
              <div className="px-6 py-6">
                <div className="flex aspect-video items-center justify-center overflow-hidden rounded-lg border border-slate-700 bg-slate-900">
                  <div className="max-w-sm px-6 text-center">
                    <IconActivity className="mx-auto mb-3 h-10 w-10 text-slate-500" aria-hidden />
                    <p className="text-sm font-medium text-slate-200">Monitoring preview</p>
                    <p className="mt-2 text-xs leading-relaxed text-slate-400">
                      SigNoz (or similar) is not wired yet. You can embed a dashboard URL here later
                      (for example{" "}
                      <code className="rounded bg-slate-800 px-1 py-0.5 text-slate-200">
                        localhost:3301
                      </code>
                      ).
                    </p>
                  </div>
                </div>
                <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">
                  Watch CPU load on assigned cores as students start their lab instances. Each active
                  student will use their dedicated core.
                </p>
                {capacityMessage && !capacityReliable ? (
                  <p className="mt-2 text-xs text-amber-800 dark:text-amber-200">
                    Capacity note: {capacityMessage}
                  </p>
                ) : null}
              </div>
            </div>
          </section>
        )}
      </main>
    </div>
  );
}

function formatApiError(e: unknown): string {
  if (isTeacherLabApiError(e)) {
    if (e.status === 400) {
      return e.message || "Invalid request.";
    }
    if (e.status === 401) {
      return "Your session expired or is missing. Please sign in again.";
    }
    if (e.status === 403) {
      return "You are not allowed to perform this action.";
    }
    if (e.status === 503) {
      return e.message || "Remote server unavailable.";
    }
    if (e.status >= 500) {
      return e.message || "Server error.";
    }
    return e.message || "Request failed.";
  }
  if (e instanceof Error) return e.message;
  return "Something went wrong.";
}
