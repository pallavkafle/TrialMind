// Thin client for the FastAPI intelligence service.
const ML_URL = (process.env.ML_SERVICE_URL || "http://127.0.0.1:8000").replace(/\/$/, "");

class MlError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.statusCode = statusCode; // picked up by errorHandler
  }
}

const extractMessage = (data, status) => {
  if (data && typeof data.detail === "string") return data.detail;
  if (data && Array.isArray(data.detail)) {
    const fields = data.detail
      .map((d) => (d.loc || []).filter((x) => x !== "body").join("."))
      .filter(Boolean);
    return `Invalid input${fields.length ? ": " + [...new Set(fields)].join(", ") : ""}`;
  }
  return `ML service error (${status})`;
};

const callMl = async (path, { method = "GET", body, timeoutMs = 30000 } = {}) => {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  let res;
  try {
    res = await fetch(`${ML_URL}${path}`, {
      method,
      headers: body ? { "Content-Type": "application/json" } : undefined,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
  } catch (err) {
    if (err.name === "AbortError") throw new MlError("ML service timed out", 504);
    throw new MlError(`ML service is not reachable. Is it running on ${ML_URL}?`, 503);
  } finally {
    clearTimeout(timer);
  }

  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new MlError(extractMessage(data, res.status), res.status >= 500 ? 502 : res.status);
  }
  return data;
};

module.exports = { callMl, MlError };