let csrf;
export async function request(path, options = {}) {
  const method = options.method || "GET";
  if (method !== "GET" && !csrf) {
    const r = await fetch("/api/csrf");
    csrf = (await r.json()).token;
  }
  const headers = { ...options.headers };
  if (method !== "GET") headers["X-XSRF-TOKEN"] = csrf;
  if (options.body && !(options.body instanceof URLSearchParams)) {
    headers["Content-Type"] = "application/json";
    options = { ...options, body: JSON.stringify(options.body) };
  }
  const r = await fetch("/api" + path, { ...options, headers });
  if (!r.ok) {
    let body;
    try {
      body = await r.json();
    } catch {}
    throw new Error(body?.message || `Request failed (${r.status})`);
  }
  if (path === "/login" || path === "/logout" || path === "/password")
    csrf = undefined;
  return r.status === 204 || r.headers.get("content-length") === "0"
    ? null
    : r.text().then((t) => (t ? JSON.parse(t) : null));
}
