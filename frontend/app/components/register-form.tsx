import { useState } from "react";
import { Link } from "react-router";
import { authInputClass, authLabelClass } from "~/components/auth/field-styles";
import { IconLock, IconMail, IconUser } from "~/components/auth/icons";
import {
  getStudentIdError,
  getStudentRegistrationEmailError,
} from "~/components/auth/validate-student-id";

type RegisterTab = "student" | "teacher";

type StudentFields = {
  studentId: string;
  email: string;
  password: string;
  confirmPassword: string;
};

type TeacherFields = {
  email: string;
  password: string;
  confirmPassword: string;
};

export function RegisterForm() {
  const [tab, setTab] = useState<RegisterTab>("student");
  const [studentIdError, setStudentIdError] = useState<string | null>(null);
  const [studentEmailError, setStudentEmailError] = useState<string | null>(
    null,
  );
  const [formError, setFormError] = useState<string | null>(null);

  const [student, setStudent] = useState<StudentFields>({
    studentId: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const [teacher, setTeacher] = useState<TeacherFields>({
    email: "",
    password: "",
    confirmPassword: "",
  });

  function onStudentIdChange(value: string) {
    setStudent((s) => ({ ...s, studentId: value }));
    setStudentIdError(value ? getStudentIdError(value) : null);
  }

  function onStudentEmailChange(value: string) {
    setStudent((s) => ({ ...s, email: value }));
    setStudentEmailError(
      getStudentRegistrationEmailError(value, { allowEmpty: true }),
    );
  }

  function handleStudentSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);

    const idErr = getStudentIdError(student.studentId);
    if (idErr) {
      setStudentIdError(idErr);
      return;
    }

    const emailErr = getStudentRegistrationEmailError(student.email);
    if (emailErr) {
      setStudentEmailError(emailErr);
      return;
    }

    if (student.password !== student.confirmPassword) {
      setFormError("Passwords do not match.");
      return;
    }

    // Registration API will be wired here later (`POST /api/auth/register`).
  }

  function handleTeacherSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);

    if (teacher.password !== teacher.confirmPassword) {
      setFormError("Passwords do not match.");
      return;
    }

    // Registration API will be wired here later (`POST /api/auth/register`).
  }

  return (
    <>
      {formError ? (
        <p
          className="mb-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800 dark:border-red-900/50 dark:bg-red-950/40 dark:text-red-100"
          role="alert"
        >
          {formError}
        </p>
      ) : null}

      <div
        className="mb-4 grid grid-cols-2 gap-1 rounded-lg bg-slate-100 p-1 dark:bg-slate-900"
        role="tablist"
        aria-label="Registration type"
      >
        <button
          type="button"
          role="tab"
          aria-selected={tab === "student"}
          onClick={() => {
            setTab("student");
            setFormError(null);
          }}
          className={`rounded-md px-3 py-2 text-sm font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${
            tab === "student"
              ? "bg-white text-slate-900 shadow-sm dark:bg-slate-800 dark:text-slate-50"
              : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200"
          }`}
        >
          Student
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === "teacher"}
          onClick={() => {
            setTab("teacher");
            setFormError(null);
          }}
          className={`rounded-md px-3 py-2 text-sm font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ${
            tab === "teacher"
              ? "bg-white text-slate-900 shadow-sm dark:bg-slate-800 dark:text-slate-50"
              : "text-slate-600 hover:text-slate-900 dark:text-slate-400 dark:hover:text-slate-200"
          }`}
        >
          Teacher
        </button>
      </div>

      {tab === "student" ? (
        <form onSubmit={handleStudentSubmit} className="space-y-4">
          <div className="space-y-2">
            <label htmlFor="student-id" className={authLabelClass}>
              Student ID
            </label>
            <div className="relative">
              <IconUser className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                id="student-id"
                name="studentId"
                type="text"
                autoComplete="username"
                placeholder="john-doe-123"
                className={authInputClass}
                value={student.studentId}
                onChange={(e) => onStudentIdChange(e.target.value)}
                required
              />
            </div>
            {studentIdError ? (
              <p className="text-sm text-red-600 dark:text-red-400">
                {studentIdError}
              </p>
            ) : null}
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Use lowercase letters, numbers, and hyphens only (e.g.,
              john-doe-123).
            </p>
          </div>
          <div className="space-y-2">
            <label htmlFor="student-email" className={authLabelClass}>
              Email
            </label>
            <div className="relative">
              <IconMail className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                id="student-email"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="your.name@student.utcluj.ro"
                className={authInputClass}
                value={student.email}
                onChange={(e) => onStudentEmailChange(e.target.value)}
                required
              />
            </div>
            {studentEmailError ? (
              <p className="text-sm text-red-600 dark:text-red-400">
                {studentEmailError}
              </p>
            ) : null}
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Must be your official{" "}
              <span className="font-medium text-slate-600 dark:text-slate-300">
                @student.utcluj.ro
              </span>{" "}
              address.
            </p>
          </div>
          <div className="space-y-2">
            <label htmlFor="student-password" className={authLabelClass}>
              Password
            </label>
            <div className="relative">
              <IconLock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                id="student-password"
                name="password"
                type="password"
                autoComplete="new-password"
                placeholder="••••••••"
                className={authInputClass}
                value={student.password}
                onChange={(e) =>
                  setStudent((s) => ({ ...s, password: e.target.value }))
                }
                required
                minLength={8}
              />
            </div>
          </div>
          <div className="space-y-2">
            <label
              htmlFor="student-confirm-password"
              className={authLabelClass}
            >
              Confirm Password
            </label>
            <div className="relative">
              <IconLock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                id="student-confirm-password"
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                placeholder="••••••••"
                className={authInputClass}
                value={student.confirmPassword}
                onChange={(e) =>
                  setStudent((s) => ({
                    ...s,
                    confirmPassword: e.target.value,
                  }))
                }
                required
                minLength={8}
              />
            </div>
          </div>
          <button
            type="submit"
            disabled={!!studentIdError || !!studentEmailError}
            className="w-full rounded-md bg-blue-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60 dark:focus-visible:ring-offset-slate-950"
          >
            Create Student Account
          </button>
        </form>
      ) : (
        <form onSubmit={handleTeacherSubmit} className="space-y-4">
          <div className="space-y-2">
            <label htmlFor="teacher-email" className={authLabelClass}>
              Email
            </label>
            <div className="relative">
              <IconMail className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                id="teacher-email"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="professor@cs.utcluj.ro"
                className={authInputClass}
                value={teacher.email}
                onChange={(e) =>
                  setTeacher((t) => ({ ...t, email: e.target.value }))
                }
                required
              />
            </div>
          </div>
          <div className="space-y-2">
            <label htmlFor="teacher-password" className={authLabelClass}>
              Password
            </label>
            <div className="relative">
              <IconLock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                id="teacher-password"
                name="password"
                type="password"
                autoComplete="new-password"
                placeholder="••••••••"
                className={authInputClass}
                value={teacher.password}
                onChange={(e) =>
                  setTeacher((t) => ({ ...t, password: e.target.value }))
                }
                required
                minLength={8}
              />
            </div>
          </div>
          <div className="space-y-2">
            <label
              htmlFor="teacher-confirm-password"
              className={authLabelClass}
            >
              Confirm Password
            </label>
            <div className="relative">
              <IconLock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                id="teacher-confirm-password"
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                placeholder="••••••••"
                className={authInputClass}
                value={teacher.confirmPassword}
                onChange={(e) =>
                  setTeacher((t) => ({
                    ...t,
                    confirmPassword: e.target.value,
                  }))
                }
                required
                minLength={8}
              />
            </div>
          </div>
          <button
            type="submit"
            className="w-full rounded-md bg-blue-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-blue-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 dark:focus-visible:ring-offset-slate-950"
          >
            Create Teacher Account
          </button>
        </form>
      )}

      <p className="mt-6 text-center text-sm text-slate-600 dark:text-slate-400">
        Already have an account?{" "}
        <Link
          to="/"
          className="font-medium text-blue-600 hover:text-blue-700 hover:underline dark:text-blue-400 dark:hover:text-blue-300"
        >
          Sign in
        </Link>
      </p>
    </>
  );
}
