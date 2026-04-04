/** Student registration email must use the official @student.utcluj.ro domain. */
export function getStudentRegistrationEmailError(
  email: string,
  options?: { allowEmpty?: boolean },
): string | null {
  const t = email.trim();
  if (!t) {
    return options?.allowEmpty ? null : "Email is required.";
  }
  if (/^[^\s@]+@student\.utcluj\.ro$/i.test(t)) {
    return null;
  }
  if (options?.allowEmpty === true && !t.includes("@")) {
    return null;
  }
  return "Use your @student.utcluj.ro university email (personal addresses like Gmail are not allowed).";
}

/** Returns an error message if invalid, or `null` if the value is valid. */
export function getStudentIdError(value: string): string | null {
  if (!value.trim()) {
    return "Student ID is required.";
  }
  const validPattern = /^[a-z0-9-]*$/;
  if (!validPattern.test(value)) {
    return "Only lowercase letters, numbers, and hyphens are allowed.";
  }
  if (value.startsWith("-") || value.endsWith("-")) {
    return "Cannot start or end with a hyphen.";
  }
  if (value.includes("--")) {
    return "Cannot have consecutive hyphens.";
  }
  return null;
}
