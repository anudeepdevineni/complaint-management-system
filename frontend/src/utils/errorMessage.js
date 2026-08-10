// Pulls a human-readable message out of an axios error.
// The backend returns errors as { status, error: "..." }, so we prefer that.
// Falls back to a caller-supplied default when the backend didn't send one.
export function getErrorMessage(err, fallback = "Something went wrong. Please try again.") {
  return err?.response?.data?.error || fallback;
}
